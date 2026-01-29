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
import com.childrengreens.netty.spring.boot.context.client.ClientProfile;
import com.childrengreens.netty.spring.boot.context.client.ClientProfileRegistry;
import com.childrengreens.netty.spring.boot.context.client.ClientProxyFactory;
import com.childrengreens.netty.spring.boot.context.client.NettyClientOrchestrator;
import com.childrengreens.netty.spring.boot.context.client.TcpLengthFieldJsonClientProfile;
import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.properties.NettyProperties;
import com.childrengreens.netty.spring.boot.context.transport.TransportFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Netty client components.
 *
 * <p>This configuration provides beans for:
 * <ul>
 * <li>Client profile registry for protocol handling</li>
 * <li>Client pipeline assembler for channel initialization</li>
 * <li>Client orchestrator for connection lifecycle management</li>
 * <li>Client proxy factory for creating client proxies</li>
 * </ul>
 *
 * <p>This configuration is activated when {@code spring.netty.client.enabled}
 * is not explicitly set to {@code false}.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see NettyServerAutoConfiguration
 */
@AutoConfiguration(after = NettyAutoConfiguration.class)
@ConditionalOnBean(CodecRegistry.class)
@ConditionalOnProperty(prefix = "spring.netty.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NettyClientAutoConfiguration {

    /**
     * Create the client profile registry with built-in profiles.
     * @param profiles additional client profiles from the application context
     * @return the client profile registry
     */
    @Bean
    @ConditionalOnMissingBean
    public ClientProfileRegistry clientProfileRegistry(ObjectProvider<ClientProfile> profiles) {
        ClientProfileRegistry registry = new ClientProfileRegistry();

        // Register built-in client profiles
        registry.register(new TcpLengthFieldJsonClientProfile());

        // Register custom profiles
        profiles.forEach(registry::register);

        return registry;
    }

    /**
     * Create the client pipeline assembler.
     * @param clientProfileRegistry the client profile registry
     * @param codecRegistry the codec registry
     * @return the client pipeline assembler
     */
    @Bean
    @ConditionalOnMissingBean
    public ClientPipelineAssembler clientPipelineAssembler(ClientProfileRegistry clientProfileRegistry,
                                                            CodecRegistry codecRegistry) {
        return new ClientPipelineAssembler(clientProfileRegistry, codecRegistry);
    }

    /**
     * Create the client orchestrator.
     * @param properties the Netty properties
     * @param transportFactory the transport factory
     * @param clientPipelineAssembler the client pipeline assembler
     * @param codecRegistry the codec registry
     * @return the client orchestrator
     */
    @Bean
    @ConditionalOnMissingBean
    public NettyClientOrchestrator nettyClientOrchestrator(NettyProperties properties,
                                                            TransportFactory transportFactory,
                                                            ClientPipelineAssembler clientPipelineAssembler,
                                                            CodecRegistry codecRegistry) {
        return new NettyClientOrchestrator(properties, transportFactory, clientPipelineAssembler, codecRegistry);
    }

    /**
     * Create the client proxy factory.
     * @param orchestrator the client orchestrator
     * @return the client proxy factory
     */
    @Bean
    @ConditionalOnMissingBean
    public ClientProxyFactory clientProxyFactory(NettyClientOrchestrator orchestrator) {
        return new ClientProxyFactory(orchestrator);
    }

}
