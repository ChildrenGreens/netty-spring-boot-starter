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

import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureMetrics;
import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureSpec;
import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureStrategy;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.handler.BackpressureHandler;
import com.childrengreens.netty.spring.boot.context.properties.FeaturesSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BackpressureFeatureProvider}.
 */
class BackpressureFeatureProviderTest {

    private BackpressureFeatureProvider featureProvider;
    private ServerSpec serverSpec;
    private BackpressureSpec backpressureSpec;

    @BeforeEach
    void setUp() {
        featureProvider = new BackpressureFeatureProvider();
        serverSpec = new ServerSpec();
        serverSpec.setName("test-server");

        backpressureSpec = new BackpressureSpec();
        backpressureSpec.setEnabled(true);
        backpressureSpec.setHighWaterMark(64 * 1024);
        backpressureSpec.setLowWaterMark(32 * 1024);
        backpressureSpec.setStrategy(BackpressureStrategy.SUSPEND_READ);
        backpressureSpec.setMetrics(true);

        FeaturesSpec features = new FeaturesSpec();
        features.setBackpressure(backpressureSpec);
        serverSpec.setFeatures(features);
    }

    @Test
    void getName_returnsBackpressure() {
        assertThat(featureProvider.getName()).isEqualTo("backpressure");
    }

    @Test
    void getOrder_returnsCorrectOrder() {
        // Should be after metrics (5) and SSL (10)
        assertThat(featureProvider.getOrder()).isEqualTo(15);
    }

    @Test
    void isEnabled_whenEnabled_returnsTrue() {
        assertThat(featureProvider.isEnabled(serverSpec)).isTrue();
    }

    @Test
    void isEnabled_whenDisabled_returnsFalse() {
        backpressureSpec.setEnabled(false);
        assertThat(featureProvider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void isEnabled_whenSpecNull_returnsFalse() {
        serverSpec.getFeatures().setBackpressure(null);
        assertThat(featureProvider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void configure_addsBackpressureHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        featureProvider.configure(pipeline, serverSpec);

        assertThat(pipeline.get("backpressureHandler")).isNotNull();
        assertThat(pipeline.get("backpressureHandler")).isInstanceOf(BackpressureHandler.class);

        channel.close();
    }

    @Test
    void configure_setsWriteBufferWaterMark() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        featureProvider.configure(pipeline, serverSpec);

        WriteBufferWaterMark waterMark = channel.config().getWriteBufferWaterMark();
        assertThat(waterMark.low()).isEqualTo(32 * 1024);
        assertThat(waterMark.high()).isEqualTo(64 * 1024);

        channel.close();
    }

    @Test
    void configure_withMetricsEnabled_createsMetrics() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        featureProvider.configure(pipeline, serverSpec);

        BackpressureMetrics metrics = channel.attr(NettyContext.BACKPRESSURE_METRICS_KEY).get();
        assertThat(metrics).isNotNull();
        assertThat(metrics.getServerName()).isEqualTo("test-server");

        channel.close();
    }

    @Test
    void configure_withMetricsDisabled_noMetrics() {
        backpressureSpec.setMetrics(false);
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        featureProvider.configure(pipeline, serverSpec);

        BackpressureMetrics metrics = channel.attr(NettyContext.BACKPRESSURE_METRICS_KEY).get();
        assertThat(metrics).isNull();

        channel.close();
    }

    @Test
    void configure_withExistingMetricsAttribute_usesExisting() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        // Set existing metrics
        BackpressureMetrics existingMetrics = new BackpressureMetrics("existing-server");
        channel.attr(NettyContext.BACKPRESSURE_METRICS_KEY).set(existingMetrics);

        featureProvider.configure(pipeline, serverSpec);

        BackpressureMetrics metrics = channel.attr(NettyContext.BACKPRESSURE_METRICS_KEY).get();
        assertThat(metrics).isSameAs(existingMetrics);

        channel.close();
    }

    @Test
    void configure_whenDisabled_doesNotAddHandler() {
        backpressureSpec.setEnabled(false);
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        featureProvider.configure(pipeline, serverSpec);

        assertThat(pipeline.get("backpressureHandler")).isNull();

        channel.close();
    }

    @Test
    void configure_whenSpecNull_doesNotAddHandler() {
        serverSpec.getFeatures().setBackpressure(null);
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        featureProvider.configure(pipeline, serverSpec);

        assertThat(pipeline.get("backpressureHandler")).isNull();

        channel.close();
    }
}
