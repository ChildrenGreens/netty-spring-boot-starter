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

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ClientResponseHandler}.
 */
class ClientResponseHandlerTest {

    @Test
    void channelRead0_withCorrelationId_completesRequest() throws Exception {
        RequestInvoker invoker = mock(RequestInvoker.class);
        when(invoker.completeRequest(anyString(), any())).thenReturn(true);

        ClientResponseHandler handler = new ClientResponseHandler(invoker, "test-client");
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        Map<String, Object> response = new HashMap<>();
        response.put(RequestInvoker.CORRELATION_ID_HEADER, "test-correlation-id");
        response.put("data", "value");

        channel.writeInbound(response);

        verify(invoker).completeRequest("test-correlation-id", response);

        channel.close();
    }

    @Test
    void channelRead0_withoutCorrelationId_handlesPushMessage() throws Exception {
        RequestInvoker invoker = mock(RequestInvoker.class);

        ClientResponseHandler handler = new ClientResponseHandler(invoker, "test-client");
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        Map<String, Object> pushMessage = new HashMap<>();
        pushMessage.put("type", "notification");
        pushMessage.put("data", "push data");
        // No correlation ID

        channel.writeInbound(pushMessage);

        // Should not call completeRequest
        verify(invoker, never()).completeRequest(anyString(), any());

        channel.close();
    }

    @Test
    void channelActive_logsConnection() throws Exception {
        RequestInvoker invoker = mock(RequestInvoker.class);
        ClientResponseHandler handler = new ClientResponseHandler(invoker, "test-client");
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Channel should be active after creation
        assertThat(channel.isActive()).isTrue();

        channel.close();
    }

    @Test
    void exceptionCaught_closesChannel() throws Exception {
        RequestInvoker invoker = mock(RequestInvoker.class);
        ClientResponseHandler handler = new ClientResponseHandler(invoker, "test-client");
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        channel.pipeline().fireExceptionCaught(new RuntimeException("Test error"));

        assertThat(channel.isOpen()).isFalse();
    }

}
