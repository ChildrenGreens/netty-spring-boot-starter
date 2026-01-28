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

import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.codec.JsonNettyCodec;
import com.childrengreens.netty.spring.boot.context.dispatch.Dispatcher;
import com.childrengreens.netty.spring.boot.context.feature.FeatureRegistry;
import com.childrengreens.netty.spring.boot.context.profile.ProfileRegistry;
import com.childrengreens.netty.spring.boot.context.profile.TcpLengthFieldJsonProfile;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import com.childrengreens.netty.spring.boot.context.routing.Router;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PipelineAssembler}.
 */
class PipelineAssemblerTest {

    private ProfileRegistry profileRegistry;
    private FeatureRegistry featureRegistry;
    private Dispatcher dispatcher;
    private CodecRegistry codecRegistry;
    private PipelineAssembler assembler;

    @BeforeEach
    void setUp() {
        profileRegistry = new ProfileRegistry();
        profileRegistry.register(new TcpLengthFieldJsonProfile());

        featureRegistry = new FeatureRegistry();

        Router router = new Router();
        codecRegistry = new CodecRegistry();
        codecRegistry.register(new JsonNettyCodec());

        dispatcher = new Dispatcher(router, codecRegistry);

        assembler = new PipelineAssembler(
                profileRegistry,
                featureRegistry,
                dispatcher,
                codecRegistry,
                Collections.emptyList()
        );
    }

    @Test
    void assemble_withValidProfile_configuresPipeline() {
        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        EmbeddedChannel channel = new EmbeddedChannel();

        assembler.assemble(channel.pipeline(), serverSpec);

        assertThat(channel.pipeline().names()).contains("dispatcherHandler", "exceptionHandler");

        channel.close();
    }

    @Test
    void assemble_withConfigurers_appliesConfigurers() {
        NettyPipelineConfigurer configurer = new NettyPipelineConfigurer() {
            @Override
            public void configure(io.netty.channel.ChannelPipeline pipeline, ServerSpec serverSpec) {
                pipeline.addLast("customHandler", new io.netty.channel.ChannelInboundHandlerAdapter());
            }

            @Override
            public boolean supports(ServerSpec serverSpec) {
                return true;
            }
        };

        assembler = new PipelineAssembler(
                profileRegistry,
                featureRegistry,
                dispatcher,
                codecRegistry,
                Collections.singletonList(configurer)
        );

        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        EmbeddedChannel channel = new EmbeddedChannel();

        assembler.assemble(channel.pipeline(), serverSpec);

        assertThat(channel.pipeline().names()).contains("customHandler");

        channel.close();
    }

    @Test
    void assemble_withConfigurerThatDoesNotSupport_skipsConfigurer() {
        NettyPipelineConfigurer configurer = new NettyPipelineConfigurer() {
            @Override
            public void configure(io.netty.channel.ChannelPipeline pipeline, ServerSpec serverSpec) {
                pipeline.addLast("customHandler", new io.netty.channel.ChannelInboundHandlerAdapter());
            }

            @Override
            public boolean supports(ServerSpec serverSpec) {
                return false;
            }
        };

        assembler = new PipelineAssembler(
                profileRegistry,
                featureRegistry,
                dispatcher,
                codecRegistry,
                Collections.singletonList(configurer)
        );

        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        EmbeddedChannel channel = new EmbeddedChannel();

        assembler.assemble(channel.pipeline(), serverSpec);

        assertThat(channel.pipeline().names()).doesNotContain("customHandler");

        channel.close();
    }

}
