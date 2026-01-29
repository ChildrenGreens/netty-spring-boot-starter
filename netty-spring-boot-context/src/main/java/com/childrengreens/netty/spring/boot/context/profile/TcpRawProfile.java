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
 * Profile for raw TCP without framing.
 *
 * <p>This profile provides no automatic frame decoding. The user is responsible
 * for handling ByteBuf directly or configuring custom codecs.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class TcpRawProfile implements Profile {

    /**
     * Profile name constant.
     */
    public static final String NAME = "tcp-raw";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        // No framing - ByteBuf passed directly to handlers
    }

    @Override
    public String getDefaultCodec() {
        return "none";
    }

    @Override
    public boolean supportsDispatcher() {
        return false;
    }

}
