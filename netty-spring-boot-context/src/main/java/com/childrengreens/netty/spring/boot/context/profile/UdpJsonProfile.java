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

package com.childrengreens.netty.spring.boot.context.profile;

import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;

/**
 * Profile for UDP with JSON codec.
 *
 * <p>This profile configures handlers for UDP datagram processing
 * with JSON serialization.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 */
public class UdpJsonProfile implements Profile {

    /**
     * Profile name constant.
     */
    public static final String NAME = "udp-json";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        // UDP doesn't need framing - each datagram is a complete message
        // JSON codec will be added by the pipeline assembler
    }

    @Override
    public String getDefaultCodec() {
        return "json";
    }

}
