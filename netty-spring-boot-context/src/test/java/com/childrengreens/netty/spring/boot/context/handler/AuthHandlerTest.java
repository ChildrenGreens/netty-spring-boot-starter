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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link AuthHandler}.
 */
class AuthHandlerTest {

    private AuthSpec spec;
    private Authenticator authenticator;
    private ConnectionManager connectionManager;
    private AuthMetrics metrics;
    private List<Object> receivedMessages;

    @BeforeEach
    void setUp() {
        spec = new AuthSpec();
        spec.setEnabled(true);
        spec.setMode(AuthMode.CREDENTIAL);
        spec.setAuthRoute("/auth");
        spec.setAuthTimeout(10000);
        spec.setCloseOnFailure(true);

        authenticator = mock(Authenticator.class);
        connectionManager = mock(ConnectionManager.class);
        metrics = new AuthMetrics("test-server");
        receivedMessages = new ArrayList<>();

        // Default mock behavior - allow connections
        when(connectionManager.onAuthenticated(any(), anyString(), any(), any())).thenReturn(true);
    }

    private EmbeddedChannel createChannel(AuthHandler handler) {
        return new EmbeddedChannel(handler, new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                receivedMessages.add(msg);
            }
        });
    }

    @Test
    void credentialAuth_validCredentials_authenticatesChannel() {
        when(authenticator.authenticateCredential("admin", "password"))
                .thenReturn(AuthResult.success("user123").withUsername("admin"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Send auth message
        Map<String, Object> authMessage = createAuthMessage("admin", "password");
        channel.writeInbound(authMessage);

        // Verify authenticated
        AuthPrincipal principal = channel.attr(NettyContext.AUTH_PRINCIPAL_KEY).get();
        assertThat(principal).isNotNull();
        assertThat(principal.getUserId()).isEqualTo("user123");
        assertThat(metrics.getSuccessCount()).isEqualTo(1);

        // Auth message should not be passed to next handler
        assertThat(receivedMessages).isEmpty();

        // Verify response was sent
        Object response = channel.readOutbound();
        assertThat(response).isNotNull();

        channel.close();
    }

    @Test
    void credentialAuth_invalidCredentials_rejectsAndCloses() {
        when(authenticator.authenticateCredential("admin", "wrong"))
                .thenReturn(AuthResult.failure("INVALID_CREDENTIALS", "Wrong password"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        Map<String, Object> authMessage = createAuthMessage("admin", "wrong");
        channel.writeInbound(authMessage);

        // Verify not authenticated
        AuthPrincipal principal = channel.attr(NettyContext.AUTH_PRINCIPAL_KEY).get();
        assertThat(principal).isNull();
        assertThat(metrics.getFailureCount()).isEqualTo(1);

        // Channel should be closed
        assertThat(channel.isActive()).isFalse();

        channel.close();
    }

    @Test
    void credentialAuth_alreadyAuthenticated_passesThrough() {
        when(authenticator.authenticateCredential("admin", "password"))
                .thenReturn(AuthResult.success("user123"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // First authenticate
        channel.writeInbound(createAuthMessage("admin", "password"));

        // Then send business message
        Map<String, Object> businessMessage = new HashMap<>();
        businessMessage.put("type", "user.query");
        businessMessage.put("payload", Map.of("userId", "123"));
        channel.writeInbound(businessMessage);

        // Business message should be passed through
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.get(0)).isEqualTo(businessMessage);

        channel.close();
    }

    @Test
    void credentialAuth_nonAuthFirstMessage_rejects() {
        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Send non-auth message first
        Map<String, Object> businessMessage = new HashMap<>();
        businessMessage.put("type", "user.query");
        channel.writeInbound(businessMessage);

        // Should be rejected and closed
        assertThat(channel.isActive()).isFalse();
        assertThat(receivedMessages).isEmpty();

        channel.close();
    }

    @Test
    void credentialAuth_missingUsername_rejects() {
        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        Map<String, Object> authMessage = new HashMap<>();
        authMessage.put("type", "/auth");
        authMessage.put("payload", Map.of("password", "123"));
        channel.writeInbound(authMessage);

        assertThat(channel.isActive()).isFalse();

        channel.close();
    }

    @Test
    void credentialAuth_connectionRejected_closesChannel() {
        when(authenticator.authenticateCredential("admin", "password"))
                .thenReturn(AuthResult.success("user123"));
        when(connectionManager.onAuthenticated(any(), anyString(), any(), any()))
                .thenReturn(false);

        spec.setConnectionPolicy(new ConnectionPolicy());
        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        channel.writeInbound(createAuthMessage("admin", "password"));

        // Should be closed due to connection policy
        assertThat(channel.isActive()).isFalse();
        assertThat(metrics.getFailureCount()).isEqualTo(1);

        channel.close();
    }

    @Test
    void credentialAuth_closeOnFailureFalse_keepsChannelOpen() {
        spec.setCloseOnFailure(false);
        when(authenticator.authenticateCredential("admin", "wrong"))
                .thenReturn(AuthResult.failure("INVALID_CREDENTIALS", "Wrong password"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        channel.writeInbound(createAuthMessage("admin", "wrong"));

        // Channel should still be active
        assertThat(channel.isActive()).isTrue();

        channel.close();
    }

    @Test
    void credentialAuth_nullMetrics_handlesGracefully() {
        when(authenticator.authenticateCredential("admin", "password"))
                .thenReturn(AuthResult.success("user123"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, null);
        EmbeddedChannel channel = createChannel(handler);

        // Should not throw
        channel.writeInbound(createAuthMessage("admin", "password"));

        AuthPrincipal principal = channel.attr(NettyContext.AUTH_PRINCIPAL_KEY).get();
        assertThat(principal).isNotNull();

        channel.close();
    }

    @Test
    void credentialAuth_nullConnectionManager_stillWorks() {
        when(authenticator.authenticateCredential("admin", "password"))
                .thenReturn(AuthResult.success("user123"));

        AuthHandler handler = new AuthHandler(spec, authenticator, null, metrics);
        EmbeddedChannel channel = createChannel(handler);

        channel.writeInbound(createAuthMessage("admin", "password"));

        AuthPrincipal principal = channel.attr(NettyContext.AUTH_PRINCIPAL_KEY).get();
        assertThat(principal).isNotNull();

        channel.close();
    }

    private Map<String, Object> createAuthMessage(String username, String password) {
        Map<String, Object> authMessage = new HashMap<>();
        authMessage.put("type", "/auth");
        authMessage.put("payload", Map.of("username", username, "password", password));
        return authMessage;
    }

    // ========== TOKEN Mode Tests ==========

    @Test
    void tokenAuth_validToken_passesThrough() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());
        spec.getToken().setHeaderName("Authorization");

        when(authenticator.authenticateToken("Bearer valid-token"))
                .thenReturn(AuthResult.success("user123"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        FullHttpRequest request = createHttpRequest("/api/data", "Authorization", "Bearer valid-token");
        channel.writeInbound(request);

        // Should pass through
        assertThat(receivedMessages).hasSize(1);
        assertThat(metrics.getSuccessCount()).isEqualTo(1);

        // Principal should be set
        AuthPrincipal principal = channel.attr(NettyContext.AUTH_PRINCIPAL_KEY).get();
        assertThat(principal).isNotNull();
        assertThat(principal.getUserId()).isEqualTo("user123");

        channel.close();
    }

    @Test
    void tokenAuth_missingToken_returns401() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());
        spec.getToken().setHeaderName("Authorization");

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        FullHttpRequest request = createHttpRequest("/api/data", null, null);
        channel.writeInbound(request);

        // Should not pass through
        assertThat(receivedMessages).isEmpty();

        // Should send 401 response
        FullHttpResponse response = channel.readOutbound();
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.UNAUTHORIZED);

        String body = response.content().toString(StandardCharsets.UTF_8);
        assertThat(body).contains("MISSING_TOKEN");

        channel.close();
    }

    @Test
    void tokenAuth_invalidToken_returns401() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());
        spec.getToken().setHeaderName("Authorization");

        when(authenticator.authenticateToken("Bearer invalid-token"))
                .thenReturn(AuthResult.failure("INVALID_TOKEN", "Token expired"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        FullHttpRequest request = createHttpRequest("/api/data", "Authorization", "Bearer invalid-token");
        channel.writeInbound(request);

        // Should not pass through
        assertThat(receivedMessages).isEmpty();
        assertThat(metrics.getFailureCount()).isEqualTo(1);

        // Should send 401 response
        FullHttpResponse response = channel.readOutbound();
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.UNAUTHORIZED);

        String body = response.content().toString(StandardCharsets.UTF_8);
        assertThat(body).contains("INVALID_TOKEN");

        channel.close();
    }

    @Test
    void tokenAuth_excludedPath_passesWithoutAuth() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());
        spec.setExcludePaths(Arrays.asList("/health", "/actuator/**", "/public/*"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Test exact match
        FullHttpRequest request1 = createHttpRequest("/health", null, null);
        channel.writeInbound(request1);
        assertThat(receivedMessages).hasSize(1);

        // Test wildcard match
        receivedMessages.clear();
        FullHttpRequest request2 = createHttpRequest("/actuator/health", null, null);
        channel.writeInbound(request2);
        assertThat(receivedMessages).hasSize(1);

        // Test single level wildcard
        receivedMessages.clear();
        FullHttpRequest request3 = createHttpRequest("/public/login", null, null);
        channel.writeInbound(request3);
        assertThat(receivedMessages).hasSize(1);

        // Verify no authentication was attempted
        verify(authenticator, never()).authenticateToken(anyString());

        channel.close();
    }

    @Test
    void tokenAuth_nonExcludedPath_requiresAuth() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());
        spec.setExcludePaths(Arrays.asList("/health"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        FullHttpRequest request = createHttpRequest("/api/users", null, null);
        channel.writeInbound(request);

        // Should be rejected due to missing token
        assertThat(receivedMessages).isEmpty();

        FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.UNAUTHORIZED);

        channel.close();
    }

    @Test
    void tokenAuth_pathWithQueryString_extractsCorrectly() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());
        spec.setExcludePaths(Arrays.asList("/health"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Request with query string - should match /health
        FullHttpRequest request = createHttpRequest("/health?detail=true", null, null);
        channel.writeInbound(request);

        // Should pass through without auth (path extracted correctly)
        assertThat(receivedMessages).hasSize(1);

        channel.close();
    }

    @Test
    void tokenAuth_nonHttpMessage_passesThrough() {
        spec.setMode(AuthMode.TOKEN);

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Send non-HTTP message
        String textMessage = "plain text message";
        channel.writeInbound(textMessage);

        // Should pass through without authentication
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.get(0)).isEqualTo(textMessage);

        channel.close();
    }

    @Test
    void tokenAuth_customHeaderName_usesCorrectHeader() {
        spec.setMode(AuthMode.TOKEN);
        TokenSpec tokenSpec = new TokenSpec();
        tokenSpec.setHeaderName("X-API-Key");
        spec.setToken(tokenSpec);

        when(authenticator.authenticateToken("my-api-key"))
                .thenReturn(AuthResult.success("api-user"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        FullHttpRequest request = createHttpRequest("/api/data", "X-API-Key", "my-api-key");
        channel.writeInbound(request);

        assertThat(receivedMessages).hasSize(1);

        channel.close();
    }

    @Test
    void tokenAuth_nullTokenSpec_usesDefaultHeader() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(null);

        when(authenticator.authenticateToken("Bearer token"))
                .thenReturn(AuthResult.success("user123"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        FullHttpRequest request = createHttpRequest("/api/data", "Authorization", "Bearer token");
        channel.writeInbound(request);

        assertThat(receivedMessages).hasSize(1);

        channel.close();
    }

    @Test
    void tokenAuth_emptyToken_returns401() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());
        spec.getToken().setHeaderName("Authorization");

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        FullHttpRequest request = createHttpRequest("/api/data", "Authorization", "");
        channel.writeInbound(request);

        // Should be rejected
        assertThat(receivedMessages).isEmpty();

        FullHttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.UNAUTHORIZED);

        String body = response.content().toString(StandardCharsets.UTF_8);
        assertThat(body).contains("MISSING_TOKEN");

        channel.close();
    }

    // ========== Auth Timeout Tests ==========

    @Test
    void credentialAuth_authTimeout_closesChannel() throws Exception {
        spec.setAuthTimeout(100); // 100ms timeout

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Wait for timeout
        Thread.sleep(200);

        // Run pending scheduled tasks
        channel.runScheduledPendingTasks();

        // Verify timeout occurred
        assertThat(metrics.getTimeoutCount()).isEqualTo(1);

        // Response should be sent
        Object response = channel.readOutbound();
        assertThat(response).isNotNull();

        channel.close();
    }

    @Test
    void credentialAuth_authBeforeTimeout_noTimeoutError() throws Exception {
        spec.setAuthTimeout(500); // 500ms timeout

        when(authenticator.authenticateCredential("admin", "password"))
                .thenReturn(AuthResult.success("user123"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Authenticate before timeout
        channel.writeInbound(createAuthMessage("admin", "password"));

        // Wait and run scheduled tasks
        Thread.sleep(100);
        channel.runScheduledPendingTasks();

        // No timeout should have occurred
        assertThat(metrics.getTimeoutCount()).isEqualTo(0);
        assertThat(metrics.getSuccessCount()).isEqualTo(1);

        channel.close();
    }

    @Test
    void credentialAuth_zeroTimeout_noTimeoutScheduled() throws Exception {
        spec.setAuthTimeout(0); // Disable timeout

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Wait a bit
        Thread.sleep(100);
        channel.runScheduledPendingTasks();

        // No timeout should have occurred
        assertThat(metrics.getTimeoutCount()).isEqualTo(0);

        channel.close();
    }

    @Test
    void credentialAuth_channelInactive_cancelsTimeout() throws Exception {
        spec.setAuthTimeout(500);

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Close channel before timeout
        channel.close();

        // Wait for original timeout
        Thread.sleep(100);

        // No timeout error should occur since channel was closed
        assertThat(metrics.getTimeoutCount()).isEqualTo(0);
    }

    // ========== isExcluded Tests ==========

    @Test
    void isExcluded_nullExcludePaths_returnsFalse() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());
        spec.setExcludePaths(null);

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        FullHttpRequest request = createHttpRequest("/any/path", null, null);
        channel.writeInbound(request);

        // Should require auth (not excluded)
        assertThat(receivedMessages).isEmpty();

        channel.close();
    }

    @Test
    void isExcluded_emptyExcludePaths_returnsFalse() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());
        spec.setExcludePaths(new ArrayList<>());

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        FullHttpRequest request = createHttpRequest("/any/path", null, null);
        channel.writeInbound(request);

        // Should require auth (not excluded)
        assertThat(receivedMessages).isEmpty();

        channel.close();
    }

    @Test
    void isExcluded_antPatternMatching_works() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());
        spec.setExcludePaths(Arrays.asList(
                "/api/v1/public/**",  // double wildcard
                "/static/*.js",       // single level with extension
                "/health"             // exact match
        ));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Double wildcard - nested path
        FullHttpRequest request1 = createHttpRequest("/api/v1/public/users/list", null, null);
        channel.writeInbound(request1);
        assertThat(receivedMessages).hasSize(1);

        // Single level with extension
        receivedMessages.clear();
        FullHttpRequest request2 = createHttpRequest("/static/app.js", null, null);
        channel.writeInbound(request2);
        assertThat(receivedMessages).hasSize(1);

        // Exact match
        receivedMessages.clear();
        FullHttpRequest request3 = createHttpRequest("/health", null, null);
        channel.writeInbound(request3);
        assertThat(receivedMessages).hasSize(1);

        // Should NOT match
        receivedMessages.clear();
        FullHttpRequest request4 = createHttpRequest("/api/v2/public/users", null, null);
        channel.writeInbound(request4);
        assertThat(receivedMessages).isEmpty(); // Requires auth

        channel.close();
    }

    // ========== escapeJson Tests ==========

    @Test
    void tokenAuth_specialCharsInError_escapedCorrectly() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());

        when(authenticator.authenticateToken("bad-token"))
                .thenReturn(AuthResult.failure("ERROR", "Invalid \"token\"\nwith\tnewlines\\and\\backslashes"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        FullHttpRequest request = createHttpRequest("/api/data", "Authorization", "bad-token");
        channel.writeInbound(request);

        FullHttpResponse response = channel.readOutbound();
        String body = response.content().toString(StandardCharsets.UTF_8);

        // Verify JSON is properly escaped
        assertThat(body).contains("\\\"token\\\"");  // escaped quotes
        assertThat(body).contains("\\n");            // escaped newline
        assertThat(body).contains("\\t");            // escaped tab
        assertThat(body).contains("\\\\");           // escaped backslash

        channel.close();
    }

    @Test
    void tokenAuth_nullErrorMessage_handledGracefully() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());

        when(authenticator.authenticateToken("bad-token"))
                .thenReturn(AuthResult.failure("ERROR", null));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        FullHttpRequest request = createHttpRequest("/api/data", "Authorization", "bad-token");
        channel.writeInbound(request);

        FullHttpResponse response = channel.readOutbound();
        String body = response.content().toString(StandardCharsets.UTF_8);

        // Should handle null gracefully (empty string)
        assertThat(body).contains("\"message\":\"\"");

        channel.close();
    }

    @Test
    void tokenAuth_carriageReturn_escapedCorrectly() {
        spec.setMode(AuthMode.TOKEN);
        spec.setToken(new TokenSpec());

        when(authenticator.authenticateToken("bad-token"))
                .thenReturn(AuthResult.failure("ERROR", "line1\r\nline2"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        FullHttpRequest request = createHttpRequest("/api/data", "Authorization", "bad-token");
        channel.writeInbound(request);

        FullHttpResponse response = channel.readOutbound();
        String body = response.content().toString(StandardCharsets.UTF_8);

        // Verify carriage return is escaped
        assertThat(body).contains("\\r\\n");

        channel.close();
    }

    // ========== Helper Methods ==========

    private FullHttpRequest createHttpRequest(String uri, String headerName, String headerValue) {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                uri,
                Unpooled.EMPTY_BUFFER);

        if (headerName != null && headerValue != null) {
            request.headers().set(headerName, headerValue);
        }

        return request;
    }
}
