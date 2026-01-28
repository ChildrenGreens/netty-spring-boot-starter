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
import com.childrengreens.netty.spring.boot.context.properties.ClientSpec;
import com.childrengreens.netty.spring.boot.context.properties.FeaturesSpec;
import com.childrengreens.netty.spring.boot.context.properties.IdleSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClientPipelineAssembler}.
 */
class ClientPipelineAssemblerTest {

    private ClientPipelineAssembler assembler;
    private ClientProfileRegistry profileRegistry;
    private CodecRegistry codecRegistry;

    @BeforeEach
    void setUp() {
        profileRegistry = new ClientProfileRegistry();
        profileRegistry.register(new TcpLengthFieldJsonClientProfile());

        codecRegistry = new CodecRegistry();
        codecRegistry.register(new JsonNettyCodec());

        assembler = new ClientPipelineAssembler(profileRegistry, codecRegistry);
    }

    @Test
    void assemble_withValidProfile_configuresPipeline() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();
        ClientSpec clientSpec = new ClientSpec();
        clientSpec.setName("test-client");
        clientSpec.setProfile("tcp-lengthfield-json");

        RequestInvoker requestInvoker = mock(RequestInvoker.class);

        assembler.assemble(pipeline, clientSpec, requestInvoker);

        assertThat(pipeline.names()).contains("frameDecoder", "frameEncoder", "codecHandler", "responseHandler");

        channel.close();
    }

    @Test
    void assemble_withUnknownProfile_throwsException() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();
        ClientSpec clientSpec = new ClientSpec();
        clientSpec.setName("test-client");
        clientSpec.setProfile("unknown-profile");

        RequestInvoker requestInvoker = mock(RequestInvoker.class);

        assertThatThrownBy(() -> assembler.assemble(pipeline, clientSpec, requestInvoker))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown client profile");

        channel.close();
    }

    @Test
    void assemble_withIdleFeature_addsIdleHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();
        ClientSpec clientSpec = new ClientSpec();
        clientSpec.setName("test-client");
        clientSpec.setProfile("tcp-lengthfield-json");

        FeaturesSpec features = new FeaturesSpec();
        IdleSpec idleSpec = new IdleSpec();
        idleSpec.setEnabled(true);
        idleSpec.setReadSeconds(30);
        features.setIdle(idleSpec);
        clientSpec.setFeatures(features);

        RequestInvoker requestInvoker = mock(RequestInvoker.class);

        assembler.assemble(pipeline, clientSpec, requestInvoker);

        assertThat(pipeline.names()).contains("idleStateHandler");

        channel.close();
    }

    @Test
    void assemble_withDisabledIdleFeature_noIdleHandler() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();
        ClientSpec clientSpec = new ClientSpec();
        clientSpec.setName("test-client");
        clientSpec.setProfile("tcp-lengthfield-json");

        FeaturesSpec features = new FeaturesSpec();
        IdleSpec idleSpec = new IdleSpec();
        idleSpec.setEnabled(false);
        features.setIdle(idleSpec);
        clientSpec.setFeatures(features);

        RequestInvoker requestInvoker = mock(RequestInvoker.class);

        assembler.assemble(pipeline, clientSpec, requestInvoker);

        assertThat(pipeline.names()).doesNotContain("idleStateHandler");

        channel.close();
    }

}
