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

import com.childrengreens.netty.spring.boot.context.properties.LoggingSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LoggingHandler;

/**
 * Feature provider for channel logging.
 *
 * <p>Adds {@link LoggingHandler} to the pipeline for debugging
 * and monitoring channel activity.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class LoggingFeatureProvider implements FeatureProvider {

    /**
     * Feature name constant.
     */
    public static final String NAME = "logging";

    /**
     * Order for this feature - early in the pipeline.
     */
    public static final int ORDER = 50;

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
        LoggingSpec logging = serverSpec.getFeatures().getLogging();
        if (logging != null && logging.isEnabled()) {
            pipeline.addLast("loggingHandler", new LoggingHandler(logging.getLevel()));
        }
    }

    @Override
    public boolean isEnabled(ServerSpec serverSpec) {
        LoggingSpec logging = serverSpec.getFeatures().getLogging();
        return logging != null && logging.isEnabled();
    }

}
