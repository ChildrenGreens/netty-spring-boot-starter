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

import com.childrengreens.netty.spring.boot.context.properties.ConnectionLimitSpec;
import com.childrengreens.netty.spring.boot.context.properties.FeaturesSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ConnectionLimitFeatureProvider}.
 */
class ConnectionLimitFeatureProviderTest {

    private ConnectionLimitFeatureProvider provider;
    private ChannelPipeline pipeline;
    private ServerSpec serverSpec;
    private FeaturesSpec featuresSpec;
    private ConnectionLimitSpec connectionLimitSpec;

    @BeforeEach
    void setUp() {
        provider = new ConnectionLimitFeatureProvider();
        pipeline = mock(ChannelPipeline.class);
        serverSpec = mock(ServerSpec.class);
        featuresSpec = mock(FeaturesSpec.class);
        connectionLimitSpec = new ConnectionLimitSpec();

        when(serverSpec.getFeatures()).thenReturn(featuresSpec);
        when(featuresSpec.getConnectionLimit()).thenReturn(connectionLimitSpec);
        when(pipeline.addLast(any(String.class), any())).thenReturn(pipeline);
    }

    @Test
    void getName_returnsConnectionLimit() {
        assertThat(provider.getName()).isEqualTo("connectionLimit");
    }

    @Test
    void getOrder_returns10() {
        assertThat(provider.getOrder()).isEqualTo(10);
    }

    @Test
    void isEnabled_whenConnectionLimitEnabled_returnsTrue() {
        connectionLimitSpec.setEnabled(true);
        assertThat(provider.isEnabled(serverSpec)).isTrue();
    }

    @Test
    void isEnabled_whenConnectionLimitDisabled_returnsFalse() {
        connectionLimitSpec.setEnabled(false);
        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void isEnabled_whenConnectionLimitSpecNull_returnsFalse() {
        when(featuresSpec.getConnectionLimit()).thenReturn(null);
        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void configure_whenEnabled_addsConnectionLimitHandler() {
        connectionLimitSpec.setEnabled(true);
        connectionLimitSpec.setMaxConnections(10000);

        provider.configure(pipeline, serverSpec);

        verify(pipeline).addLast(eq("connectionLimitHandler"), any());
    }

    @Test
    void configure_whenDisabled_doesNotAddHandler() {
        connectionLimitSpec.setEnabled(false);

        provider.configure(pipeline, serverSpec);

        verify(pipeline, never()).addLast(any(String.class), any());
    }

    @Test
    void configure_whenConnectionLimitSpecNull_doesNotAddHandler() {
        when(featuresSpec.getConnectionLimit()).thenReturn(null);

        provider.configure(pipeline, serverSpec);

        verify(pipeline, never()).addLast(any(String.class), any());
    }
}
