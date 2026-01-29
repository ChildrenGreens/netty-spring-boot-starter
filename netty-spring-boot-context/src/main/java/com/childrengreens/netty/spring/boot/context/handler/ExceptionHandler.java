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

import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Global exception handler for the pipeline.
 *
 * <p>Catches any exceptions that propagate through the pipeline and
 * handles them appropriately based on the protocol type.
 *
 * <p>The handler determines the protocol type from the channel attribute
 * {@link NettyContext#PROTOCOL_TYPE_KEY} and sends the appropriate error response:
 * <ul>
 * <li>HTTP - Sends HTTP 500 response with JSON error body</li>
 * <li>WebSocket - Sends close frame with error reason</li>
 * <li>TCP/UDP - Logs error without sending response</li>
 * </ul>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class ExceptionHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String protocolType = null;
        if (ctx.channel().hasAttr(NettyContext.PROTOCOL_TYPE_KEY)) {
            protocolType = ctx.channel().attr(NettyContext.PROTOCOL_TYPE_KEY).get();
        }

        // Log based on exception type
        if (isConnectionResetException(cause)) {
            logger.debug("Connection reset by peer: {}", ctx.channel().remoteAddress());
        } else {
            logger.error("Unhandled exception in pipeline (protocol={})", protocolType, cause);
        }

        // Send error response if channel is still active
        if (ctx.channel().isActive()) {
            sendErrorResponse(ctx, cause, protocolType);
        }

        // Determine if connection should be closed
        if (shouldCloseConnection(cause, protocolType)) {
            ctx.close();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            logger.debug("Idle state event: {} for channel: {}", event.state(), ctx.channel().remoteAddress());
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * Send appropriate error response based on protocol type.
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, Throwable cause, String protocolType) {
        if (protocolType == null) {
            protocolType = NettyContext.PROTOCOL_TCP;
        }

        try {
            switch (protocolType) {
                case NettyContext.PROTOCOL_HTTP -> sendHttpErrorResponse(ctx, cause);
                case NettyContext.PROTOCOL_WEBSOCKET -> sendWebSocketErrorResponse(ctx, cause);
                case NettyContext.PROTOCOL_TCP, NettyContext.PROTOCOL_UDP -> {
                    // For TCP/UDP, typically don't send error response
                    // Application should handle errors at message level
                    logger.debug("No error response sent for {} protocol", protocolType);
                }
                default -> logger.debug("Unknown protocol type: {}, no error response sent", protocolType);
            }
        } catch (Exception e) {
            logger.debug("Failed to send error response", e);
        }
    }

    /**
     * Send HTTP error response.
     */
    private void sendHttpErrorResponse(ChannelHandlerContext ctx, Throwable cause) {
        String errorJson = "{\"error\":\"Internal Server Error\",\"message\":\"" +
                escapeJson(cause.getMessage()) + "\"}";
        byte[] content = errorJson.getBytes(StandardCharsets.UTF_8);

        ByteBuf buffer = ctx.alloc().buffer(content.length);
        buffer.writeBytes(content);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.INTERNAL_SERVER_ERROR,
                buffer
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);

        ctx.writeAndFlush(response);
    }

    /**
     * Send WebSocket close frame with error.
     */
    private void sendWebSocketErrorResponse(ChannelHandlerContext ctx, Throwable cause) {
        String reason = cause.getMessage();
        if (reason == null || reason.length() > 123) {
            // WebSocket close reason is limited to 123 bytes
            reason = "Internal error";
        }
        ctx.writeAndFlush(new CloseWebSocketFrame(1011, reason));
    }

    /**
     * Determine if the connection should be closed after the exception.
     */
    private boolean shouldCloseConnection(Throwable cause, String protocolType) {
        // Always close on fatal errors
        if (cause instanceof OutOfMemoryError) {
            return true;
        }

        // Close on connection-related exceptions
        if (isConnectionResetException(cause)) {
            return true;
        }

        // Close on timeout exceptions
        if (cause instanceof ReadTimeoutException || cause instanceof WriteTimeoutException) {
            return true;
        }

        // For HTTP, close after error (unless keep-alive is properly handled)
        if (NettyContext.PROTOCOL_HTTP.equals(protocolType)) {
            return true;
        }

        // For WebSocket, close after sending close frame
        if (NettyContext.PROTOCOL_WEBSOCKET.equals(protocolType)) {
            return true;
        }

        // For TCP, keep connection open for recoverable errors
        // Application can decide to close by throwing specific exceptions
        return false;
    }

    /**
     * Check if the exception is a connection reset exception.
     */
    private boolean isConnectionResetException(Throwable cause) {
        if (cause instanceof IOException) {
            String message = cause.getMessage();
            return message != null && (
                    message.contains("Connection reset") ||
                    message.contains("Broken pipe") ||
                    message.contains("远程主机强迫关闭") ||
                    message.contains("forcibly closed")
            );
        }
        return false;
    }

    /**
     * Escape special characters for JSON string.
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "Unknown error";
        }
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

}
