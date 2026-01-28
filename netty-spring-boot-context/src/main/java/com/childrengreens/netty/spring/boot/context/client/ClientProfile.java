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

package com.childrengreens.netty.spring.boot.context.client;

import com.childrengreens.netty.spring.boot.context.properties.ClientSpec;
import io.netty.channel.ChannelPipeline;

/**
 * Strategy interface for client protocol stack profiles.
 *
 * <p>A client profile defines the default pipeline configuration for a specific
 * protocol or use case on the client side.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see ClientProfileRegistry
 */
public interface ClientProfile {

    /**
     * Return the unique name of this profile.
     * @return the profile name
     */
    String getName();

    /**
     * Configure the channel pipeline according to this profile.
     * <p>This method is called during channel initialization to set up
     * the default handler chain.
     * @param pipeline the channel pipeline to configure
     * @param clientSpec the client specification
     */
    void configure(ChannelPipeline pipeline, ClientSpec clientSpec);

    /**
     * Return the default codec type for this profile.
     * @return the codec type name (e.g., "json", "protobuf")
     */
    default String getDefaultCodec() {
        return "json";
    }

}
