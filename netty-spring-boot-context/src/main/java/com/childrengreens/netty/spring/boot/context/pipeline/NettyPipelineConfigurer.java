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

import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;

/**
 * Callback interface for customizing the channel pipeline.
 *
 * <p>Users can implement this interface to add custom handlers to the
 * pipeline during server initialization.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 */
@FunctionalInterface
public interface NettyPipelineConfigurer {

    /**
     * Configure the channel pipeline.
     * <p>This method is called after the profile and features have been
     * applied, allowing for additional customization.
     * @param pipeline the channel pipeline to configure
     * @param serverSpec the server specification
     */
    void configure(ChannelPipeline pipeline, ServerSpec serverSpec);

    /**
     * Return whether this configurer should be applied to the given server.
     * @param serverSpec the server specification
     * @return {@code true} if this configurer should be applied
     */
    default boolean supports(ServerSpec serverSpec) {
        return true;
    }

    /**
     * Return the order in which this configurer should be applied.
     * <p>Lower values are applied first.
     * @return the order value
     */
    default int getOrder() {
        return 0;
    }

}
