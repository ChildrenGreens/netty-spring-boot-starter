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

package com.childrengreens.netty.spring.boot.context.pipeline;

import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureMetrics;
import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.dispatch.Dispatcher;
import com.childrengreens.netty.spring.boot.context.feature.FeatureProvider;
import com.childrengreens.netty.spring.boot.context.feature.FeatureRegistry;
import com.childrengreens.netty.spring.boot.context.handler.DispatcherHandler;
import com.childrengreens.netty.spring.boot.context.handler.ExceptionHandler;
import com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics;
import com.childrengreens.netty.spring.boot.context.profile.Profile;
import com.childrengreens.netty.spring.boot.context.profile.ProfileRegistry;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Assembler for constructing channel pipelines.
 *
 * <p>The assembler applies profile, features, and custom configurers
 * to build the complete pipeline for a server.
 *
 * <p>Pipeline stages are applied in order:
 * <ol>
 * <li>SSL/Transport (features)</li>
 * <li>Connection governance (features)</li>
 * <li>Profile handlers (framing, codec base)</li>
 * <li>Remaining features</li>
 * <li>Business dispatcher</li>
 * <li>Exception handler</li>
 * <li>Custom configurers</li>
 * </ol>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see Profile
 * @see FeatureProvider
 * @see NettyPipelineConfigurer
 */
public class PipelineAssembler {

    private static final Logger logger = LoggerFactory.getLogger(PipelineAssembler.class);

    private final ProfileRegistry profileRegistry;
    private final FeatureRegistry featureRegistry;
    private final Dispatcher dispatcher;
    private final CodecRegistry codecRegistry;
    private final List<NettyPipelineConfigurer> configurers;

    /**
     * Create a new PipelineAssembler.
     * @param profileRegistry the profile registry
     * @param featureRegistry the feature registry
     * @param dispatcher the message dispatcher
     * @param codecRegistry the codec registry
     * @param configurers the custom pipeline configurers
     */
    public PipelineAssembler(ProfileRegistry profileRegistry, FeatureRegistry featureRegistry,
                              Dispatcher dispatcher, CodecRegistry codecRegistry,
                              List<NettyPipelineConfigurer> configurers) {
        this.profileRegistry = profileRegistry;
        this.featureRegistry = featureRegistry;
        this.dispatcher = dispatcher;
        this.codecRegistry = codecRegistry;
        this.configurers = new ArrayList<>(configurers);
        this.configurers.sort(Comparator.comparingInt(NettyPipelineConfigurer::getOrder));
    }

    /**
     * Assemble the pipeline for the specified server.
     * @param pipeline the channel pipeline
     * @param serverSpec the server specification
     */
    public void assemble(ChannelPipeline pipeline, ServerSpec serverSpec) {
        assemble(pipeline, serverSpec, null);
    }

    /**
     * Assemble the pipeline for the specified server with metrics support.
     * @param pipeline the channel pipeline
     * @param serverSpec the server specification
     * @param serverMetrics the server metrics for tracking stats (may be null)
     * @since 0.0.2
     */
    public void assemble(ChannelPipeline pipeline, ServerSpec serverSpec,
                         @Nullable ServerMetrics serverMetrics) {
        assemble(pipeline, serverSpec, serverMetrics, null);
    }

    /**
     * Assemble the pipeline for the specified server with metrics and backpressure metrics support.
     * @param pipeline the channel pipeline
     * @param serverSpec the server specification
     * @param serverMetrics the server metrics for tracking stats (may be null)
     * @param backpressureMetrics the backpressure metrics (may be null)
     * @since 0.0.2
     */
    public void assemble(ChannelPipeline pipeline, ServerSpec serverSpec,
                         @Nullable ServerMetrics serverMetrics,
                         @Nullable BackpressureMetrics backpressureMetrics) {
        String profileName = serverSpec.getProfile();
        Profile profile = profileRegistry.getRequiredProfile(profileName);

        logger.debug("Assembling pipeline for server [{}] with profile [{}]",
                serverSpec.getName(), profileName);

        // 0. Set protocol type attribute for ExceptionHandler
        String protocolType = profile.getProtocolType();
        pipeline.channel().attr(NettyContext.PROTOCOL_TYPE_KEY).set(protocolType);

        // 0.1 Set server metrics attribute for MetricsChannelHandler
        if (serverMetrics != null) {
            pipeline.channel().attr(NettyContext.SERVER_METRICS_KEY).set(serverMetrics);
        }

        // 0.2 Set backpressure metrics attribute for BackpressureHandler
        if (backpressureMetrics != null) {
            pipeline.channel().attr(NettyContext.BACKPRESSURE_METRICS_KEY).set(backpressureMetrics);
        }

        // 1. Apply features with low order numbers (SSL, connection governance)
        applyFeatures(pipeline, serverSpec, 0, 200);

        // 2. Apply profile (framing, base codec)
        profile.configure(pipeline, serverSpec);

        // 3. Apply remaining features
        applyFeatures(pipeline, serverSpec, 200, Integer.MAX_VALUE);

        // 4. Add dispatcher handler if profile supports it
        if (profile.supportsDispatcher()) {
            pipeline.addLast("dispatcherHandler",
                    new DispatcherHandler(dispatcher, serverSpec, codecRegistry, serverMetrics));
        }

        // 5. Add exception handler
        pipeline.addLast("exceptionHandler", new ExceptionHandler());

        // 6. Apply custom configurers
        for (NettyPipelineConfigurer configurer : configurers) {
            if (configurer.supports(serverSpec)) {
                configurer.configure(pipeline, serverSpec);
            }
        }

        logger.debug("Pipeline assembled with handlers: {}", pipeline.names());
    }

    /**
     * Apply features within the specified order range.
     */
    private void applyFeatures(ChannelPipeline pipeline, ServerSpec serverSpec,
                               int minOrder, int maxOrder) {
        for (FeatureProvider feature : featureRegistry.getOrderedFeatures()) {
            int order = feature.getOrder();
            if (order >= minOrder && order < maxOrder && feature.isEnabled(serverSpec)) {
                logger.debug("Applying feature [{}] (order={})", feature.getName(), order);
                feature.configure(pipeline, serverSpec);
            }
        }
    }

}
