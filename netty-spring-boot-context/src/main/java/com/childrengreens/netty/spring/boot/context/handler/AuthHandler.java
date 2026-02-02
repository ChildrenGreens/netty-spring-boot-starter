/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.childrengreens.netty.spring.boot.context.handler;

import com.childrengreens.netty.spring.boot.context.auth.*;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Channel handler for authentication.
 *
 * <p>This handler performs authentication based on the configured mode:
 * <ul>
 * <li>{@link AuthMode#TOKEN}: Validates token from HTTP header on each request</li>
 * <li>{@link AuthMode#CREDENTIAL}: Validates username/password from first message</li>
 * </ul>
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class AuthHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final AuthSpec spec;
    private final Authenticator authenticator;
    private final ConnectionManager connectionManager;
    private final AuthMetrics metrics;

    private ScheduledFuture<?> authTimeoutFuture;

    /**
     * Create a new AuthHandler.
     *
     * @param spec the auth specification
     * @param authenticator the authenticator
     * @param connectionManager the connection manager (may be null for TOKEN mode)
     * @param metrics the auth metrics (may be null)
     */
    public AuthHandler(AuthSpec spec, Authenticator authenticator,
                       ConnectionManager connectionManager, AuthMetrics metrics) {
        this.spec = spec;
        this.authenticator = authenticator;
        this.connectionManager = connectionManager;
        this.metrics = metrics;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (spec.getMode() == AuthMode.CREDENTIAL) {
            // Start auth timeout timer for credential mode
            long timeout = spec.getAuthTimeout();
            if (timeout > 0) {
                authTimeoutFuture = ctx.executor().schedule(() -> {
                    if (!isAuthenticated(ctx)) {
                        logger.warn("Authentication timeout for channel {}", ctx.channel().remoteAddress());
                        if (metrics != null) {
                            metrics.incrementTimeout();
                        }
                        sendAuthError(ctx, "AUTH_TIMEOUT", "Authentication timeout");
                        ctx.close();
                    }
                }, timeout, TimeUnit.MILLISECONDS);
            }
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cancelAuthTimeout();
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        switch (spec.getMode()) {
            case TOKEN:
                handleTokenAuth(ctx, msg);
                break;

            case CREDENTIAL:
                handleCredentialAuth(ctx, msg);
                break;

            default:
                ctx.fireChannelRead(msg);
        }
    }

    /**
     * Handle token-based authentication (HTTP).
     */
    private void handleTokenAuth(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest)) {
            ctx.fireChannelRead(msg);
            return;
        }

        FullHttpRequest request = (FullHttpRequest) msg;
        String path = extractPath(request);

        // Check if path is excluded from authentication
        if (isExcluded(path)) {
            ctx.fireChannelRead(msg);
            return;
        }

        // Extract token from header
        String headerName = spec.getToken() != null ? spec.getToken().getHeaderName() : "Authorization";
        String token = request.headers().get(headerName);

        if (token == null || token.isEmpty()) {
            sendHttpUnauthorized(ctx, request, "MISSING_TOKEN", "Authorization header is required");
            return;
        }

        // Authenticate
        AuthResult result = authenticator.authenticateToken(token);

        if (result.isSuccess()) {
            if (metrics != null) {
                metrics.incrementSuccess();
            }
            // Store principal in channel attribute for this request
            ctx.channel().attr(NettyContext.AUTH_PRINCIPAL_KEY).set(result.getPrincipal());
            ctx.fireChannelRead(msg);
        } else {
            if (metrics != null) {
                metrics.incrementFailure();
            }
            sendHttpUnauthorized(ctx, request, result.getErrorCode(), result.getErrorMessage());
        }
    }

    /**
     * Handle credential-based authentication (WebSocket/TCP).
     */
    @SuppressWarnings("unchecked")
    private void handleCredentialAuth(ChannelHandlerContext ctx, Object msg) throws Exception {
        // If already authenticated, pass through
        if (isAuthenticated(ctx)) {
            ctx.fireChannelRead(msg);
            return;
        }

        // Check if this is an auth message
        if (!isAuthMessage(msg)) {
            logger.warn("First message must be authentication: {}", ctx.channel().remoteAddress());
            sendAuthError(ctx, "AUTH_REQUIRED", "First message must be authentication");
            if (spec.isCloseOnFailure()) {
                ctx.close();
            }
            return;
        }

        // Cancel timeout since we received the auth message
        cancelAuthTimeout();

        // Extract credentials from message
        Map<String, Object> authMsg = (Map<String, Object>) msg;
        Object payload = authMsg.get("payload");
        if (!(payload instanceof Map)) {
            sendAuthError(ctx, "INVALID_FORMAT", "Invalid authentication message format");
            if (spec.isCloseOnFailure()) {
                ctx.close();
            }
            return;
        }

        Map<String, Object> credentials = (Map<String, Object>) payload;
        String username = (String) credentials.get("username");
        String password = (String) credentials.get("password");

        if (username == null || password == null) {
            sendAuthError(ctx, "MISSING_CREDENTIALS", "Username and password are required");
            if (spec.isCloseOnFailure()) {
                ctx.close();
            }
            return;
        }

        // Authenticate
        AuthResult result = authenticator.authenticateCredential(username, password);

        if (result.isSuccess()) {
            // Check connection policy
            if (connectionManager != null && spec.getConnectionPolicy() != null) {
                boolean allowed = connectionManager.onAuthenticated(
                        ctx.channel(), result.getPrincipal().getUserId(),
                        spec.getConnectionPolicy(), metrics);

                if (!allowed) {
                    if (metrics != null) {
                        metrics.incrementFailure();
                    }
                    sendAuthError(ctx, "ALREADY_CONNECTED", "User already has an active connection");
                    ctx.close();
                    return;
                }
            }

            if (metrics != null) {
                metrics.incrementSuccess();
            }

            // Store principal in channel attribute
            ctx.channel().attr(NettyContext.AUTH_PRINCIPAL_KEY).set(result.getPrincipal());

            logger.debug("User {} authenticated on channel {}",
                    result.getPrincipal().getUserId(), ctx.channel().remoteAddress());

            // Send success response
            sendAuthSuccess(ctx, result);
        } else {
            if (metrics != null) {
                metrics.incrementFailure();
            }
            sendAuthError(ctx, result.getErrorCode(), result.getErrorMessage());
            if (spec.isCloseOnFailure()) {
                ctx.close();
            }
        }
        // Note: Auth message is not passed to business handlers
    }

    /**
     * Check if the channel is authenticated.
     */
    private boolean isAuthenticated(ChannelHandlerContext ctx) {
        return ctx.channel().attr(NettyContext.AUTH_PRINCIPAL_KEY).get() != null;
    }

    /**
     * Check if the message is an authentication message.
     */
    @SuppressWarnings("unchecked")
    private boolean isAuthMessage(Object msg) {
        if (msg instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) msg;
            Object type = map.get("type");
            return spec.getAuthRoute().equals(type);
        }
        return false;
    }

    /**
     * Check if the path is excluded from authentication.
     */
    private boolean isExcluded(String path) {
        if (spec.getExcludePaths() == null || spec.getExcludePaths().isEmpty()) {
            return false;
        }
        for (String pattern : spec.getExcludePaths()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract path from HTTP request.
     */
    private String extractPath(FullHttpRequest request) {
        String uri = request.uri();
        int queryIndex = uri.indexOf('?');
        return queryIndex > 0 ? uri.substring(0, queryIndex) : uri;
    }

    /**
     * Send HTTP 401 Unauthorized response.
     */
    private void sendHttpUnauthorized(ChannelHandlerContext ctx, FullHttpRequest request,
                                       String code, String message) {
        logger.debug("Authentication failed for {}: {} - {}", request.uri(), code, message);

        // Build simple JSON response body
        String jsonBody = String.format("{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\"}}",
                escapeJson(code), escapeJson(message));
        byte[] bodyBytes = jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        DefaultFullHttpResponse response =
                new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.UNAUTHORIZED,
                        Unpooled.wrappedBuffer(bodyBytes));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bodyBytes.length);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Simple JSON string escaping.
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Send authentication success response.
     */
    private void sendAuthSuccess(ChannelHandlerContext ctx, AuthResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", spec.getAuthRoute() + "/response");
        response.putAll(result.toResponseMap());

        ctx.writeAndFlush(response);
    }

    /**
     * Send authentication error response.
     */
    private void sendAuthError(ChannelHandlerContext ctx, String code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", spec.getAuthRoute() + "/response");
        response.put("success", false);

        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        response.put("error", error);

        ctx.writeAndFlush(response);
    }

    /**
     * Cancel the auth timeout timer.
     */
    private void cancelAuthTimeout() {
        if (authTimeoutFuture != null && !authTimeoutFuture.isDone()) {
            authTimeoutFuture.cancel(false);
            authTimeoutFuture = null;
        }
    }

}
