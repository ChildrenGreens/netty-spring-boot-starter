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
import com.childrengreens.netty.spring.boot.context.dispatch.ArgumentResolver;
import com.childrengreens.netty.spring.boot.context.dispatch.Dispatcher;
import com.childrengreens.netty.spring.boot.context.dispatch.PathVariableArgumentResolver;
import com.childrengreens.netty.spring.boot.context.dispatch.QueryArgumentResolver;
import com.childrengreens.netty.spring.boot.context.feature.ConnectionLimitFeatureProvider;
import com.childrengreens.netty.spring.boot.context.feature.FeatureProvider;
import com.childrengreens.netty.spring.boot.context.feature.FeatureRegistry;
import com.childrengreens.netty.spring.boot.context.feature.IdleFeatureProvider;
import com.childrengreens.netty.spring.boot.context.feature.LoggingFeatureProvider;
import com.childrengreens.netty.spring.boot.context.feature.RateLimitFeatureProvider;
import com.childrengreens.netty.spring.boot.context.feature.SslFeatureProvider;
import com.childrengreens.netty.spring.boot.context.pipeline.NettyPipelineConfigurer;
import com.childrengreens.netty.spring.boot.context.pipeline.PipelineAssembler;
import com.childrengreens.netty.spring.boot.context.profile.Http1JsonProfile;
import com.childrengreens.netty.spring.boot.context.profile.Profile;
import com.childrengreens.netty.spring.boot.context.profile.ProfileRegistry;
import com.childrengreens.netty.spring.boot.context.profile.TcpLengthFieldJsonProfile;
import com.childrengreens.netty.spring.boot.context.profile.TcpLineProfile;
import com.childrengreens.netty.spring.boot.context.profile.TcpRawProfile;
import com.childrengreens.netty.spring.boot.context.profile.UdpJsonProfile;
import com.childrengreens.netty.spring.boot.context.profile.WebSocketProfile;
import com.childrengreens.netty.spring.boot.context.properties.NettyProperties;
import com.childrengreens.netty.spring.boot.context.routing.AnnotationRegistry;
import com.childrengreens.netty.spring.boot.context.routing.Router;
import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import com.childrengreens.netty.spring.boot.context.transport.TransportFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-configuration for Netty Spring Boot Starter.
 *
 * <p>This configuration is activated when:
 * <ul>
 * <li>Netty classes are on the classpath</li>
 * <li>{@code netty.enabled} property is not {@code false}</li>
 * </ul>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
@AutoConfiguration
@ConditionalOnClass(io.netty.channel.Channel.class)
@ConditionalOnProperty(prefix = "netty", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(NettyProperties.class)
public class NettyAutoConfiguration {

    /**
     * Create the profile registry with built-in profiles.
     * @param profiles additional profiles from the application context
     * @return the profile registry
     */
    @Bean
    @ConditionalOnMissingBean
    public ProfileRegistry profileRegistry(ObjectProvider<Profile> profiles) {
        ProfileRegistry registry = new ProfileRegistry();

        // Register built-in profiles
        registry.register(new TcpLengthFieldJsonProfile());
        registry.register(new TcpLineProfile());
        registry.register(new TcpRawProfile());
        registry.register(new Http1JsonProfile());
        registry.register(new WebSocketProfile());
        registry.register(new UdpJsonProfile());

        // Register custom profiles
        profiles.forEach(registry::register);

        return registry;
    }

    /**
     * Create the feature registry with built-in features.
     * @param features additional features from the application context
     * @return the feature registry
     */
    @Bean
    @ConditionalOnMissingBean
    public FeatureRegistry featureRegistry(ObjectProvider<FeatureProvider> features) {
        FeatureRegistry registry = new FeatureRegistry();

        // Register built-in features
        registry.register(new SslFeatureProvider());
        registry.register(new LoggingFeatureProvider());
        registry.register(new IdleFeatureProvider());
        registry.register(new RateLimitFeatureProvider());
        registry.register(new ConnectionLimitFeatureProvider());

        // Register custom features
        features.forEach(registry::register);

        return registry;
    }

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
     * Create the router.
     * @return the router
     */
    @Bean
    @ConditionalOnMissingBean
    public Router router() {
        return new Router();
    }

    /**
     * Create the dispatcher.
     * @param router the router
     * @param codecRegistry the codec registry
     * @param resolvers custom argument resolvers
     * @return the dispatcher
     */
    @Bean
    @ConditionalOnMissingBean
    public Dispatcher dispatcher(Router router, CodecRegistry codecRegistry,
                                  ObjectProvider<ArgumentResolver> resolvers) {
        Dispatcher dispatcher = new Dispatcher(router, codecRegistry);

        // Add built-in resolvers
        dispatcher.getArgumentResolvers().addResolver(new PathVariableArgumentResolver());
        dispatcher.getArgumentResolvers().addResolver(new QueryArgumentResolver());

        // Add custom resolvers
        resolvers.forEach(dispatcher.getArgumentResolvers()::addResolver);

        return dispatcher;
    }

    /**
     * Create the annotation registry for scanning handler methods.
     * @param router the router
     * @return the annotation registry
     */
    @Bean
    @ConditionalOnMissingBean
    public AnnotationRegistry annotationRegistry(Router router) {
        return new AnnotationRegistry(router);
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

    /**
     * Create the pipeline assembler.
     * @param profileRegistry the profile registry
     * @param featureRegistry the feature registry
     * @param dispatcher the dispatcher
     * @param codecRegistry the codec registry
     * @param configurers custom pipeline configurers
     * @return the pipeline assembler
     */
    @Bean
    @ConditionalOnMissingBean
    public PipelineAssembler pipelineAssembler(ProfileRegistry profileRegistry,
                                                FeatureRegistry featureRegistry,
                                                Dispatcher dispatcher,
                                                CodecRegistry codecRegistry,
                                                ObjectProvider<NettyPipelineConfigurer> configurers) {
        List<NettyPipelineConfigurer> configurerList = new ArrayList<>();
        configurers.forEach(configurerList::add);
        return new PipelineAssembler(profileRegistry, featureRegistry, dispatcher,
                codecRegistry, configurerList);
    }

    /**
     * Create the server orchestrator.
     * @param properties the Netty properties
     * @param transportFactory the transport factory
     * @param pipelineAssembler the pipeline assembler
     * @return the server orchestrator
     */
    @Bean
    @ConditionalOnMissingBean
    public NettyServerOrchestrator nettyServerOrchestrator(NettyProperties properties,
                                                            TransportFactory transportFactory,
                                                            PipelineAssembler pipelineAssembler) {
        return new NettyServerOrchestrator(properties, transportFactory, pipelineAssembler);
    }

}
