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

package com.childrengreens.netty.spring.boot.context.feature;

import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.properties.FeaturesSpec;
import com.childrengreens.netty.spring.boot.context.properties.RateLimitAction;
import com.childrengreens.netty.spring.boot.context.properties.RateLimitSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link RateLimitFeatureProvider}.
 */
class RateLimitFeatureProviderTest {

    private RateLimitFeatureProvider provider;
    private ChannelPipeline pipeline;
    private ServerSpec serverSpec;
    private FeaturesSpec featuresSpec;
    private RateLimitSpec rateLimitSpec;

    @BeforeEach
    void setUp() {
        provider = new RateLimitFeatureProvider();
        pipeline = mock(ChannelPipeline.class);
        serverSpec = mock(ServerSpec.class);
        featuresSpec = mock(FeaturesSpec.class);
        rateLimitSpec = new RateLimitSpec();

        when(serverSpec.getFeatures()).thenReturn(featuresSpec);
        when(featuresSpec.getRateLimit()).thenReturn(rateLimitSpec);
        when(pipeline.addLast(any(String.class), any())).thenReturn(pipeline);
    }

    @Test
    void getName_returnsRateLimit() {
        assertThat(provider.getName()).isEqualTo("rateLimit");
    }

    @Test
    void getOrder_returns50() {
        assertThat(provider.getOrder()).isEqualTo(50);
    }

    @Test
    void isEnabled_whenRateLimitEnabled_returnsTrue() {
        rateLimitSpec.setEnabled(true);
        assertThat(provider.isEnabled(serverSpec)).isTrue();
    }

