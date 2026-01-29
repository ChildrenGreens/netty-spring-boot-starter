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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.codec.JsonNettyCodec;
import com.childrengreens.netty.spring.boot.context.properties.NettyProperties;
import com.childrengreens.netty.spring.boot.context.transport.TransportFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Netty Spring Boot Starter.
 *
 * <p>This configuration provides shared beans used by both server and client:
 * <ul>
 * <li>Codec registry for message serialization</li>
 * <li>Transport factory for creating event loop groups</li>
 * </ul>
 *
 * <p>This configuration is activated when:
 * <ul>
 * <li>Netty classes are on the classpath</li>
 * <li>{@code spring.netty.enabled} property is not {@code false}</li>
 * </ul>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see NettyServerAutoConfiguration
 * @see NettyClientAutoConfiguration
 */
@AutoConfiguration
@ConditionalOnClass(io.netty.channel.Channel.class)
@ConditionalOnProperty(prefix = "spring.netty", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(NettyProperties.class)
public class NettyAutoConfiguration {

    /**
     * Create the codec registry with built-in codecs.
     * @param objectMapper the Jackson ObjectMapper
     * @return the codec registry
     */
    @Bean
    @ConditionalOnMissingBean
    public CodecRegistry codecRegistry(ObjectProvider<ObjectMapper> objectMapper) {
        CodecRegistry registry = new CodecRegistry();

        // Register JSON codec
        ObjectMapper mapper = objectMapper.getIfAvailable(ObjectMapper::new);
        registry.register(new JsonNettyCodec(mapper));

        return registry;
    }

    /**
     * Create the transport factory.
     * @param properties the Netty properties
     * @return the transport factory
     */
    @Bean
    @ConditionalOnMissingBean
    public TransportFactory transportFactory(NettyProperties properties) {
        return new TransportFactory(properties.getDefaults().getTransport().getPrefer());
    }

}
