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
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TcpLengthFieldJsonClientProfile}.
 */
class TcpLengthFieldJsonClientProfileTest {

    @Test
    void getName_returnsTcpLengthfieldJson() {
        TcpLengthFieldJsonClientProfile profile = new TcpLengthFieldJsonClientProfile();

        assertThat(profile.getName()).isEqualTo("tcp-lengthfield-json");
    }

    @Test
    void getDefaultCodec_returnsJson() {
        TcpLengthFieldJsonClientProfile profile = new TcpLengthFieldJsonClientProfile();

        assertThat(profile.getDefaultCodec()).isEqualTo("json");
    }

    @Test
    void configure_addsFrameDecoderAndEncoder() {
        TcpLengthFieldJsonClientProfile profile = new TcpLengthFieldJsonClientProfile();
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();
        ClientSpec clientSpec = new ClientSpec();

        profile.configure(pipeline, clientSpec);

        assertThat(pipeline.get("frameDecoder")).isInstanceOf(LengthFieldBasedFrameDecoder.class);
        assertThat(pipeline.get("frameEncoder")).isInstanceOf(LengthFieldPrepender.class);

        channel.close();
    }

    @Test
    void configure_handlersInCorrectOrder() {
        TcpLengthFieldJsonClientProfile profile = new TcpLengthFieldJsonClientProfile();
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelPipeline pipeline = channel.pipeline();
        ClientSpec clientSpec = new ClientSpec();

        profile.configure(pipeline, clientSpec);

        // frameDecoder should come before frameEncoder
        assertThat(pipeline.names()).containsSequence("frameDecoder", "frameEncoder");

        channel.close();
    }

}
