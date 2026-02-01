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

import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureMetrics;
import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureSpec;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.handler.BackpressureHandler;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.WriteBufferWaterMark;

/**
 * Feature provider for backpressure handling.
 *
 * <p>Adds {@link BackpressureHandler} to the pipeline for controlling
 * the flow of incoming data when the server is overwhelmed.
 *
 * <p>This feature should be placed early in the pipeline (after SSL and metrics)
 * to effectively control the data flow.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class BackpressureFeatureProvider implements FeatureProvider {

    /**
     * Feature name constant.
     */
    public static final String NAME = "backpressure";

    /**
     * Order for this feature - after metrics and SSL.
     */
    public static final int ORDER = 15;

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
        BackpressureSpec spec = serverSpec.getFeatures().getBackpressure();
        if (spec == null || !spec.isEnabled()) {
            return;
        }

        // Configure write buffer water mark
        WriteBufferWaterMark waterMark = new WriteBufferWaterMark(
                spec.getLowWaterMark(),
                spec.getHighWaterMark()
        );
        pipeline.channel().config().setWriteBufferWaterMark(waterMark);

        // Get or create backpressure metrics
        BackpressureMetrics metrics = null;
        if (spec.isMetrics()) {
            metrics = pipeline.channel().attr(NettyContext.BACKPRESSURE_METRICS_KEY).get();
            if (metrics == null) {
                metrics = new BackpressureMetrics(serverSpec.getName());
                pipeline.channel().attr(NettyContext.BACKPRESSURE_METRICS_KEY).set(metrics);
            }
        }

        // Add handler
        pipeline.addLast("backpressureHandler", new BackpressureHandler(spec, metrics));
    }

    @Override
    public boolean isEnabled(ServerSpec serverSpec) {
        BackpressureSpec spec = serverSpec.getFeatures().getBackpressure();
        return spec != null && spec.isEnabled();
    }

}
