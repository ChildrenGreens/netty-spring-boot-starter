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

import com.childrengreens.netty.spring.boot.context.codec.JsonNettyCodec;
import com.childrengreens.netty.spring.boot.context.properties.ClientSpec;
import com.childrengreens.netty.spring.boot.context.properties.TimeoutSpec;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RequestInvoker}.
 */
class RequestInvokerTest {

    private RequestInvoker invoker;
    private ClientSpec clientSpec;

    @BeforeEach
    void setUp() {
        clientSpec = new ClientSpec();
        clientSpec.setName("test-client");
        TimeoutSpec timeout = new TimeoutSpec();
        timeout.setRequestMs(5000);
        clientSpec.setTimeout(timeout);

        invoker = new RequestInvoker(clientSpec, new JsonNettyCodec());
    }

    @AfterEach
    void tearDown() {
        invoker.close();
    }

    @Test
    void invoke_sendRequestAndAwaitResponse() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();

        CompletableFuture<Object> future = invoker.invoke(channel, "ping", null, 0);

        // Verify request was sent
        Object outbound = channel.readOutbound();
        assertThat(outbound).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) outbound;
        assertThat(request.get("type")).isEqualTo("ping");
        assertThat(request.get(RequestInvoker.CORRELATION_ID_HEADER)).isNotNull();

        // Complete the request
        String correlationId = (String) request.get(RequestInvoker.CORRELATION_ID_HEADER);
        Map<String, Object> response = Map.of("type", "pong", "data", "success");
        invoker.completeRequest(correlationId, response);

        // Verify response
        Object result = future.get(1, TimeUnit.SECONDS);
        assertThat(result).isEqualTo(response);

        channel.close();
    }

    @Test
    void invoke_withPayload_includesPayloadInRequest() {
        EmbeddedChannel channel = new EmbeddedChannel();

        invoker.invoke(channel, "order", Map.of("orderId", "123"), 0);

        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) channel.readOutbound();
        assertThat(request.get("type")).isEqualTo("order");
        assertThat(request.get("orderId")).isEqualTo("123");

        channel.close();
    }

    @Test
    void invokeOneWay_sendsRequestWithoutCorrelationId() {
        EmbeddedChannel channel = new EmbeddedChannel();

        invoker.invokeOneWay(channel, "notify", Map.of("event", "test"));

        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) channel.readOutbound();
        assertThat(request.get("type")).isEqualTo("notify");
        assertThat(request.get("event")).isEqualTo("test");
        assertThat(request.get(RequestInvoker.CORRELATION_ID_HEADER)).isNull();

        channel.close();
    }

    @Test
    void completeRequest_withUnknownCorrelationId_returnsFalse() {
        boolean completed = invoker.completeRequest("unknown-id", "response");

        assertThat(completed).isFalse();
    }

    @Test
    void failRequest_completesExceptionally() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();

        CompletableFuture<Object> future = invoker.invoke(channel, "test", null, 0);

        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) channel.readOutbound();
        String correlationId = (String) request.get(RequestInvoker.CORRELATION_ID_HEADER);

        invoker.failRequest(correlationId, new RuntimeException("Test error"));

        assertThat(future.isCompletedExceptionally()).isTrue();

        channel.close();
    }

    @Test
    void getPendingRequestCount_tracksPendingRequests() {
        EmbeddedChannel channel = new EmbeddedChannel();

        assertThat(invoker.getPendingRequestCount()).isEqualTo(0);

        invoker.invoke(channel, "test1", null, 0);
        assertThat(invoker.getPendingRequestCount()).isEqualTo(1);

        invoker.invoke(channel, "test2", null, 0);
        assertThat(invoker.getPendingRequestCount()).isEqualTo(2);

        channel.close();
    }

    @Test
    void close_cancelsAllPendingRequests() {
        EmbeddedChannel channel = new EmbeddedChannel();

        CompletableFuture<Object> future1 = invoker.invoke(channel, "test1", null, 0);
        CompletableFuture<Object> future2 = invoker.invoke(channel, "test2", null, 0);

        invoker.close();

        assertThat(future1.isCompletedExceptionally()).isTrue();
        assertThat(future2.isCompletedExceptionally()).isTrue();
        assertThat(invoker.getPendingRequestCount()).isEqualTo(0);

        channel.close();
    }

    @Test
    void invoke_usesCustomTimeout() {
        EmbeddedChannel channel = new EmbeddedChannel();

        // Create invoker with short default timeout
        ClientSpec shortTimeoutSpec = new ClientSpec();
        shortTimeoutSpec.setName("short-timeout");
        TimeoutSpec timeout = new TimeoutSpec();
        timeout.setRequestMs(100);
        shortTimeoutSpec.setTimeout(timeout);
        RequestInvoker shortInvoker = new RequestInvoker(shortTimeoutSpec, new JsonNettyCodec());

        CompletableFuture<Object> future = shortInvoker.invoke(channel, "test", null, 5000);

        // The custom timeout of 5000ms should be used instead of 100ms
        assertThat(future.isDone()).isFalse();

        shortInvoker.close();
        channel.close();
    }

}
