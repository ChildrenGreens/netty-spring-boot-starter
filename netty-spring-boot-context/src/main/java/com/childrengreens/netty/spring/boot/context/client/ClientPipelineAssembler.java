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

import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.codec.JsonNettyCodec;
import com.childrengreens.netty.spring.boot.context.codec.NettyCodec;
import com.childrengreens.netty.spring.boot.context.handler.JsonCodecHandler;
import com.childrengreens.netty.spring.boot.context.properties.ClientSpec;
import com.childrengreens.netty.spring.boot.context.properties.IdleSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Assembler for client channel pipelines.
 *
 * <p>This class assembles the channel pipeline for client connections,
 * configuring frame codecs, message codecs, idle handlers, and response handlers.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see ClientProfile
 * @see ClientSpec
 */
public class ClientPipelineAssembler {

    private static final Logger logger = LoggerFactory.getLogger(ClientPipelineAssembler.class);

    private final ClientProfileRegistry profileRegistry;
    private final CodecRegistry codecRegistry;

    /**
     * Create a new ClientPipelineAssembler.
     * @param profileRegistry the profile registry
     * @param codecRegistry the codec registry
     */
    public ClientPipelineAssembler(ClientProfileRegistry profileRegistry, CodecRegistry codecRegistry) {
        this.profileRegistry = profileRegistry;
        this.codecRegistry = codecRegistry;
    }

    /**
     * Assemble the pipeline for a client connection.
     * @param pipeline the channel pipeline
     * @param clientSpec the client specification
     * @param requestInvoker the request invoker for handling responses
     */
    public void assemble(ChannelPipeline pipeline, ClientSpec clientSpec, RequestInvoker requestInvoker) {
        String profileName = clientSpec.getProfile();

        // Get and apply profile
        ClientProfile profile = profileRegistry.getProfile(profileName);
        if (profile == null) {
            throw new IllegalStateException("Unknown client profile: " + profileName);
        }

        // Apply profile (frame codecs)
        profile.configure(pipeline, clientSpec);

        // Add idle handler if configured
        addIdleHandler(pipeline, clientSpec);

        // Add codec handler
        addCodecHandler(pipeline, profile);

        // Add response handler
        pipeline.addLast("responseHandler", new ClientResponseHandler(requestInvoker, clientSpec.getName()));

        logger.debug("Assembled client pipeline for [{}] with profile [{}]",
                clientSpec.getName(), profileName);
    }

    /**
     * Add idle handler to the pipeline.
     * @param pipeline the pipeline
     * @param clientSpec the client specification
     */
    private void addIdleHandler(ChannelPipeline pipeline, ClientSpec clientSpec) {
        IdleSpec idleSpec = clientSpec.getFeatures().getIdle();
        if (idleSpec != null && idleSpec.isEnabled()) {
            pipeline.addLast("idleStateHandler", new IdleStateHandler(
                    idleSpec.getReadSeconds(),
                    idleSpec.getWriteSeconds(),
                    idleSpec.getAllSeconds(),
                    TimeUnit.SECONDS
            ));
        }
    }

    /**
     * Add codec handler to the pipeline.
     * @param pipeline the pipeline
     * @param profile the client profile
     */
    private void addCodecHandler(ChannelPipeline pipeline, ClientProfile profile) {
        String codecName = profile.getDefaultCodec();
        NettyCodec codec = codecRegistry.getCodec(codecName);

        if (codec == null) {
            // Fall back to JSON codec
            codec = new JsonNettyCodec();
        }

        pipeline.addLast("codecHandler", new JsonCodecHandler(codec));
    }

}
