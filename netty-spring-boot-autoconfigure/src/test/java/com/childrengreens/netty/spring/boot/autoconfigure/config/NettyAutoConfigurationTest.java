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

package com.childrengreens.netty.spring.boot.autoconfigure.config;

import com.childrengreens.netty.spring.boot.context.client.ClientPipelineAssembler;
import com.childrengreens.netty.spring.boot.context.client.ClientProfileRegistry;
import com.childrengreens.netty.spring.boot.context.client.ClientProxyFactory;
import com.childrengreens.netty.spring.boot.context.client.NettyClientOrchestrator;
import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.dispatch.Dispatcher;
import com.childrengreens.netty.spring.boot.context.feature.FeatureRegistry;
import com.childrengreens.netty.spring.boot.context.pipeline.PipelineAssembler;
import com.childrengreens.netty.spring.boot.context.profile.ProfileRegistry;
import com.childrengreens.netty.spring.boot.context.properties.NettyProperties;
import com.childrengreens.netty.spring.boot.context.routing.AnnotationRegistry;
import com.childrengreens.netty.spring.boot.context.routing.Router;
import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import com.childrengreens.netty.spring.boot.context.transport.TransportFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NettyAutoConfiguration}.
 */
class NettyAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    NettyAutoConfiguration.class,
                    NettyServerAutoConfiguration.class,
                    NettyClientAutoConfiguration.class));

    @Test
    void autoConfiguration_createsNettyProperties() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(NettyProperties.class));
    }

    @Test
    void autoConfiguration_createsProfileRegistry() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ProfileRegistry.class);
            ProfileRegistry registry = context.getBean(ProfileRegistry.class);
            assertThat(registry.hasProfile("tcp-lengthfield-json")).isTrue();
            assertThat(registry.hasProfile("tcp-line")).isTrue();
            assertThat(registry.hasProfile("tcp-raw")).isTrue();
            assertThat(registry.hasProfile("http1-json")).isTrue();
            assertThat(registry.hasProfile("websocket")).isTrue();
            assertThat(registry.hasProfile("udp-json")).isTrue();
        });
    }

    @Test
    void autoConfiguration_createsFeatureRegistry() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FeatureRegistry.class);
            FeatureRegistry registry = context.getBean(FeatureRegistry.class);
            assertThat(registry.hasFeature("ssl")).isTrue();
            assertThat(registry.hasFeature("logging")).isTrue();
            assertThat(registry.hasFeature("idle")).isTrue();
            assertThat(registry.hasFeature("rateLimit")).isTrue();
            assertThat(registry.hasFeature("connectionLimit")).isTrue();
        });
    }

    @Test
    void autoConfiguration_createsCodecRegistry() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CodecRegistry.class);
            CodecRegistry registry = context.getBean(CodecRegistry.class);
            assertThat(registry.hasCodec("json")).isTrue();
        });
    }

    @Test
    void autoConfiguration_createsRouter() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(Router.class));
    }

    @Test
    void autoConfiguration_createsDispatcher() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(Dispatcher.class));
    }

    @Test
    void autoConfiguration_createsAnnotationRegistry() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(AnnotationRegistry.class));
    }

    @Test
    void autoConfiguration_createsTransportFactory() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(TransportFactory.class));
    }

    @Test
    void autoConfiguration_createsPipelineAssembler() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(PipelineAssembler.class));
    }

    @Test
    void autoConfiguration_createsNettyServerOrchestrator() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(NettyServerOrchestrator.class));
    }

    @Test
    void autoConfiguration_disabledWhenPropertySetToFalse() {
        this.contextRunner
                .withPropertyValues("spring.netty.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(NettyServerOrchestrator.class));
    }

    @Test
    void autoConfiguration_respectsCustomBeans() {
        this.contextRunner
                .withBean(Router.class, Router::new)
                .run(context -> assertThat(context).hasSingleBean(Router.class));
    }

    @Test
    void autoConfiguration_notLoadedWithoutNettyOnClasspath() {
        new ApplicationContextRunner()
                .run(context -> assertThat(context).doesNotHaveBean(NettyAutoConfiguration.class));
    }

    // ==================== Server Configuration Tests ====================

    @Test
    void serverAutoConfiguration_disabledWhenPropertySetToFalse() {
        this.contextRunner
                .withPropertyValues("spring.netty.server.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(NettyServerOrchestrator.class);
                    assertThat(context).doesNotHaveBean(ProfileRegistry.class);
                    assertThat(context).doesNotHaveBean(Router.class);
                    // Client beans should still be present
                    assertThat(context).hasSingleBean(NettyClientOrchestrator.class);
                });
    }

    // ==================== Client Configuration Tests ====================

    @Test
    void clientAutoConfiguration_createsClientProfileRegistry() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ClientProfileRegistry.class);
            ClientProfileRegistry registry = context.getBean(ClientProfileRegistry.class);
            assertThat(registry.hasProfile("tcp-lengthfield-json")).isTrue();
        });
    }

    @Test
    void clientAutoConfiguration_createsClientPipelineAssembler() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(ClientPipelineAssembler.class));
    }

    @Test
    void clientAutoConfiguration_createsNettyClientOrchestrator() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(NettyClientOrchestrator.class));
    }

    @Test
    void clientAutoConfiguration_createsClientProxyFactory() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(ClientProxyFactory.class));
    }

    @Test
    void clientAutoConfiguration_disabledWhenPropertySetToFalse() {
        this.contextRunner
                .withPropertyValues("spring.netty.client.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(NettyClientOrchestrator.class);
                    assertThat(context).doesNotHaveBean(ClientProfileRegistry.class);
                    assertThat(context).doesNotHaveBean(ClientProxyFactory.class);
                    // Server beans should still be present
                    assertThat(context).hasSingleBean(NettyServerOrchestrator.class);
                });
    }
}
