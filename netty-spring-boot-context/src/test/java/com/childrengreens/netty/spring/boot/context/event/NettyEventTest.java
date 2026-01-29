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

package com.childrengreens.netty.spring.boot.context.event;

import com.childrengreens.netty.spring.boot.context.client.NettyClientOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for Netty event classes.
 */
class NettyEventTest {

    // ==================== NettyServerStartedEvent Tests ====================

    @Test
    void serverStartedEvent_shouldReturnCorrectValues() {
        NettyServerOrchestrator orchestrator = mock(NettyServerOrchestrator.class);
        NettyServerStartedEvent event = new NettyServerStartedEvent(
                orchestrator, "test-server", "0.0.0.0", 8080, "tcp-json");

        assertThat(event.getName()).isEqualTo("test-server");
        assertThat(event.getHost()).isEqualTo("0.0.0.0");
        assertThat(event.getPort()).isEqualTo(8080);
        assertThat(event.getProfile()).isEqualTo("tcp-json");
        assertThat(event.getSource()).isSameAs(orchestrator);
        assertThat(event.getEventTimestamp()).isNotNull();
    }

    @Test
    void serverStartedEvent_toString_shouldContainAllFields() {
        NettyServerOrchestrator orchestrator = mock(NettyServerOrchestrator.class);
        NettyServerStartedEvent event = new NettyServerStartedEvent(
                orchestrator, "test-server", "0.0.0.0", 8080, "tcp-json");

        String str = event.toString();

        assertThat(str).contains("test-server");
        assertThat(str).contains("0.0.0.0");
        assertThat(str).contains("8080");
        assertThat(str).contains("tcp-json");
    }

    // ==================== NettyServerStoppedEvent Tests ====================

    @Test
    void serverStoppedEvent_shouldReturnCorrectValues() {
        NettyServerOrchestrator orchestrator = mock(NettyServerOrchestrator.class);
        NettyServerStoppedEvent event = new NettyServerStoppedEvent(orchestrator, "test-server");

        assertThat(event.getName()).isEqualTo("test-server");
        assertThat(event.getSource()).isSameAs(orchestrator);
        assertThat(event.getEventTimestamp()).isNotNull();
    }

    @Test
    void serverStoppedEvent_toString_shouldContainName() {
        NettyServerOrchestrator orchestrator = mock(NettyServerOrchestrator.class);
        NettyServerStoppedEvent event = new NettyServerStoppedEvent(orchestrator, "test-server");

        String str = event.toString();

        assertThat(str).contains("test-server");
    }

    // ==================== NettyClientConnectedEvent Tests ====================

    @Test
    void clientConnectedEvent_shouldReturnCorrectValues() {
        NettyClientOrchestrator orchestrator = mock(NettyClientOrchestrator.class);
        NettyClientConnectedEvent event = new NettyClientConnectedEvent(
                orchestrator, "test-client", "127.0.0.1", 9000);

        assertThat(event.getName()).isEqualTo("test-client");
        assertThat(event.getHost()).isEqualTo("127.0.0.1");
        assertThat(event.getPort()).isEqualTo(9000);
        assertThat(event.getSource()).isSameAs(orchestrator);
        assertThat(event.getEventTimestamp()).isNotNull();
    }

    @Test
    void clientConnectedEvent_toString_shouldContainAllFields() {
        NettyClientOrchestrator orchestrator = mock(NettyClientOrchestrator.class);
        NettyClientConnectedEvent event = new NettyClientConnectedEvent(
                orchestrator, "test-client", "127.0.0.1", 9000);

        String str = event.toString();

        assertThat(str).contains("test-client");
        assertThat(str).contains("127.0.0.1");
        assertThat(str).contains("9000");
    }

    // ==================== NettyClientDisconnectedEvent Tests ====================

    @Test
    void clientDisconnectedEvent_shouldReturnCorrectValues() {
        NettyClientOrchestrator orchestrator = mock(NettyClientOrchestrator.class);
        NettyClientDisconnectedEvent event = new NettyClientDisconnectedEvent(
                orchestrator, "test-client", "127.0.0.1", 9000, "connection reset");

        assertThat(event.getName()).isEqualTo("test-client");
        assertThat(event.getHost()).isEqualTo("127.0.0.1");
        assertThat(event.getPort()).isEqualTo(9000);
        assertThat(event.getReason()).isEqualTo("connection reset");
        assertThat(event.getSource()).isSameAs(orchestrator);
        assertThat(event.getEventTimestamp()).isNotNull();
    }

    @Test
    void clientDisconnectedEvent_shouldAllowNullReason() {
        NettyClientOrchestrator orchestrator = mock(NettyClientOrchestrator.class);
        NettyClientDisconnectedEvent event = new NettyClientDisconnectedEvent(
                orchestrator, "test-client", "127.0.0.1", 9000, null);

        assertThat(event.getReason()).isNull();
    }

    @Test
    void clientDisconnectedEvent_toString_shouldContainAllFields() {
        NettyClientOrchestrator orchestrator = mock(NettyClientOrchestrator.class);
        NettyClientDisconnectedEvent event = new NettyClientDisconnectedEvent(
                orchestrator, "test-client", "127.0.0.1", 9000, "connection reset");

        String str = event.toString();

        assertThat(str).contains("test-client");
        assertThat(str).contains("127.0.0.1");
        assertThat(str).contains("9000");
        assertThat(str).contains("connection reset");
    }

}
