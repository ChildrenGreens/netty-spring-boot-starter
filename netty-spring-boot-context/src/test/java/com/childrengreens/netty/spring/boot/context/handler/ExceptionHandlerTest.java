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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.Attribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ExceptionHandler}.
 */
class ExceptionHandlerTest {

    private ExceptionHandler handler;
    private ChannelHandlerContext ctx;
    private Channel channel;
    private ChannelFuture channelFuture;
    private ChannelPipeline pipeline;

    @BeforeEach
    void setUp() {
        handler = new ExceptionHandler();
        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);
        channelFuture = mock(ChannelFuture.class);
        pipeline = mock(ChannelPipeline.class);
        when(ctx.channel()).thenReturn(channel);
        when(ctx.pipeline()).thenReturn(pipeline);
        when(ctx.writeAndFlush(any())).thenReturn(channelFuture);
        when(channelFuture.addListener(any())).thenReturn(channelFuture);
        // Default: no protocol type attribute set
        when(channel.hasAttr(NettyContext.PROTOCOL_TYPE_KEY)).thenReturn(false);
        // Default: empty pipeline for protocol detection
        when(pipeline.toMap()).thenReturn(new LinkedHashMap<>());
    }

    @Test
    void exceptionCaught_withHttpProtocol_sendsHttpErrorResponseWithCloseListener() {
        // Set up HTTP protocol
        setupProtocolType(NettyContext.PROTOCOL_HTTP);
        when(channel.isActive()).thenReturn(true);
        when(ctx.alloc()).thenReturn(io.netty.buffer.UnpooledByteBufAllocator.DEFAULT);

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        // Verify HTTP response sent with close listener
        verify(ctx).writeAndFlush(any());
        verify(channelFuture).addListener(any());
        // ctx.close() should NOT be called directly for HTTP - close is via listener
        verify(ctx, never()).close();
    }

    @Test
    void exceptionCaught_withWebSocketProtocol_sendsCloseFrameWithCloseListener() {
        // Set up WebSocket protocol
        setupProtocolType(NettyContext.PROTOCOL_WEBSOCKET);
        when(channel.isActive()).thenReturn(true);

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        // Verify CloseWebSocketFrame sent
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ctx).writeAndFlush(captor.capture());
        assertInstanceOf(CloseWebSocketFrame.class, captor.getValue());

        // Verify close listener added
        verify(channelFuture).addListener(any());

        // ctx.close() should NOT be called directly for WebSocket - close is via listener
        verify(ctx, never()).close();
    }

    @Test
    void exceptionCaught_withWebSocketProtocol_sendsCloseFrameWithStatusCode1011() {
        // Set up WebSocket protocol
        setupProtocolType(NettyContext.PROTOCOL_WEBSOCKET);
        when(channel.isActive()).thenReturn(true);

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        // Verify CloseWebSocketFrame with correct status code (1011 = Internal Error)
        ArgumentCaptor<CloseWebSocketFrame> captor = ArgumentCaptor.forClass(CloseWebSocketFrame.class);
        verify(ctx).writeAndFlush(captor.capture());
        CloseWebSocketFrame frame = captor.getValue();
        assertEquals(1011, frame.statusCode());
    }

    @Test
    void exceptionCaught_withWebSocketProtocol_truncatesLongReasonText() {
        // Set up WebSocket protocol
        setupProtocolType(NettyContext.PROTOCOL_WEBSOCKET);
        when(channel.isActive()).thenReturn(true);

        // Create exception with message longer than 123 bytes
        String longMessage = "A".repeat(200);
        handler.exceptionCaught(ctx, new RuntimeException(longMessage));

        // Verify CloseWebSocketFrame reason is truncated
        ArgumentCaptor<CloseWebSocketFrame> captor = ArgumentCaptor.forClass(CloseWebSocketFrame.class);
        verify(ctx).writeAndFlush(captor.capture());
        CloseWebSocketFrame frame = captor.getValue();
        assertEquals("Internal error", frame.reasonText());
    }

    @Test
    void exceptionCaught_withWebSocketProtocol_handlesNullMessage() {
        // Set up WebSocket protocol
        setupProtocolType(NettyContext.PROTOCOL_WEBSOCKET);
        when(channel.isActive()).thenReturn(true);

        // Create exception with null message
        handler.exceptionCaught(ctx, new RuntimeException((String) null));

        // Verify CloseWebSocketFrame with default reason
        ArgumentCaptor<CloseWebSocketFrame> captor = ArgumentCaptor.forClass(CloseWebSocketFrame.class);
        verify(ctx).writeAndFlush(captor.capture());
        CloseWebSocketFrame frame = captor.getValue();
        assertEquals("Internal error", frame.reasonText());
    }

    @Test
    void exceptionCaught_withTcpProtocol_doesNotCloseForRecoverableError() {
        // TCP protocol - should not close for recoverable errors
        setupProtocolType(NettyContext.PROTOCOL_TCP);
        when(channel.isActive()).thenReturn(true);

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        // TCP doesn't send error response by default
        verify(ctx, never()).writeAndFlush(any());
        // TCP doesn't close for recoverable errors
        verify(ctx, never()).close();
    }

    @Test
    void exceptionCaught_withUdpProtocol_doesNotSendResponseOrClose() {
        // UDP protocol - no response, no close (UDP is connectionless)
        setupProtocolType(NettyContext.PROTOCOL_UDP);
        when(channel.isActive()).thenReturn(true);

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        // UDP doesn't send error response
        verify(ctx, never()).writeAndFlush(any());
        // UDP doesn't close for recoverable errors
        verify(ctx, never()).close();
    }

    @Test
    void exceptionCaught_withInactiveChannel_doesNotSendResponse() {
        when(channel.isActive()).thenReturn(false);

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        verify(ctx, never()).writeAndFlush(any());
    }

    @Test
    void exceptionCaught_connectionReset_closesConnection() {
        when(channel.isActive()).thenReturn(false);

        handler.exceptionCaught(ctx, new IOException("Connection reset by peer"));

        verify(ctx).close();
    }

    @Test
    void exceptionCaught_readTimeout_closesConnection() {
        setupProtocolType(NettyContext.PROTOCOL_TCP);
        when(channel.isActive()).thenReturn(true);

        handler.exceptionCaught(ctx, ReadTimeoutException.INSTANCE);

        verify(ctx).close();
    }

    @Test
    void exceptionCaught_whenWriteFails_handlesGracefully() {
        setupProtocolType(NettyContext.PROTOCOL_HTTP);
        when(channel.isActive()).thenReturn(true);
        when(ctx.alloc()).thenReturn(io.netty.buffer.UnpooledByteBufAllocator.DEFAULT);
        when(ctx.writeAndFlush(any())).thenThrow(new RuntimeException("Write failed"));

        // Should not throw exception
        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        // Write failed, but no explicit close for HTTP (would happen via listener)
        verify(ctx, never()).close();
    }

    @Test
    void exceptionCaught_withWebSocketProtocol_whenWriteFails_handlesGracefully() {
        setupProtocolType(NettyContext.PROTOCOL_WEBSOCKET);
        when(channel.isActive()).thenReturn(true);
        when(ctx.writeAndFlush(any())).thenThrow(new RuntimeException("Write failed"));

        // Should not throw exception
        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        // Write failed, but no explicit close for WebSocket (would happen via listener)
        verify(ctx, never()).close();
    }

    private void setupProtocolType(String protocolType) {
        when(channel.hasAttr(NettyContext.PROTOCOL_TYPE_KEY)).thenReturn(true);
        @SuppressWarnings("unchecked")
        Attribute<String> attr = mock(Attribute.class);
        when(channel.attr(NettyContext.PROTOCOL_TYPE_KEY)).thenReturn(attr);
        when(attr.get()).thenReturn(protocolType);
    }

    // ==================== Protocol Detection Tests ====================

    @Test
    void exceptionCaught_withNoProtocolAttribute_detectsHttpFromHttpServerCodec() {
        // No protocol type attribute set
        when(channel.hasAttr(NettyContext.PROTOCOL_TYPE_KEY)).thenReturn(false);
        when(channel.isActive()).thenReturn(true);
        when(ctx.alloc()).thenReturn(io.netty.buffer.UnpooledByteBufAllocator.DEFAULT);

        // Set up pipeline with HttpServerCodec (real instance)
        Map<String, io.netty.channel.ChannelHandler> handlers = new LinkedHashMap<>();
        handlers.put("httpCodec", new HttpServerCodec());
        when(pipeline.toMap()).thenReturn(handlers);

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        // Should send HTTP response (detected from pipeline)
        verify(ctx).writeAndFlush(any());
        verify(channelFuture).addListener(any());
        // HTTP closes via listener, not directly
        verify(ctx, never()).close();
    }

    @Test
    void exceptionCaught_withNoProtocolAttribute_detectsWebSocketFromWebSocketHandler() {
        // No protocol type attribute set
        when(channel.hasAttr(NettyContext.PROTOCOL_TYPE_KEY)).thenReturn(false);
        when(channel.isActive()).thenReturn(true);

        // Set up pipeline with both HTTP and WebSocket handlers
        // WebSocket should take precedence
        Map<String, io.netty.channel.ChannelHandler> handlers = new LinkedHashMap<>();
        handlers.put("httpCodec", new HttpServerCodec());
        handlers.put("wsHandler", new WebSocketServerProtocolHandler("/ws"));
        when(pipeline.toMap()).thenReturn(handlers);

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        // Should send WebSocket close frame (detected from pipeline)
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ctx).writeAndFlush(captor.capture());
        assertInstanceOf(CloseWebSocketFrame.class, captor.getValue());
    }

    @Test
    void exceptionCaught_withNoProtocolAttribute_defaultsToTcpWhenNoPipelineHandlers() {
        // No protocol type attribute set
        when(channel.hasAttr(NettyContext.PROTOCOL_TYPE_KEY)).thenReturn(false);
        when(channel.isActive()).thenReturn(true);

        // Empty pipeline - no HTTP or WebSocket handlers
        when(pipeline.toMap()).thenReturn(new LinkedHashMap<>());

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        // Should default to TCP behavior (no response sent)
        verify(ctx, never()).writeAndFlush(any());
        // TCP doesn't close for recoverable errors
        verify(ctx, never()).close();
    }

    @Test
    void exceptionCaught_withNoProtocolAttribute_closesConnectionForHttpDetected() {
        // No protocol type attribute set
        when(channel.hasAttr(NettyContext.PROTOCOL_TYPE_KEY)).thenReturn(false);
        when(channel.isActive()).thenReturn(true);
        when(ctx.alloc()).thenReturn(io.netty.buffer.UnpooledByteBufAllocator.DEFAULT);

        // Set up pipeline with HttpServerCodec (real instance)
        Map<String, io.netty.channel.ChannelHandler> handlers = new LinkedHashMap<>();
        handlers.put("httpCodec", new HttpServerCodec());
        when(pipeline.toMap()).thenReturn(handlers);

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        // HTTP should close connection after sending response (via listener)
        verify(channelFuture).addListener(any());
        // Not closed directly for HTTP
        verify(ctx, never()).close();
    }
}
