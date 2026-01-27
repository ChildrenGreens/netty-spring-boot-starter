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

package com.childrengreens.netty.spring.boot.context.message;

import com.childrengreens.netty.spring.boot.context.properties.TransportType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InboundMessage}.
 */
class InboundMessageTest {

    @Test
    void builder_createsMessageWithAllFields() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("key", "value");
        byte[] payload = "test".getBytes();

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/api/test")
                .headers(headers)
                .rawPayload(payload)
                .payload("testPayload")
                .build();

        assertThat(message.getTransport()).isEqualTo(TransportType.HTTP);
        assertThat(message.getRouteKey()).isEqualTo("/api/test");
        assertThat(message.getHeaders()).containsEntry("key", "value");
        assertThat(message.getRawPayload()).isEqualTo(payload);
        assertThat(message.getPayload()).isEqualTo("testPayload");
    }

    @Test
    void getHeader_withExistingKey_returnsValue() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("httpMethod", "GET");

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/test")
                .headers(headers)
                .build();

        String httpMethod = message.getHeader("httpMethod");
        assertThat(httpMethod).isEqualTo("GET");
    }

    @Test
    void getHeader_withMissingKey_returnsNull() {
        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.TCP)
                .routeKey("test")
                .build();

        Object result = message.getHeader("nonexistent");
        assertThat(result).isNull();
    }

    @Test
    void builder_withMinimalFields() {
        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.TCP)
                .routeKey("ping")
                .build();

        assertThat(message.getTransport()).isEqualTo(TransportType.TCP);
        assertThat(message.getRouteKey()).isEqualTo("ping");
        assertThat(message.getHeaders()).isEmpty();
        assertThat(message.getRawPayload()).isNull();
        assertThat(message.getPayload()).isNull();
    }
}
