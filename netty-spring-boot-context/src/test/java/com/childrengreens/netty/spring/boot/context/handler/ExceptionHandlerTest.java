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
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.Attribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ExceptionHandler}.
 */
class ExceptionHandlerTest {

    private ExceptionHandler handler;
    private ChannelHandlerContext ctx;
    private Channel channel;

    @BeforeEach
    void setUp() {
        handler = new ExceptionHandler();
        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        // Default: no protocol type attribute set
        when(channel.hasAttr(NettyContext.PROTOCOL_TYPE_KEY)).thenReturn(false);
    }

    @Test
    void exceptionCaught_withHttpProtocol_sendsHttpErrorResponseAndCloses() {
        // Set up HTTP protocol
        setupProtocolType(NettyContext.PROTOCOL_HTTP);
        when(channel.isActive()).thenReturn(true);
        when(ctx.alloc()).thenReturn(io.netty.buffer.UnpooledByteBufAllocator.DEFAULT);
        ChannelFuture future = mock(ChannelFuture.class);
        when(ctx.writeAndFlush(any())).thenReturn(future);

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        verify(ctx).writeAndFlush(any());
        verify(ctx).close();
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

        // Should still try to close for HTTP
        verify(ctx).close();
    }

    private void setupProtocolType(String protocolType) {
        when(channel.hasAttr(NettyContext.PROTOCOL_TYPE_KEY)).thenReturn(true);
        @SuppressWarnings("unchecked")
        Attribute<String> attr = mock(Attribute.class);
        when(channel.attr(NettyContext.PROTOCOL_TYPE_KEY)).thenReturn(attr);
        when(attr.get()).thenReturn(protocolType);
    }
}
