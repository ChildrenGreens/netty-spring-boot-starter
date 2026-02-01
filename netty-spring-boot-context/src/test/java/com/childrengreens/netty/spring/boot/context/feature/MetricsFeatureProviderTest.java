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
import com.childrengreens.netty.spring.boot.context.handler.MetricsChannelHandler;
import com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsFeatureProvider}.
 */
class MetricsFeatureProviderTest {

    private MetricsFeatureProvider featureProvider;
    private ServerSpec serverSpec;

    @BeforeEach
    void setUp() {
        featureProvider = new MetricsFeatureProvider();
        serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
    }

    @Test
    void getName_returnsMetrics() {
        assertThat(featureProvider.getName()).isEqualTo("metrics");
    }

    @Test
    void getOrder_returnsVeryLowOrder() {
        // Metrics should be very early in the pipeline
        assertThat(featureProvider.getOrder()).isEqualTo(5);
    }

    @Test
    void isEnabled_alwaysReturnsTrue() {
        assertThat(featureProvider.isEnabled(serverSpec)).isTrue();
    }

    @Test
    void configure_withServerMetricsAttribute_addsMetricsHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        // Set up server metrics in channel attribute
        ServerMetrics serverMetrics = new ServerMetrics("test-server");
        channel.attr(NettyContext.SERVER_METRICS_KEY).set(serverMetrics);

        featureProvider.configure(pipeline, serverSpec);

        assertThat(pipeline.get("metricsHandler")).isNotNull();
        assertThat(pipeline.get("metricsHandler")).isInstanceOf(MetricsChannelHandler.class);

        channel.close();
    }

    @Test
    void configure_withoutServerMetricsAttribute_doesNotAddHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        // Don't set server metrics attribute
        featureProvider.configure(pipeline, serverSpec);

        assertThat(pipeline.get("metricsHandler")).isNull();

        channel.close();
    }

}
