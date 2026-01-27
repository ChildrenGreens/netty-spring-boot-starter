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
import com.childrengreens.netty.spring.boot.context.properties.LoggingSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link LoggingFeatureProvider}.
 */
class LoggingFeatureProviderTest {

    private LoggingFeatureProvider provider;
    private ChannelPipeline pipeline;
    private ServerSpec serverSpec;
    private FeaturesSpec featuresSpec;
    private LoggingSpec loggingSpec;

    @BeforeEach
    void setUp() {
        provider = new LoggingFeatureProvider();
        pipeline = mock(ChannelPipeline.class);
        serverSpec = mock(ServerSpec.class);
        featuresSpec = mock(FeaturesSpec.class);
        loggingSpec = new LoggingSpec();

        when(serverSpec.getFeatures()).thenReturn(featuresSpec);
        when(featuresSpec.getLogging()).thenReturn(loggingSpec);
        when(pipeline.addLast(any(String.class), any())).thenReturn(pipeline);
    }

    @Test
    void getName_returnsLogging() {
        assertThat(provider.getName()).isEqualTo("logging");
    }

    @Test
    void getOrder_returns50() {
        assertThat(provider.getOrder()).isEqualTo(50);
    }

    @Test
    void isEnabled_whenLoggingEnabled_returnsTrue() {
        loggingSpec.setEnabled(true);
        assertThat(provider.isEnabled(serverSpec)).isTrue();
    }

    @Test
    void isEnabled_whenLoggingDisabled_returnsFalse() {
        loggingSpec.setEnabled(false);
        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void isEnabled_whenLoggingSpecNull_returnsFalse() {
        when(featuresSpec.getLogging()).thenReturn(null);
        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void configure_whenEnabled_addsLoggingHandler() {
        loggingSpec.setEnabled(true);
        loggingSpec.setLevel(LogLevel.DEBUG);

        provider.configure(pipeline, serverSpec);

        verify(pipeline).addLast(eq("loggingHandler"), any(LoggingHandler.class));
    }

    @Test
    void configure_whenDisabled_doesNotAddHandler() {
        loggingSpec.setEnabled(false);

        provider.configure(pipeline, serverSpec);

        verify(pipeline, never()).addLast(any(String.class), any());
    }

    @Test
    void configure_withInfoLevel_addsHandlerWithInfoLevel() {
        loggingSpec.setEnabled(true);
        loggingSpec.setLevel(LogLevel.INFO);

        provider.configure(pipeline, serverSpec);

        verify(pipeline).addLast(eq("loggingHandler"), any(LoggingHandler.class));
    }
}
