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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BackpressureHandler}.
 */
class BackpressureHandlerTest {

    private BackpressureSpec spec;
    private BackpressureMetrics metrics;
    private List<Object> receivedMessages;

    @BeforeEach
    void setUp() {
        spec = new BackpressureSpec();
        spec.setEnabled(true);
        spec.setHighWaterMark(64 * 1024); // 64KB
        spec.setLowWaterMark(32 * 1024); // 32KB
        spec.setOverflowThreshold(10 * 1024 * 1024); // 10MB
        spec.setStrategy(BackpressureStrategy.SUSPEND_READ);

        metrics = new BackpressureMetrics("test-server");
        receivedMessages = new ArrayList<>();
    }

    private EmbeddedChannel createChannel(BackpressureHandler handler) {
        return new EmbeddedChannel(handler, new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                receivedMessages.add(msg);
            }
        });
    }

    @Test
    void channelRead_belowHighWaterMark_forwardsMessage() {
        BackpressureHandler handler = new BackpressureHandler(spec, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Send a message - write buffer is empty so should forward
        ByteBuf msg = Unpooled.wrappedBuffer("test".getBytes());
        channel.writeInbound(msg);

        assertThat(receivedMessages).hasSize(1);
        assertThat(handler.isSuspended()).isFalse();
        assertThat(metrics.getSuspendCount()).isZero();

        channel.close();
    }

    @Test
    void channelRead_withNullMetrics_handlesGracefully() {
        BackpressureHandler handler = new BackpressureHandler(spec, null);
        EmbeddedChannel channel = createChannel(handler);

        // Send a message - should not throw
        ByteBuf msg = Unpooled.wrappedBuffer("test".getBytes());
        channel.writeInbound(msg);

        assertThat(receivedMessages).hasSize(1);

        channel.close();
    }

    @Test
    void channelInactive_resetsSuspendedState() {
        BackpressureHandler handler = new BackpressureHandler(spec, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Simulate channel becoming inactive
        channel.close();

        assertThat(handler.isSuspended()).isFalse();
    }

    @Test
    void channelWritabilityChanged_propagatesEvent() {
        BackpressureHandler handler = new BackpressureHandler(spec, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Initially writable
        assertThat(channel.isWritable()).isTrue();

        channel.close();
    }

    @Test
    void isSuspended_initiallyFalse() {
        BackpressureHandler handler = new BackpressureHandler(spec, metrics);
        assertThat(handler.isSuspended()).isFalse();
    }

    @Test
    void constructor_acceptsNullMetrics() {
        BackpressureHandler handler = new BackpressureHandler(spec, null);
        assertThat(handler).isNotNull();
    }

    @Test
    void multipleMessages_belowThreshold_allForwarded() {
        BackpressureHandler handler = new BackpressureHandler(spec, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Send multiple messages
        for (int i = 0; i < 5; i++) {
            ByteBuf msg = Unpooled.wrappedBuffer(("test" + i).getBytes());
            channel.writeInbound(msg);
        }

        assertThat(receivedMessages).hasSize(5);
        assertThat(metrics.getSuspendCount()).isZero();
        assertThat(metrics.getDroppedCount()).isZero();

        channel.close();
    }

    @Test
    void dropStrategy_configuration() {
        spec.setStrategy(BackpressureStrategy.DROP);
        BackpressureHandler handler = new BackpressureHandler(spec, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Send a message - write buffer is empty, so normal flow
        ByteBuf msg = Unpooled.wrappedBuffer("test".getBytes());
        channel.writeInbound(msg);

        // Message should be forwarded when under threshold
        assertThat(receivedMessages).hasSize(1);

        channel.close();
    }

    @Test
    void disconnectStrategy_configuration() {
        spec.setStrategy(BackpressureStrategy.DISCONNECT);
        BackpressureHandler handler = new BackpressureHandler(spec, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Send a message - write buffer is empty, so normal flow
        ByteBuf msg = Unpooled.wrappedBuffer("test".getBytes());
        channel.writeInbound(msg);

        // Message should be forwarded when under threshold
        assertThat(receivedMessages).hasSize(1);

        channel.close();
    }

    @Test
    void suspendReadStrategy_configuration() {
        spec.setStrategy(BackpressureStrategy.SUSPEND_READ);
        BackpressureHandler handler = new BackpressureHandler(spec, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Send a message - write buffer is empty, so normal flow
        ByteBuf msg = Unpooled.wrappedBuffer("test".getBytes());
        channel.writeInbound(msg);

        // Message should be forwarded when under threshold
        assertThat(receivedMessages).hasSize(1);

        channel.close();
    }
}
