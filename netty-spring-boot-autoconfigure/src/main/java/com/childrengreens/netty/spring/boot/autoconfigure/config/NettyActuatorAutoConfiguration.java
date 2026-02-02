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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Netty actuator endpoints and health indicator.
 *
 * <p>This configuration creates actuator beans when at least one of
 * {@link NettyServerOrchestrator} or {@link NettyClientOrchestrator} is available.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
@AutoConfiguration(after = {NettyServerAutoConfiguration.class, NettyClientAutoConfiguration.class})
@ConditionalOnClass({Endpoint.class, HealthIndicator.class})
public class NettyActuatorAutoConfiguration {

    /**
     * Create the Netty actuator endpoint.
     *
     * @param serverOrchestrator the server orchestrator (optional)
     * @param clientOrchestrator the client orchestrator (optional)
     * @return the netty endpoint
     */
    @Bean
    @ConditionalOnMissingBean
    public NettyEndpoint nettyEndpoint(ObjectProvider<NettyServerOrchestrator> serverOrchestrator,
                                       ObjectProvider<NettyClientOrchestrator> clientOrchestrator) {
        return new NettyEndpoint(serverOrchestrator.getIfAvailable(), clientOrchestrator.getIfAvailable());
    }

    /**
     * Create the Netty health indicator.
     *
     * @param serverOrchestrator the server orchestrator (optional)
     * @param clientOrchestrator the client orchestrator (optional)
     * @return the health indicator
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.netty.observability", name = "health",
            havingValue = "true", matchIfMissing = true)
    public NettyHealthIndicator nettyHealthIndicator(ObjectProvider<NettyServerOrchestrator> serverOrchestrator,
                                                     ObjectProvider<NettyClientOrchestrator> clientOrchestrator) {
        return new NettyHealthIndicator(serverOrchestrator.getIfAvailable(), clientOrchestrator.getIfAvailable());
    }

}
