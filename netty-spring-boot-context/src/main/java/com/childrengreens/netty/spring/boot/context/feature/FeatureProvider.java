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

import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;

/**
 * Strategy interface for feature providers that add handlers to the pipeline.
 *
 * <p>Features are composable handler suites that can be enabled/disabled
 * independently of the profile. Examples include idle detection, SSL/TLS,
 * logging, rate limiting, and connection limiting.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 * @see FeatureRegistry
 */
public interface FeatureProvider {

    /**
     * Return the unique name of this feature.
     * @return the feature name
     */
    String getName();

    /**
     * Return the order in which this feature should be applied.
     * <p>Lower values are applied first. Recommended ranges:
     * <ul>
     * <li>0-100: SSL/Transport level</li>
     * <li>100-200: Connection governance</li>
     * <li>200-300: Framing</li>
     * <li>300-400: Codec</li>
     * <li>400-500: Business</li>
     * <li>500+: Outbound/Metrics</li>
     * </ul>
     * @return the order value
     */
    int getOrder();

    /**
     * Configure the channel pipeline with this feature's handlers.
     * @param pipeline the channel pipeline
     * @param serverSpec the server specification
     */
    void configure(ChannelPipeline pipeline, ServerSpec serverSpec);

    /**
     * Return whether this feature is enabled for the given server spec.
     * @param serverSpec the server specification
     * @return {@code true} if the feature should be applied
     */
    boolean isEnabled(ServerSpec serverSpec);

}
