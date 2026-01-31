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
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * Channel handler for collecting server metrics.
 *
 * <p>This handler tracks:
 * <ul>
 * <li>Connection counts (current and total) via channelActive/channelInactive</li>
 * <li>Bytes received via channelRead</li>
 * <li>Bytes sent via write</li>
 * </ul>
 *
 * <p>This handler should be placed early in the pipeline to capture all bytes
 * before any decoding/encoding happens.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class MetricsChannelHandler extends ChannelDuplexHandler {

    private final ServerMetrics serverMetrics;

    /**
     * Create a new MetricsChannelHandler.
     * @param serverMetrics the server metrics collector
     */
    public MetricsChannelHandler(ServerMetrics serverMetrics) {
        this.serverMetrics = serverMetrics;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        serverMetrics.connectionOpened();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        serverMetrics.connectionClosed();
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        long bytes = calculateReadableBytes(msg);
        if (bytes > 0) {
            serverMetrics.addBytesIn(bytes);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        long bytes = calculateReadableBytes(msg);
        if (bytes > 0) {
            serverMetrics.addBytesOut(bytes);
        }
        super.write(ctx, msg, promise);
    }

    /**
     * Calculate the readable bytes from a message.
     * @param msg the message
     * @return the number of readable bytes, or 0 if unknown
     */
    private long calculateReadableBytes(Object msg) {
        if (msg instanceof ByteBuf) {
            return ((ByteBuf) msg).readableBytes();
        } else if (msg instanceof ByteBufHolder) {
            return ((ByteBufHolder) msg).content().readableBytes();
        }
        return 0;
    }

}
