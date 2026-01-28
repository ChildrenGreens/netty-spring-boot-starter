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

package com.childrengreens.netty.spring.boot.context.handler;

import com.childrengreens.netty.spring.boot.context.codec.JsonNettyCodec;
import com.childrengreens.netty.spring.boot.context.codec.NettyCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonCodecHandler}.
 */
class JsonCodecHandlerTest {

    private NettyCodec codec;
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        codec = new JsonNettyCodec();
        channel = new EmbeddedChannel(new JsonCodecHandler(codec));
    }

    @Test
    void channelRead_withByteBuf_decodesToMap() {
        String json = "{\"type\":\"test\",\"value\":123}";
        ByteBuf input = Unpooled.copiedBuffer(json.getBytes(StandardCharsets.UTF_8));

        channel.writeInbound(input);

        Object result = channel.readInbound();
        assertThat(result).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertThat(map.get("type")).isEqualTo("test");
        assertThat(map.get("value")).isEqualTo(123);
    }

    @Test
    void channelRead_withNonByteBuf_passesThrough() {
        String message = "plain string";

        channel.writeInbound(message);

        Object result = channel.readInbound();
        assertThat(result).isEqualTo(message);
    }

    @Test
    void write_withMap_encodesToByteBuf() {
        Map<String, Object> message = Map.of("type", "response", "data", "value");

        channel.writeOutbound(message);

        ByteBuf result = channel.readOutbound();
        assertThat(result).isNotNull();

        String json = result.toString(StandardCharsets.UTF_8);
        assertThat(json).contains("\"type\"");
        assertThat(json).contains("\"response\"");
        assertThat(json).contains("\"data\"");
        assertThat(json).contains("\"value\"");

        result.release();
    }

    @Test
    void write_withString_encodesToByteBuf() {
        String message = "test message";

        channel.writeOutbound(message);

        ByteBuf result = channel.readOutbound();
        assertThat(result).isNotNull();

        String json = result.toString(StandardCharsets.UTF_8);
        assertThat(json).isEqualTo("\"test message\"");

        result.release();
    }

    @Test
    void write_withByteBuf_passesThrough() {
        ByteBuf original = Unpooled.copiedBuffer("raw bytes".getBytes(StandardCharsets.UTF_8));

        channel.writeOutbound(original);

        ByteBuf result = channel.readOutbound();
        assertThat(result).isSameAs(original);

        result.release();
    }

    @Test
    void roundTrip_encodeAndDecode() {
        // Create a new channel with two handlers for round-trip test
        EmbeddedChannel roundTripChannel = new EmbeddedChannel(
                new JsonCodecHandler(codec),
                new JsonCodecHandler(codec)
        );

        Map<String, Object> original = Map.of("type", "test", "id", 42);

        // Write outbound (encode)
        roundTripChannel.writeOutbound(original);
        ByteBuf encoded = roundTripChannel.readOutbound();

        // Write inbound (decode)
        roundTripChannel.writeInbound(encoded);
        Object decoded = roundTripChannel.readInbound();

        assertThat(decoded).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) decoded;
        assertThat(result.get("type")).isEqualTo("test");
        assertThat(result.get("id")).isEqualTo(42);

        roundTripChannel.close();
    }

}
