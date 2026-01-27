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
import com.childrengreens.netty.spring.boot.context.properties.IdleSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link IdleFeatureProvider}.
 */
class IdleFeatureProviderTest {

    private IdleFeatureProvider provider;
    private ChannelPipeline pipeline;
    private ServerSpec serverSpec;
    private FeaturesSpec featuresSpec;
    private IdleSpec idleSpec;

    @BeforeEach
    void setUp() {
        provider = new IdleFeatureProvider();
        pipeline = mock(ChannelPipeline.class);
        serverSpec = mock(ServerSpec.class);
        featuresSpec = mock(FeaturesSpec.class);
        idleSpec = new IdleSpec();

        when(serverSpec.getFeatures()).thenReturn(featuresSpec);
        when(featuresSpec.getIdle()).thenReturn(idleSpec);
        when(pipeline.addLast(any(String.class), any())).thenReturn(pipeline);
    }

    @Test
    void getName_returnsIdle() {
        assertThat(provider.getName()).isEqualTo("idle");
    }

    @Test
    void getOrder_returns150() {
        assertThat(provider.getOrder()).isEqualTo(150);
    }

    @Test
    void isEnabled_whenIdleEnabled_returnsTrue() {
        idleSpec.setEnabled(true);
        assertThat(provider.isEnabled(serverSpec)).isTrue();
    }

    @Test
    void isEnabled_whenIdleDisabled_returnsFalse() {
        idleSpec.setEnabled(false);
        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void isEnabled_whenIdleSpecNull_returnsFalse() {
        when(featuresSpec.getIdle()).thenReturn(null);
        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void configure_whenEnabled_addsIdleStateHandler() {
        idleSpec.setEnabled(true);
        idleSpec.setReadSeconds(30);
        idleSpec.setWriteSeconds(20);
        idleSpec.setAllSeconds(60);

        provider.configure(pipeline, serverSpec);

        ArgumentCaptor<IdleStateHandler> captor = ArgumentCaptor.forClass(IdleStateHandler.class);
        verify(pipeline).addLast(eq("idleStateHandler"), captor.capture());

        IdleStateHandler handler = captor.getValue();
        assertThat(handler).isNotNull();
    }

    @Test
    void configure_whenDisabled_doesNotAddHandler() {
        idleSpec.setEnabled(false);

        provider.configure(pipeline, serverSpec);

        verify(pipeline, never()).addLast(any(String.class), any());
    }

    @Test
    void configure_whenIdleSpecNull_doesNotAddHandler() {
        when(featuresSpec.getIdle()).thenReturn(null);

        provider.configure(pipeline, serverSpec);

        verify(pipeline, never()).addLast(any(String.class), any());
    }
}
