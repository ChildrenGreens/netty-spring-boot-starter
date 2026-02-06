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

package com.childrengreens.netty.spring.boot.context.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link JsonNettyCodec}.
 */
class JsonNettyCodecTest {

    private JsonNettyCodec codec;

    @BeforeEach
    void setUp() {
        codec = new JsonNettyCodec();
    }

    @Test
    void getName_returnsJson() {
        assertThat(codec.getName()).isEqualTo("json");
    }

    @Test
    void getContentType_returnsApplicationJson() {
        assertThat(codec.getContentType()).isEqualTo("application/json");
    }

    @Test
    void encode_withSimpleObject_returnsJsonBytes() throws Exception {
        TestObject obj = new TestObject("test", 42);
        byte[] result = codec.encode(obj);
        String json = new String(result, StandardCharsets.UTF_8);
        assertThat(json).contains("\"name\":\"test\"");
        assertThat(json).contains("\"value\":42");
    }

    @Test
    void encode_withMap_returnsJsonBytes() throws Exception {
        Map<String, Object> map = Map.of("key", "value", "number", 123);
        byte[] result = codec.encode(map);
        String json = new String(result, StandardCharsets.UTF_8);
        assertThat(json).contains("\"key\":\"value\"");
        assertThat(json).contains("\"number\":123");
    }

    @Test
    void encode_withInstant_returnsJsonBytes() throws Exception {
        TimeObject obj = new TimeObject(Instant.parse("2025-01-01T00:00:00Z"));
        byte[] result = codec.encode(obj);
        String json = new String(result, StandardCharsets.UTF_8);
        assertThat(json).contains("\"processedAt\"");
    }

    @Test
    void encode_withNull_returnsNullBytes() throws Exception {
        byte[] result = codec.encode(null);
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("null");
    }

    @Test
    void decode_withValidJson_returnsObject() throws Exception {
        String json = "{\"name\":\"decoded\",\"value\":100}";
        TestObject result = codec.decode(json.getBytes(StandardCharsets.UTF_8), TestObject.class);
        assertThat(result.getName()).isEqualTo("decoded");
        assertThat(result.getValue()).isEqualTo(100);
    }

    @Test
    void decode_withInvalidJson_throwsCodecException() {
        byte[] invalidJson = "not valid json".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> codec.decode(invalidJson, TestObject.class))
                .isInstanceOf(CodecException.class)
                .hasMessageContaining("Failed to decode JSON");
    }

    @Test
    void decode_withUnknownProperties_ignoresUnknownFields() throws Exception {
        String json = "{\"name\":\"test\",\"value\":1,\"unknown\":\"field\"}";
        TestObject result = codec.decode(json.getBytes(StandardCharsets.UTF_8), TestObject.class);
        assertThat(result.getName()).isEqualTo("test");
        assertThat(result.getValue()).isEqualTo(1);
    }

    @Test
    void constructor_withCustomObjectMapper_usesProvidedMapper() throws Exception {
        ObjectMapper customMapper = new ObjectMapper();
        JsonNettyCodec customCodec = new JsonNettyCodec(customMapper);
        assertThat(customCodec.getObjectMapper()).isSameAs(customMapper);
    }

    @Test
    void constructor_withCustomObjectMapper_registersJavaTimeModule() throws Exception {
        ObjectMapper customMapper = new ObjectMapper();
        JsonNettyCodec customCodec = new JsonNettyCodec(customMapper);
        byte[] result = customCodec.encode(new TimeObject(Instant.parse("2025-01-01T00:00:00Z")));
        String json = new String(result, StandardCharsets.UTF_8);
        assertThat(json).contains("\"processedAt\"");
    }

    @Test
    void getObjectMapper_returnsConfiguredMapper() {
        assertThat(codec.getObjectMapper()).isNotNull();
    }

    /**
     * Test object for serialization/deserialization.
     */
    static class TestObject {
        private String name;
        private int value;

        TestObject() {
        }

        TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    /**
     * Test object with Java time type.
     */
    static class TimeObject {
        private Instant processedAt;

        TimeObject() {
        }

        TimeObject(Instant processedAt) {
            this.processedAt = processedAt;
        }

        public Instant getProcessedAt() {
            return processedAt;
        }

        public void setProcessedAt(Instant processedAt) {
            this.processedAt = processedAt;
        }
    }
}
