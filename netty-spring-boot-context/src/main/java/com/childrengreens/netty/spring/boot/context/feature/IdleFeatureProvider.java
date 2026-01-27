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

import com.childrengreens.netty.spring.boot.context.properties.IdleSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * Feature provider for idle connection detection.
 *
 * <p>Adds {@link IdleStateHandler} to the pipeline for detecting
 * idle connections based on read/write/all idle timeouts.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public class IdleFeatureProvider implements FeatureProvider {

    /**
     * Feature name constant.
     */
    public static final String NAME = "idle";

    /**
     * Order for this feature - after SSL but before business handlers.
     */
    public static final int ORDER = 150;

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
        IdleSpec idle = serverSpec.getFeatures().getIdle();
        if (idle != null && idle.isEnabled()) {
            pipeline.addLast("idleStateHandler", new IdleStateHandler(
                    idle.getReadSeconds(),
                    idle.getWriteSeconds(),
                    idle.getAllSeconds(),
                    TimeUnit.SECONDS
            ));
        }
    }

    @Override
    public boolean isEnabled(ServerSpec serverSpec) {
        IdleSpec idle = serverSpec.getFeatures().getIdle();
        return idle != null && idle.isEnabled();
    }

}
