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
import com.childrengreens.netty.spring.boot.context.metrics.ClientMetrics;
import com.childrengreens.netty.spring.boot.context.properties.ClientSpec;
import com.childrengreens.netty.spring.boot.context.properties.TimeoutSpec;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.GenericFutureListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link RequestInvoker}.
 */
class RequestInvokerTest {

    private RequestInvoker invoker;
    private ClientSpec clientSpec;
    private ClientMetrics clientMetrics;

    @BeforeEach
    void setUp() {
        clientSpec = new ClientSpec();
        clientSpec.setName("test-client");
        TimeoutSpec timeout = new TimeoutSpec();
        timeout.setRequestMs(5000);
        clientSpec.setTimeout(timeout);

        invoker = new RequestInvoker(clientSpec, new JsonNettyCodec());
        clientMetrics = new ClientMetrics("test-client");
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

        Map<String, Object> request = channel.readOutbound();
        assertThat(request.get("type")).isEqualTo("order");
        assertThat(request.get("orderId")).isEqualTo("123");

        channel.close();
    }

    @Test
    void invokeOneWay_sendsRequestWithoutCorrelationId() {
        EmbeddedChannel channel = new EmbeddedChannel();

        invoker.invokeOneWay(channel, "notify", Map.of("event", "test"));

        Map<String, Object> request = channel.readOutbound();
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

        Map<String, Object> request = channel.readOutbound();
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

    @Test
    void invoke_reservedFieldsNotOverwrittenByPayload() {
        // This test verifies the fix for reserved header fields being overwritten
        // When payload contains 'type' or 'X-Correlation-Id', they should be ignored
        EmbeddedChannel channel = new EmbeddedChannel();

        Map<String, Object> maliciousPayload = new HashMap<>();
        maliciousPayload.put("type", "malicious-type");  // Try to override type
        maliciousPayload.put(RequestInvoker.CORRELATION_ID_HEADER, "fake-correlation-id");  // Try to override correlation id
        maliciousPayload.put("data", "legitimate-data");

        invoker.invoke(channel, "actual-type", maliciousPayload, 0);

        Map<String, Object> request = channel.readOutbound();

        // Before fix: type would be "malicious-type" and correlationId would be "fake-correlation-id"
        // After fix: reserved fields should retain their correct values
        assertThat(request.get("type")).isEqualTo("actual-type");
        assertThat(request.get(RequestInvoker.CORRELATION_ID_HEADER)).isNotEqualTo("fake-correlation-id");
        assertThat(request.get(RequestInvoker.CORRELATION_ID_HEADER)).isNotNull();
        // Payload data should still be included
        assertThat(request.get("data")).isEqualTo("legitimate-data");

        channel.close();
    }

    @Test
    void invokeOneWay_reservedFieldsNotOverwrittenByPayload() {
        EmbeddedChannel channel = new EmbeddedChannel();

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "wrong-type");  // Try to override type
        payload.put(RequestInvoker.CORRELATION_ID_HEADER, "fake-correlation-id");
        payload.put("value", "test-value");

        invoker.invokeOneWay(channel, "correct-type", payload);

        Map<String, Object> request = channel.readOutbound();

        assertThat(request.get("type")).isEqualTo("correct-type");
        assertThat(request.get("value")).isEqualTo("test-value");
        // One-way requests don't have correlation id
        assertThat(request.get(RequestInvoker.CORRELATION_ID_HEADER)).isNull();

        channel.close();
    }

    @Test
    void invoke_whenMessageTypeNull_doesNotAllowPayloadTypeInjection() {
        EmbeddedChannel channel = new EmbeddedChannel();

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "injected-type");
        payload.put("data", "payload-data");

        invoker.invoke(channel, null, payload, 0);

        Map<String, Object> request = channel.readOutbound();

        // Reserved fields should be filtered from payload even when messageType is null
        assertThat(request.get("type")).isNull();
        assertThat(request.get("data")).isEqualTo("payload-data");

