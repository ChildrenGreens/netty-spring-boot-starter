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

import com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link MetricsChannelHandler}.
 */
class MetricsChannelHandlerTest {

    private MetricsChannelHandler handler;
    private ServerMetrics serverMetrics;
    private ChannelHandlerContext ctx;
    private ChannelPromise promise;

    @BeforeEach
    void setUp() {
        serverMetrics = mock(ServerMetrics.class);
        handler = new MetricsChannelHandler(serverMetrics);
        ctx = mock(ChannelHandlerContext.class);
        promise = mock(ChannelPromise.class);
    }

    @Test
    void channelActive_incrementsConnectionCount() throws Exception {
        handler.channelActive(ctx);

        verify(serverMetrics).connectionOpened();
        verify(ctx).fireChannelActive();
    }

    @Test
    void channelInactive_decrementsConnectionCount() throws Exception {
        handler.channelInactive(ctx);

        verify(serverMetrics).connectionClosed();
        verify(ctx).fireChannelInactive();
    }

    @Test
    void channelRead_withByteBuf_recordsBytesIn() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[100]);

        handler.channelRead(ctx, buf);

        verify(serverMetrics).addBytesIn(100);
        verify(ctx).fireChannelRead(buf);
        buf.release();
    }

    @Test
    void channelRead_withByteBufHolder_recordsBytesIn() throws Exception {
        ByteBuf content = Unpooled.wrappedBuffer(new byte[50]);
        ByteBufHolder holder = mock(ByteBufHolder.class);
        when(holder.content()).thenReturn(content);

        handler.channelRead(ctx, holder);

        verify(serverMetrics).addBytesIn(50);
        verify(ctx).fireChannelRead(holder);
        content.release();
    }

    @Test
    void channelRead_withNonByteBuf_doesNotRecordBytes() throws Exception {
        Object msg = new Object();

        handler.channelRead(ctx, msg);

        verify(serverMetrics, never()).addBytesIn(anyLong());
        verify(ctx).fireChannelRead(msg);
    }

    @Test
    void channelRead_withEmptyByteBuf_doesNotRecordBytes() throws Exception {
        ByteBuf buf = Unpooled.EMPTY_BUFFER;

        handler.channelRead(ctx, buf);

        verify(serverMetrics, never()).addBytesIn(anyLong());
        verify(ctx).fireChannelRead(buf);
    }

    @Test
    void write_withByteBuf_recordsBytesOut() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[200]);

        handler.write(ctx, buf, promise);

        verify(serverMetrics).addBytesOut(200);
        verify(ctx).write(buf, promise);
        buf.release();
    }

    @Test
    void write_withByteBufHolder_recordsBytesOut() throws Exception {
        ByteBuf content = Unpooled.wrappedBuffer(new byte[75]);
        ByteBufHolder holder = mock(ByteBufHolder.class);
        when(holder.content()).thenReturn(content);

        handler.write(ctx, holder, promise);

        verify(serverMetrics).addBytesOut(75);
        verify(ctx).write(holder, promise);
        content.release();
    }

    @Test
    void write_withNonByteBuf_doesNotRecordBytes() throws Exception {
        Object msg = new Object();

        handler.write(ctx, msg, promise);

        verify(serverMetrics, never()).addBytesOut(anyLong());
        verify(ctx).write(msg, promise);
    }

    @Test
    void write_withEmptyByteBuf_doesNotRecordBytes() throws Exception {
        ByteBuf buf = Unpooled.EMPTY_BUFFER;

        handler.write(ctx, buf, promise);

        verify(serverMetrics, never()).addBytesOut(anyLong());
        verify(ctx).write(buf, promise);
    }
}
