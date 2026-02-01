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

import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.handler.MetricsChannelHandler;
import com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;

/**
 * Feature provider for server metrics collection.
 *
 * <p>Adds {@link MetricsChannelHandler} to the pipeline for collecting
 * server metrics such as connection counts, bytes in/out, and request statistics.
 *
 * <p>This feature should be placed very early in the pipeline (order 5) to capture
 * all bytes before any decoding/encoding happens.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class MetricsFeatureProvider implements FeatureProvider {

    /**
     * Feature name constant.
     */
    public static final String NAME = "metrics";

    /**
     * Order for this feature - very early in the pipeline to capture all bytes.
     */
    public static final int ORDER = 5;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        ServerMetrics serverMetrics = pipeline.channel().attr(NettyContext.SERVER_METRICS_KEY).get();
        if (serverMetrics != null) {
            pipeline.addLast("metricsHandler", new MetricsChannelHandler(serverMetrics));
        }
    }

    @Override
    public boolean isEnabled(ServerSpec serverSpec) {
        // Metrics are always enabled when ServerMetrics is available
        // The actual check is done in configure() by checking the channel attribute
        return true;
    }

}
