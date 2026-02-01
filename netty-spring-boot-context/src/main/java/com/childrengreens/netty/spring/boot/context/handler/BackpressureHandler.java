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

import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureMetrics;
import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureSpec;
import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureStrategy;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel handler for backpressure management.
 *
 * <p>This handler monitors the channel's write buffer and applies backpressure
 * when it exceeds the configured high water mark. The behavior depends on the
 * configured strategy:
 * <ul>
 * <li>{@link BackpressureStrategy#SUSPEND_READ}: Stops reading from the channel</li>
 * <li>{@link BackpressureStrategy#DROP}: Drops incoming messages</li>
 * <li>{@link BackpressureStrategy#DISCONNECT}: Closes the connection</li>
 * </ul>
 *
 * <p>This handler should be placed early in the pipeline (after SSL if present)
 * to effectively control the flow of incoming data.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class BackpressureHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(BackpressureHandler.class);

    private final BackpressureSpec spec;
    private final BackpressureMetrics metrics;

    private volatile boolean suspended = false;

    /**
     * Create a new BackpressureHandler.
     * @param spec the backpressure specification
     * @param metrics the metrics collector (may be null)
     */
    public BackpressureHandler(BackpressureSpec spec, BackpressureMetrics metrics) {
        this.spec = spec;
        this.metrics = metrics;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        long pendingBytes = getPendingWriteBytes(ctx);

        // Check for overflow threshold (DISCONNECT strategy)
        if (spec.getStrategy() == BackpressureStrategy.DISCONNECT
                && pendingBytes > spec.getOverflowThreshold()) {
            handleDisconnect(ctx, pendingBytes);
            ReferenceCountUtil.release(msg);
            return;
        }

        // Check high water mark
        if (pendingBytes > spec.getHighWaterMark()) {
            switch (spec.getStrategy()) {
                case SUSPEND_READ:
                    handleSuspend(ctx, pendingBytes);
                    // Still forward the message, but stop reading more
                    ctx.fireChannelRead(msg);
                    break;

                case DROP:
                    handleDrop(ctx, msg, pendingBytes);
                    // Message is dropped, don't forward
                    break;

                case DISCONNECT:
                    handleDisconnect(ctx, pendingBytes);
                    ReferenceCountUtil.release(msg);
                    break;
            }
        } else {
            // Normal flow
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isWritable() && suspended) {
            resume(ctx);
        }
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Reset state on disconnect
        suspended = false;
        ctx.fireChannelInactive();
    }

    /**
     * Handle suspend read strategy.
     */
    private void handleSuspend(ChannelHandlerContext ctx, long pendingBytes) {
        if (!suspended) {
            suspended = true;
            ctx.channel().config().setAutoRead(false);
            if (metrics != null) {
                metrics.incrementSuspend();
            }
            logger.debug("Backpressure: suspended reading on channel {}, pending bytes: {}",
                    ctx.channel().remoteAddress(), pendingBytes);
        }
    }

    /**
     * Resume reading after backpressure.
     */
    private void resume(ChannelHandlerContext ctx) {
        suspended = false;
        ctx.channel().config().setAutoRead(true);
        if (metrics != null) {
            metrics.incrementResume();
        }
        logger.debug("Backpressure: resumed reading on channel {}",
                ctx.channel().remoteAddress());
    }

    /**
     * Handle drop strategy.
     */
    private void handleDrop(ChannelHandlerContext ctx, Object msg, long pendingBytes) {
        ReferenceCountUtil.release(msg);
        if (metrics != null) {
            metrics.incrementDropped();
        }
        logger.debug("Backpressure: dropped message on channel {}, pending bytes: {}",
                ctx.channel().remoteAddress(), pendingBytes);
    }

    /**
     * Handle disconnect strategy.
     */
    private void handleDisconnect(ChannelHandlerContext ctx, long pendingBytes) {
        if (metrics != null) {
            metrics.incrementDisconnect();
        }
        logger.warn("Backpressure: disconnecting channel {} due to overflow, pending bytes: {}",
                ctx.channel().remoteAddress(), pendingBytes);
        ctx.close();
    }

    /**
     * Get the number of bytes pending in the outbound buffer.
     */
    private long getPendingWriteBytes(ChannelHandlerContext ctx) {
        ChannelOutboundBuffer buffer = ctx.channel().unsafe().outboundBuffer();
        return buffer != null ? buffer.totalPendingWriteBytes() : 0;
    }

    /**
     * Return whether reading is currently suspended.
     * @return true if suspended
     */
    public boolean isSuspended() {
        return suspended;
    }

}
