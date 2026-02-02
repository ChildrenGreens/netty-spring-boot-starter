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

import com.childrengreens.netty.spring.boot.actuator.endpoint.NettyEndpoint;
import com.childrengreens.netty.spring.boot.actuator.health.NettyHealthIndicator;
import com.childrengreens.netty.spring.boot.context.client.NettyClientOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NettyActuatorAutoConfiguration}.
 */
class NettyActuatorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    NettyAutoConfiguration.class,
                    NettyServerAutoConfiguration.class,
                    NettyClientAutoConfiguration.class,
                    NettyActuatorAutoConfiguration.class));

    @Test
    void actuatorAutoConfiguration_createsNettyEndpoint_withServerAndClient() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(NettyServerOrchestrator.class);
            assertThat(context).hasSingleBean(NettyClientOrchestrator.class);
            assertThat(context).hasSingleBean(NettyEndpoint.class);
        });
    }

    @Test
    void actuatorAutoConfiguration_createsNettyHealthIndicator_withServerAndClient() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(NettyServerOrchestrator.class);
            assertThat(context).hasSingleBean(NettyClientOrchestrator.class);
            assertThat(context).hasSingleBean(NettyHealthIndicator.class);
        });
    }

    @Test
    void actuatorAutoConfiguration_createsNettyEndpoint_withServerOnly() {
        this.contextRunner
                .withPropertyValues("spring.netty.client.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(NettyServerOrchestrator.class);
                    assertThat(context).doesNotHaveBean(NettyClientOrchestrator.class);
                    assertThat(context).hasSingleBean(NettyEndpoint.class);
                });
    }

    @Test
    void actuatorAutoConfiguration_createsNettyHealthIndicator_withServerOnly() {
        this.contextRunner
                .withPropertyValues("spring.netty.client.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(NettyServerOrchestrator.class);
                    assertThat(context).doesNotHaveBean(NettyClientOrchestrator.class);
                    assertThat(context).hasSingleBean(NettyHealthIndicator.class);
                });
    }

    @Test
    void actuatorAutoConfiguration_createsNettyEndpoint_withClientOnly() {
        this.contextRunner
                .withPropertyValues("spring.netty.server.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(NettyServerOrchestrator.class);
                    assertThat(context).hasSingleBean(NettyClientOrchestrator.class);
                    assertThat(context).hasSingleBean(NettyEndpoint.class);
                });
    }

    @Test
    void actuatorAutoConfiguration_createsNettyHealthIndicator_withClientOnly() {
        this.contextRunner
                .withPropertyValues("spring.netty.server.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(NettyServerOrchestrator.class);
                    assertThat(context).hasSingleBean(NettyClientOrchestrator.class);
                    assertThat(context).hasSingleBean(NettyHealthIndicator.class);
                });
    }

    @Test
    void actuatorAutoConfiguration_createsNettyEndpoint_withNoOrchestrators() {
        this.contextRunner
                .withPropertyValues(
                        "spring.netty.server.enabled=false",
                        "spring.netty.client.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(NettyServerOrchestrator.class);
                    assertThat(context).doesNotHaveBean(NettyClientOrchestrator.class);
                    // Endpoint is still created, but will return empty data
                    assertThat(context).hasSingleBean(NettyEndpoint.class);
                });
    }

    @Test
    void actuatorAutoConfiguration_healthIndicatorDisabled_whenPropertySetToFalse() {
        this.contextRunner
                .withPropertyValues("spring.netty.observability.health=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(NettyEndpoint.class);
                    assertThat(context).doesNotHaveBean(NettyHealthIndicator.class);
                });
    }

    @Test
    void actuatorAutoConfiguration_healthIndicatorEnabled_byDefault() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(NettyHealthIndicator.class));
    }

    @Test
    void actuatorAutoConfiguration_respectsCustomEndpointBean() {
        this.contextRunner
                .withBean("customNettyEndpoint", NettyEndpoint.class, () -> {
                    NettyServerOrchestrator serverOrchestrator = null;
                    NettyClientOrchestrator clientOrchestrator = null;
                    return new NettyEndpoint(serverOrchestrator, clientOrchestrator);
                })
                .run(context -> assertThat(context).hasSingleBean(NettyEndpoint.class));
    }

    @Test
    void actuatorAutoConfiguration_respectsCustomHealthIndicatorBean() {
        this.contextRunner
                .withBean("customNettyHealthIndicator", NettyHealthIndicator.class, () -> {
                    NettyServerOrchestrator serverOrchestrator = null;
                    NettyClientOrchestrator clientOrchestrator = null;
                    return new NettyHealthIndicator(serverOrchestrator, clientOrchestrator);
                })
                .run(context -> assertThat(context).hasSingleBean(NettyHealthIndicator.class));
    }
}
