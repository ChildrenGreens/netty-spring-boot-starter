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

import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;

/**
 * Strategy interface for protocol stack profiles.
 *
 * <p>A profile defines the default pipeline configuration for a specific
 * protocol or use case. Profiles encapsulate the handler chain setup
 * including frame decoders, codecs, and business handlers.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see ProfileRegistry
 */
public interface Profile {

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
     * @param serverSpec the server specification
     */
    void configure(ChannelPipeline pipeline, ServerSpec serverSpec);

    /**
     * Return the default codec type for this profile.
     * @return the codec type name (e.g., "json", "protobuf")
     */
    default String getDefaultCodec() {
        return "json";
    }

    /**
     * Return whether this profile supports the dispatcher handler.
     * @return {@code true} if dispatcher should be added
     */
    default boolean supportsDispatcher() {
        return true;
    }

    /**
     * Return the protocol type for this profile.
     * <p>Used by {@link com.childrengreens.netty.spring.boot.context.handler.ExceptionHandler}
     * to determine the appropriate error response format.
     * @return the protocol type constant from {@link NettyContext}
     * @see NettyContext#PROTOCOL_HTTP
     * @see NettyContext#PROTOCOL_WEBSOCKET
     * @see NettyContext#PROTOCOL_TCP
     * @see NettyContext#PROTOCOL_UDP
     */
    default String getProtocolType() {
        return NettyContext.PROTOCOL_TCP;
    }

}
