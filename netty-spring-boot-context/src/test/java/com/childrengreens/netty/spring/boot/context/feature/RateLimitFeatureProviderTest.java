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

import com.childrengreens.netty.spring.boot.context.properties.FeaturesSpec;
import com.childrengreens.netty.spring.boot.context.properties.RateLimitSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
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
}
