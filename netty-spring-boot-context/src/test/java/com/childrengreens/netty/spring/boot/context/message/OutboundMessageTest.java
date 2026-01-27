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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OutboundMessage}.
 */
class OutboundMessageTest {

    @Test
    void ok_withPayload_creates200Response() {
        Object payload = Map.of("key", "value");
        OutboundMessage message = OutboundMessage.ok(payload);

        assertThat(message.getStatusCode()).isEqualTo(200);
        assertThat(message.getPayload()).isEqualTo(payload);
        assertThat(message.getHeaders()).isEmpty();
    }

    @Test
    void error_withCodeAndMessage_createsErrorResponse() {
        OutboundMessage message = OutboundMessage.error(404, "Not Found");

        assertThat(message.getStatusCode()).isEqualTo(404);
        assertThat(message.getPayload()).isNotNull();
    }

    @Test
    void builder_createsMessageWithAllFields() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        OutboundMessage message = OutboundMessage.builder()
                .statusCode(201)
                .payload("created")
                .headers(headers)
                .build();

        assertThat(message.getStatusCode()).isEqualTo(201);
        assertThat(message.getPayload()).isEqualTo("created");
        assertThat(message.getHeaders()).containsEntry("Content-Type", "application/json");
    }

    @Test
    void builder_withMinimalFields() {
        OutboundMessage message = OutboundMessage.builder()
                .statusCode(204)
                .build();

        assertThat(message.getStatusCode()).isEqualTo(204);
        assertThat(message.getPayload()).isNull();
        assertThat(message.getHeaders()).isEmpty();
    }

    @Test
    void ok_withNullPayload_creates200ResponseWithNullPayload() {
        OutboundMessage message = OutboundMessage.ok(null);

        assertThat(message.getStatusCode()).isEqualTo(200);
        assertThat(message.getPayload()).isNull();
    }

    @Test
    void constructor_withPayloadOnly_creates200Response() {
        OutboundMessage message = new OutboundMessage("payload");

        assertThat(message.getPayload()).isEqualTo("payload");
        assertThat(message.getStatusCode()).isEqualTo(200);
        assertThat(message.getHeaders()).isEmpty();
    }

    @Test
    void constructor_withHeadersAndPayload_creates200Response() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("key", "value");

        OutboundMessage message = new OutboundMessage(headers, "payload");

        assertThat(message.getPayload()).isEqualTo("payload");
        assertThat(message.getStatusCode()).isEqualTo(200);
        assertThat(message.getHeaders()).containsEntry("key", "value");
    }

    @Test
    void constructor_withAllParams_createsCustomResponse() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("key", "value");

        OutboundMessage message = new OutboundMessage(headers, "payload", 201);

        assertThat(message.getPayload()).isEqualTo("payload");
        assertThat(message.getStatusCode()).isEqualTo(201);
        assertThat(message.getHeaders()).containsEntry("key", "value");
    }

    @Test
    void constructor_withNullHeaders_createsEmptyHeadersMap() {
        OutboundMessage message = new OutboundMessage(null, "payload", 200);

        assertThat(message.getHeaders()).isEmpty();
    }

    @Test
    void status_createsMessageWithSpecifiedStatus() {
        OutboundMessage message = OutboundMessage.status(201, "created");

        assertThat(message.getPayload()).isEqualTo("created");
        assertThat(message.getStatusCode()).isEqualTo(201);
    }

    @Test
    void builder_header_addsIndividualHeader() {
        OutboundMessage message = OutboundMessage.builder()
                .header("h1", "v1")
                .header("h2", "v2")
                .payload("data")
                .build();

        assertThat(message.getHeaders()).containsEntry("h1", "v1");
        assertThat(message.getHeaders()).containsEntry("h2", "v2");
    }
}