        channel.close();
    }

    @Test
    void invoke_payloadDataPreservedWhenNotConflicting() {
        EmbeddedChannel channel = new EmbeddedChannel();

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", "12345");
        payload.put("amount", 100);
        payload.put("currency", "USD");

        invoker.invoke(channel, "createOrder", payload, 0);

        Map<String, Object> request = channel.readOutbound();

        // All payload fields should be preserved
        assertThat(request.get("orderId")).isEqualTo("12345");
        assertThat(request.get("amount")).isEqualTo(100);
        assertThat(request.get("currency")).isEqualTo("USD");
        // Reserved fields should be set correctly
        assertThat(request.get("type")).isEqualTo("createOrder");
        assertThat(request.get(RequestInvoker.CORRELATION_ID_HEADER)).isNotNull();

        channel.close();
    }

    // ==================== Tests for writeAndFlush failure (lines 112-114) ====================

    @Test
    void invoke_writeAndFlushFails_completesExceptionally() throws Exception {
        // Create a mock channel where writeAndFlush fails
        Channel channel = mock(Channel.class);
        ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(channel.writeAndFlush(any())).thenReturn(channelFuture);

        // Capture the listener and immediately invoke it with failure
        doAnswer(invocation -> {
            GenericFutureListener listener = invocation.getArgument(0);
            when(channelFuture.isSuccess()).thenReturn(false);
            when(channelFuture.cause()).thenReturn(new RuntimeException("Write failed"));
            listener.operationComplete(channelFuture);
            return channelFuture;
        }).when(channelFuture).addListener(any());

        CompletableFuture<Object> future = invoker.invoke(channel, "test", null, 0);

        assertThat(future.isCompletedExceptionally()).isTrue();
        assertThatThrownBy(() -> future.get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Write failed");
    }

    // ==================== Tests for exception in invoke try block (lines 120-122) ====================

    @Test
    void invoke_channelThrowsException_completesExceptionally() throws Exception {
        Channel channel = mock(Channel.class);
        when(channel.writeAndFlush(any())).thenThrow(new RuntimeException("Channel error"));

        CompletableFuture<Object> future = invoker.invoke(channel, "test", null, 0);

        assertThat(future.isCompletedExceptionally()).isTrue();
        assertThatThrownBy(() -> future.get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Channel error");
    }

    // ==================== Tests for clientMetrics recording (lines 127-129) ====================

    @Test
    void invoke_withClientMetrics_recordsLatencyOnCompletion() throws Exception {
        invoker.setClientMetrics(clientMetrics);

        EmbeddedChannel channel = new EmbeddedChannel();
        CompletableFuture<Object> future = invoker.invoke(channel, "ping", null, 0);

        // Get the correlation ID from the outbound request
        Map<String, Object> request = channel.readOutbound();
        String correlationId = (String) request.get(RequestInvoker.CORRELATION_ID_HEADER);

        // Complete the request
        invoker.completeRequest(correlationId, Map.of("result", "pong"));

        // Wait for completion
        future.get(1, TimeUnit.SECONDS);

        // Verify metrics were recorded
        assertThat(clientMetrics.getRequestsTotal()).isEqualTo(1);
        assertThat(clientMetrics.getRequestLatencyTotalNanos()).isGreaterThan(0);

        channel.close();
    }

    // ==================== Tests for invokeOneWay with metrics and exception (lines 146-150) ====================

    @Test
    void invokeOneWay_withClientMetrics_incrementsRequestCount() {
        invoker.setClientMetrics(clientMetrics);

        EmbeddedChannel channel = new EmbeddedChannel();
        invoker.invokeOneWay(channel, "notify", Map.of("event", "test"));

        // Verify metrics were recorded (no latency, just count)
        assertThat(clientMetrics.getRequestsTotal()).isEqualTo(1);
        assertThat(clientMetrics.getRequestLatencyTotalNanos()).isEqualTo(0);

        channel.close();
    }

    @Test
    void invokeOneWay_throwsException_logsErrorAndContinues() {
        // Mock channel that throws on writeAndFlush
        Channel channel = mock(Channel.class);
        when(channel.writeAndFlush(any())).thenThrow(new RuntimeException("One-way write failed"));

        // Should not throw - exception is caught and logged
        invoker.invokeOneWay(channel, "notify", Map.of("event", "test"));

        // Verify writeAndFlush was called
        verify(channel).writeAndFlush(any());
    }

    // ==================== Tests for payload as String and complex object (lines 172-176) ====================

    @Test
    void invoke_withStringPayload_wrapsInDataField() {
        EmbeddedChannel channel = new EmbeddedChannel();

        invoker.invoke(channel, "message", "Hello, World!", 0);

        Map<String, Object> request = channel.readOutbound();
        assertThat(request.get("type")).isEqualTo("message");
        assertThat(request.get("data")).isEqualTo("Hello, World!");
        assertThat(request.get(RequestInvoker.CORRELATION_ID_HEADER)).isNotNull();

        channel.close();
    }

    @Test
    void invoke_withComplexObjectPayload_wrapsInDataField() {
        EmbeddedChannel channel = new EmbeddedChannel();

        // Use a complex object (not Map, not String)
        record Order(String id, int amount) {}
        Order order = new Order("ORD-123", 100);

        invoker.invoke(channel, "createOrder", order, 0);

        Map<String, Object> request = channel.readOutbound();
        assertThat(request.get("type")).isEqualTo("createOrder");
        assertThat(request.get("data")).isEqualTo(order);
        assertThat(request.get(RequestInvoker.CORRELATION_ID_HEADER)).isNotNull();

        channel.close();
    }

    @Test
    void invokeOneWay_withStringPayload_wrapsInDataField() {
        EmbeddedChannel channel = new EmbeddedChannel();

        invoker.invokeOneWay(channel, "log", "Log message");

        Map<String, Object> request = channel.readOutbound();
        assertThat(request.get("type")).isEqualTo("log");
        assertThat(request.get("data")).isEqualTo("Log message");

        channel.close();
    }

    // ==================== Tests for timeout expiration (lines 236-240) ====================

    @Test
    void invoke_requestTimesOut_completesExceptionally() throws Exception {
        // Create invoker with very short timeout
        ClientSpec shortTimeoutSpec = new ClientSpec();
        shortTimeoutSpec.setName("timeout-test");
        TimeoutSpec timeout = new TimeoutSpec();
        timeout.setRequestMs(100); // 100ms timeout
        shortTimeoutSpec.setTimeout(timeout);
        RequestInvoker shortInvoker = new RequestInvoker(shortTimeoutSpec, new JsonNettyCodec());

        EmbeddedChannel channel = new EmbeddedChannel();

        CompletableFuture<Object> future = shortInvoker.invoke(channel, "slow-request", null, 0);

        // Don't complete the request - let it timeout
        // Wait for the timeout checker to run (runs every 1 second)
        Thread.sleep(1500);

        // The request should be completed exceptionally due to timeout
        assertThat(future.isCompletedExceptionally()).isTrue();
        assertThat(shortInvoker.getPendingRequestCount()).isEqualTo(0);

        shortInvoker.close();
        channel.close();
    }

}