    @Test
    void isEnabled_whenRateLimitDisabled_returnsFalse() {
        rateLimitSpec.setEnabled(false);
        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void isEnabled_whenRateLimitSpecNull_returnsFalse() {
        when(featuresSpec.getRateLimit()).thenReturn(null);
        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void configure_whenEnabled_addsRateLimitHandler() {
        rateLimitSpec.setEnabled(true);
        rateLimitSpec.setRequestsPerSecond(100);
        rateLimitSpec.setBurstSize(150);

        provider.configure(pipeline, serverSpec);

        verify(pipeline).addLast(eq("rateLimitHandler"), any());
    }

    @Test
    void configure_whenDisabled_doesNotAddHandler() {
        rateLimitSpec.setEnabled(false);

        provider.configure(pipeline, serverSpec);

        verify(pipeline, never()).addLast(any(String.class), any());
    }

    @Test
    void configure_whenRateLimitSpecNull_doesNotAddHandler() {
        when(featuresSpec.getRateLimit()).thenReturn(null);

        provider.configure(pipeline, serverSpec);

        verify(pipeline, never()).addLast(any(String.class), any());
    }

    @Test
    void configure_withEmbeddedChannel_addsHandler() {
        ServerSpec realServerSpec = new ServerSpec();
        realServerSpec.setName("test-server");
        FeaturesSpec features = new FeaturesSpec();
        RateLimitSpec rateLimit = new RateLimitSpec();
        rateLimit.setEnabled(true);
        rateLimit.setRequestsPerSecond(100);
        rateLimit.setBurstSize(50);
        features.setRateLimit(rateLimit);
        realServerSpec.setFeatures(features);

        EmbeddedChannel channel = new EmbeddedChannel();

        provider.configure(channel.pipeline(), realServerSpec);

        assertThat(channel.pipeline().get("rateLimitHandler")).isNotNull();
        channel.close();
    }

    @Test
    void configure_withZeroBurstSize_usesRequestsPerSecondAsDefault() {
        ServerSpec realServerSpec = new ServerSpec();
        realServerSpec.setName("test-server");
        FeaturesSpec features = new FeaturesSpec();
        RateLimitSpec rateLimit = new RateLimitSpec();
        rateLimit.setEnabled(true);
        rateLimit.setRequestsPerSecond(100);
        rateLimit.setBurstSize(0); // zero burst size
        features.setRateLimit(rateLimit);
        realServerSpec.setFeatures(features);

        EmbeddedChannel channel = new EmbeddedChannel();

        provider.configure(channel.pipeline(), realServerSpec);

        assertThat(channel.pipeline().get("rateLimitHandler")).isNotNull();
        channel.close();
    }

    @Test
    void getName_returnsConstantValue() {
        assertThat(provider.getName()).isEqualTo(RateLimitFeatureProvider.NAME);
    }

    @Test
    void getOrder_returnsConstantValue() {
        assertThat(provider.getOrder()).isEqualTo(RateLimitFeatureProvider.ORDER);
    }

    @Test
    void rateLimitHandler_allowsRequestsWithinLimit() {
        ServerSpec realServerSpec = new ServerSpec();
        realServerSpec.setName("test-server");
        FeaturesSpec features = new FeaturesSpec();
        RateLimitSpec rateLimit = new RateLimitSpec();
        rateLimit.setEnabled(true);
        rateLimit.setRequestsPerSecond(1000); // High limit
        rateLimit.setBurstSize(100);
        features.setRateLimit(rateLimit);
        realServerSpec.setFeatures(features);

        EmbeddedChannel channel = new EmbeddedChannel();
        provider.configure(channel.pipeline(), realServerSpec);

        // Should be able to send messages within the limit
        channel.writeInbound("test message");
        String result = channel.readInbound();
        assertThat(result).isEqualTo("test message");

        channel.close();
    }

    @Test
    void rateLimitHandler_releasesByteBufWhenRateLimitExceeded() {
        // This test verifies the fix for ByteBuf leak when rate limit is exceeded
        // Use CLOSE action to match the original behavior (close connection)
        RateLimitFeatureProvider.RateLimitHandler handler =
                new RateLimitFeatureProvider.RateLimitHandler(1, 1, RateLimitAction.CLOSE);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        // Set protocol type for the handler to recognize
        channel.attr(NettyContext.PROTOCOL_TYPE_KEY).set(NettyContext.PROTOCOL_TCP);

        // First message should pass (uses the initial token)
        ByteBuf buf1 = Unpooled.copiedBuffer("msg1".getBytes());
        channel.writeInbound(buf1);
        ByteBuf received = channel.readInbound();
        assertThat(received).isNotNull();
        received.release();

        // Second message should be rejected and the ByteBuf should be released
        ByteBuf buf2 = Unpooled.copiedBuffer("msg2".getBytes());
        int refCntBefore = buf2.refCnt();
        assertThat(refCntBefore).isEqualTo(1);

        channel.writeInbound(buf2);

        // ByteBuf should be released (refCnt should be 0)
        // Before fix: refCnt would still be 1 (leak)
        // After fix: refCnt should be 0
        assertThat(buf2.refCnt()).isEqualTo(0);

        // Channel should be closed (CLOSE action)
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    void rateLimitHandler_releasesMultipleByteBufsWhenRateLimitExceeded() {
        // Test that ByteBuf is released when rate limit is exceeded
        RateLimitFeatureProvider.RateLimitHandler handler =
                new RateLimitFeatureProvider.RateLimitHandler(1, 1, RateLimitAction.CLOSE);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(NettyContext.PROTOCOL_TYPE_KEY).set(NettyContext.PROTOCOL_TCP);

        // First message passes
        ByteBuf buf1 = Unpooled.copiedBuffer("msg1".getBytes());
        channel.writeInbound(buf1);
        ByteBuf received = channel.readInbound();
        assertThat(received).isNotNull();
        received.release();

        // Second message should be rejected and released
        ByteBuf buf2 = Unpooled.copiedBuffer("msg2".getBytes());
        assertThat(buf2.refCnt()).isEqualTo(1);

        channel.writeInbound(buf2);

        assertThat(buf2.refCnt()).isEqualTo(0);
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    void rateLimitHandler_getters_returnCorrectValues() {
        RateLimitFeatureProvider.RateLimitHandler handler =
                new RateLimitFeatureProvider.RateLimitHandler(100, 50, RateLimitAction.DROP);

        assertThat(handler.getBurstSize()).isEqualTo(50);
        assertThat(handler.getTokens()).isEqualTo(50); // Initial tokens = burst size
        assertThat(handler.getAction()).isEqualTo(RateLimitAction.DROP);
    }

    @Test
    void rateLimitHandler_withZeroBurstSize_usesRequestsPerSecond() {
        RateLimitFeatureProvider.RateLimitHandler handler =
                new RateLimitFeatureProvider.RateLimitHandler(100, 0, RateLimitAction.DROP);

        // When burstSize is 0, it should default to requestsPerSecond
        assertThat(handler.getBurstSize()).isEqualTo(100);
    }

    @Test
    void rateLimitHandler_releasesNonByteBufObjectSafely() {
        // ReferenceCountUtil.release() should handle non-ByteBuf objects gracefully
        RateLimitFeatureProvider.RateLimitHandler handler =
                new RateLimitFeatureProvider.RateLimitHandler(1, 1, RateLimitAction.CLOSE);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(NettyContext.PROTOCOL_TYPE_KEY).set(NettyContext.PROTOCOL_TCP);

        // First message passes
        channel.writeInbound("msg1");
        String received = channel.readInbound();
        assertThat(received).isEqualTo("msg1");

        // Second message (String, not ByteBuf) should be rejected
        // ReferenceCountUtil.release() should not throw for non-reference-counted objects
        channel.writeInbound("msg2");

        // Channel should be closed (CLOSE action)
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    void rateLimitHandler_withDropAction_keepsConnectionOpen() {
        RateLimitFeatureProvider.RateLimitHandler handler =
                new RateLimitFeatureProvider.RateLimitHandler(1, 1, RateLimitAction.DROP);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(NettyContext.PROTOCOL_TYPE_KEY).set(NettyContext.PROTOCOL_TCP);

        // First message passes
        channel.writeInbound("msg1");
        String received = channel.readInbound();
        assertThat(received).isEqualTo("msg1");

        // Second message should be dropped but connection stays open
        channel.writeInbound("msg2");

        // Connection should still be open with DROP action
        assertThat(channel.isOpen()).isTrue();

        channel.close();
    }

    @Test
    void rateLimitHandler_withRejectAction_sendsErrorAndKeepsConnection() {
        RateLimitFeatureProvider.RateLimitHandler handler =
                new RateLimitFeatureProvider.RateLimitHandler(1, 1, RateLimitAction.REJECT);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(NettyContext.PROTOCOL_TYPE_KEY).set(NettyContext.PROTOCOL_TCP);

        // First message passes
        channel.writeInbound("msg1");
        String received = channel.readInbound();
        assertThat(received).isEqualTo("msg1");

        // Second message should trigger error response
        channel.writeInbound("msg2");

        // Connection should still be open with REJECT action
        assertThat(channel.isOpen()).isTrue();

        // Should have written an error response
        Object outbound = channel.readOutbound();
        assertThat(outbound).isNotNull();

        channel.close();
    }

    @Test
    void rateLimitHandler_withNullAction_defaultsToDrop() {
        RateLimitFeatureProvider.RateLimitHandler handler =
                new RateLimitFeatureProvider.RateLimitHandler(1, 1, null);

        assertThat(handler.getAction()).isEqualTo(RateLimitAction.DROP);
    }

    @Test
    void rateLimitSpec_actionDefaultsToDrop() {
        RateLimitSpec spec = new RateLimitSpec();
        assertThat(spec.getAction()).isEqualTo(RateLimitAction.DROP);
    }

    @Test
    void rateLimitSpec_actionCanBeSet() {
        RateLimitSpec spec = new RateLimitSpec();
        spec.setAction(RateLimitAction.CLOSE);
        assertThat(spec.getAction()).isEqualTo(RateLimitAction.CLOSE);

        spec.setAction(RateLimitAction.REJECT);
        assertThat(spec.getAction()).isEqualTo(RateLimitAction.REJECT);
    }
}
