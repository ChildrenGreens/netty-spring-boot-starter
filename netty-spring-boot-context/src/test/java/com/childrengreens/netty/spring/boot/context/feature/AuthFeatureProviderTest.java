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

import com.childrengreens.netty.spring.boot.context.auth.*;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.handler.AuthHandler;
import com.childrengreens.netty.spring.boot.context.properties.FeaturesSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link AuthFeatureProvider}.
 */
class AuthFeatureProviderTest {

    private Authenticator authenticator;
    private AuthFeatureProvider featureProvider;
    private ServerSpec serverSpec;
    private AuthSpec authSpec;

    @BeforeEach
    void setUp() {
        authenticator = mock(Authenticator.class);
        featureProvider = new AuthFeatureProvider(authenticator);
        serverSpec = new ServerSpec();
        serverSpec.setName("test-server");

        authSpec = new AuthSpec();
        authSpec.setEnabled(true);
        authSpec.setMode(AuthMode.CREDENTIAL);

        FeaturesSpec features = new FeaturesSpec();
        features.setAuth(authSpec);
        serverSpec.setFeatures(features);
    }

    @Test
    void getName_returnsAuth() {
        assertThat(featureProvider.getName()).isEqualTo("auth");
    }

    @Test
    void getOrder_returnsCorrectOrder() {
        assertThat(featureProvider.getOrder()).isEqualTo(210);
    }

    @Test
    void isEnabled_whenEnabled_returnsTrue() {
        assertThat(featureProvider.isEnabled(serverSpec)).isTrue();
    }

    @Test
    void isEnabled_whenDisabled_returnsFalse() {
        authSpec.setEnabled(false);
        assertThat(featureProvider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void isEnabled_whenSpecNull_returnsFalse() {
        serverSpec.getFeatures().setAuth(null);
        assertThat(featureProvider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void configure_addsAuthHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        featureProvider.configure(pipeline, serverSpec);

        assertThat(pipeline.get("authHandler")).isNotNull();
        assertThat(pipeline.get("authHandler")).isInstanceOf(AuthHandler.class);

        channel.close();
    }

    @Test
    void configure_withMetricsEnabled_createsMetrics() {
        authSpec.setMetrics(true);
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        featureProvider.configure(pipeline, serverSpec);

        AuthMetrics metrics = channel.attr(NettyContext.AUTH_METRICS_KEY).get();
        assertThat(metrics).isNotNull();
        assertThat(metrics.getServerName()).isEqualTo("test-server");

        channel.close();
    }

    @Test
    void configure_withMetricsDisabled_noMetrics() {
        authSpec.setMetrics(false);
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        featureProvider.configure(pipeline, serverSpec);

        AuthMetrics metrics = channel.attr(NettyContext.AUTH_METRICS_KEY).get();
        assertThat(metrics).isNull();

        channel.close();
    }

    @Test
    void configure_whenDisabled_doesNotAddHandler() {
        authSpec.setEnabled(false);
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        featureProvider.configure(pipeline, serverSpec);

        assertThat(pipeline.get("authHandler")).isNull();

        channel.close();
    }

    @Test
    void configure_whenSpecNull_doesNotAddHandler() {
        serverSpec.getFeatures().setAuth(null);
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();

        featureProvider.configure(pipeline, serverSpec);

        assertThat(pipeline.get("authHandler")).isNull();

        channel.close();
    }

    @Test
    void getConnectionManager_returnsManager() {
        assertThat(featureProvider.getConnectionManager()).isNotNull();
    }

    @Test
    void constructor_withCustomConnectionManager_usesIt() {
        ConnectionManager customManager = new ConnectionManager();
        AuthFeatureProvider provider = new AuthFeatureProvider(authenticator, customManager);

        assertThat(provider.getConnectionManager()).isSameAs(customManager);
    }
}
