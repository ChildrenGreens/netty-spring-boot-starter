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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    }

    @Test
    void exceptionCaught_withActiveChannel_sendsErrorResponseAndCloses() {
        when(channel.isActive()).thenReturn(true);
        ChannelFuture future = mock(ChannelFuture.class);
        when(ctx.writeAndFlush(any())).thenReturn(future);

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        verify(ctx).writeAndFlush(any());
        verify(ctx).close();
    }

    @Test
    void exceptionCaught_withInactiveChannel_onlyCloses() {
        when(channel.isActive()).thenReturn(false);

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        verify(ctx, never()).writeAndFlush(any());
        verify(ctx).close();
    }

    @Test
    void exceptionCaught_whenWriteFails_stillCloses() {
        when(channel.isActive()).thenReturn(true);
        when(ctx.writeAndFlush(any())).thenThrow(new RuntimeException("Write failed"));

        handler.exceptionCaught(ctx, new RuntimeException("Test error"));

        verify(ctx).close();
    }
}
