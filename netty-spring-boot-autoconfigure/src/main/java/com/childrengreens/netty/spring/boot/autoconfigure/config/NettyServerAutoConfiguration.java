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

import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.dispatch.ArgumentResolver;
import com.childrengreens.netty.spring.boot.context.dispatch.Dispatcher;
import com.childrengreens.netty.spring.boot.context.dispatch.PathVariableArgumentResolver;
import com.childrengreens.netty.spring.boot.context.dispatch.QueryArgumentResolver;
import com.childrengreens.netty.spring.boot.context.feature.ConnectionLimitFeatureProvider;
import com.childrengreens.netty.spring.boot.context.feature.FeatureProvider;
import com.childrengreens.netty.spring.boot.context.feature.FeatureRegistry;
import com.childrengreens.netty.spring.boot.context.feature.IdleFeatureProvider;
import com.childrengreens.netty.spring.boot.context.feature.LoggingFeatureProvider;
import com.childrengreens.netty.spring.boot.context.feature.MetricsFeatureProvider;
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-configuration for Netty server components.
 *
 * <p>This configuration provides beans for:
 * <ul>
 * <li>Server profiles and feature registry</li>
 * <li>Router and dispatcher for request handling</li>
 * <li>Pipeline assembler for server channel initialization</li>
 * <li>Server orchestrator for lifecycle management</li>
 * </ul>
 *
 * <p>This configuration is activated when {@code spring.netty.server.enabled}
 * is not explicitly set to {@code false}.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see NettyClientAutoConfiguration
 */
@AutoConfiguration(after = NettyAutoConfiguration.class)
@ConditionalOnBean(CodecRegistry.class)
@ConditionalOnProperty(prefix = "spring.netty.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NettyServerAutoConfiguration {

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
        registry.register(new MetricsFeatureProvider());
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
     * Create the router.
     *
     * <p>This method is static to avoid early instantiation of the configuration class
     * when {@link AnnotationRegistry} (a BeanPostProcessor) obtains the Router via
     * {@link org.springframework.beans.factory.BeanFactory#getBean(Class)}.
     *
     * <p>The {@link Role} annotation marks this bean as infrastructure to suppress
     * the warning about it being created early.
     *
     * @return the router
     */
    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static Router router() {
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
     *
     * <p>This method is static because {@link AnnotationRegistry} implements
     * {@link org.springframework.beans.factory.config.BeanPostProcessor}, which must be
     * instantiated early in the container lifecycle. Declaring this as static avoids
     * premature instantiation of the configuration class.
     *
     * <p>The {@link Router} dependency is obtained via {@link org.springframework.beans.factory.BeanFactoryAware}
     * and {@link org.springframework.beans.factory.InitializingBean} in the AnnotationRegistry itself.
     *
     * @return the annotation registry
     * @see <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Bean.html">
     *      Spring @Bean documentation</a>
     */
    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static AnnotationRegistry annotationRegistry() {
        return new AnnotationRegistry();
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
