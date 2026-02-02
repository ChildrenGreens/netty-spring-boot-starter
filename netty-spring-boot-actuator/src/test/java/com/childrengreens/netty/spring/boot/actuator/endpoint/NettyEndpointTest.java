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

package com.childrengreens.netty.spring.boot.actuator.endpoint;

import com.childrengreens.netty.spring.boot.context.client.ClientRuntime;
import com.childrengreens.netty.spring.boot.context.client.ConnectionPool;
import com.childrengreens.netty.spring.boot.context.client.NettyClientOrchestrator;
import com.childrengreens.netty.spring.boot.context.properties.*;
import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.ServerRuntime;
import com.childrengreens.netty.spring.boot.context.server.ServerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link NettyEndpoint}.
 */
class NettyEndpointTest {

    private NettyServerOrchestrator serverOrchestrator;
    private NettyClientOrchestrator clientOrchestrator;
    private NettyEndpoint endpoint;

    @BeforeEach
    void setUp() {
        serverOrchestrator = mock(NettyServerOrchestrator.class);
        clientOrchestrator = mock(NettyClientOrchestrator.class);
        endpoint = new NettyEndpoint(serverOrchestrator, clientOrchestrator);
    }

    @Test
    void summary_withNoServersOrClients_returnsEmptyLists() {
        when(serverOrchestrator.getAllRuntimes()).thenReturn(new HashMap<>());
        when(clientOrchestrator.getAllRuntimes()).thenReturn(new HashMap<>());

        Map<String, Object> result = endpoint.summary();

        assertThat(result).containsKey("servers");
        assertThat(result).containsKey("clients");
    }

    @Test
    void group_servers_withServers_returnsServerInfo() {
        Map<String, ServerRuntime> runtimes = new HashMap<>();
        runtimes.put("testServer", createRuntime("testServer", true));
        when(serverOrchestrator.getAllRuntimes()).thenReturn(runtimes);

        Map<String, Object> result = endpoint.group("servers");

        assertThat(result).containsKey("servers");
        assertThat(result).containsEntry("serverCount", 1);
    }

    @Test
    void group_clients_withClients_returnsClientInfo() {
        Map<String, ClientRuntime> runtimes = new HashMap<>();
        runtimes.put("order-service", createClientRuntime("order-service"));
        when(clientOrchestrator.getAllRuntimes()).thenReturn(runtimes);

        Map<String, Object> result = endpoint.group("clients");

        assertThat(result).containsKey("clients");
        assertThat(result).containsEntry("clientCount", 1);
    }

    @Test
    void server_withExistingServer_returnsServerInfo() {
        ServerRuntime runtime = createRuntime("testServer", true);
        when(serverOrchestrator.getRuntime("testServer")).thenReturn(runtime);

        Map<String, Object> result = endpoint.server("testServer");

        assertThat(result).isNotNull();
        assertThat(result).containsEntry("name", "testServer");
    }

    @Test
    void server_withNonExistingServer_returnsNull() {
        when(serverOrchestrator.getRuntime("unknown")).thenReturn(null);

        Map<String, Object> result = endpoint.server("unknown");

        assertThat(result).isNull();
    }

    private ServerRuntime createRuntime(String name, boolean running) {
        ServerRuntime runtime = mock(ServerRuntime.class);
        ServerSpec spec = mock(ServerSpec.class);
        FeaturesSpec features = mock(FeaturesSpec.class);
        RoutingSpec routing = mock(RoutingSpec.class);
        com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics metrics =
                mock(com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics.class);

        when(spec.getName()).thenReturn(name);
        when(spec.getTransport()).thenReturn(TransportType.TCP);
        when(spec.getHost()).thenReturn("0.0.0.0");
        when(spec.getPort()).thenReturn(8080);
        when(spec.getProfile()).thenReturn("tcp-lengthfield-json");
        when(spec.getFeatures()).thenReturn(features);
        when(spec.getRouting()).thenReturn(routing);
        when(routing.getMode()).thenReturn(RoutingMode.MESSAGE_TYPE);
        when(runtime.getSpec()).thenReturn(spec);
        when(runtime.isRunning()).thenReturn(running);
        when(runtime.getState()).thenReturn(running ? ServerState.RUNNING : ServerState.STOPPED);
        when(runtime.getMetrics()).thenReturn(metrics);
        when(metrics.getCurrentConnections()).thenReturn(0);
        when(metrics.getTotalConnections()).thenReturn(0L);
        return runtime;
    }

    private ClientRuntime createClientRuntime(String name) {
        ClientRuntime runtime = mock(ClientRuntime.class);
        ClientSpec spec = mock(ClientSpec.class);
        ConnectionPool pool = mock(ConnectionPool.class);

        when(spec.getName()).thenReturn(name);
        when(spec.getHost()).thenReturn("127.0.0.1");
        when(spec.getPort()).thenReturn(9000);
        when(spec.getProfile()).thenReturn("tcp-lengthfield-json");
        when(runtime.getClientSpec()).thenReturn(spec);
        when(runtime.getConnectionPool()).thenReturn(pool);
        when(runtime.getState()).thenReturn(ClientRuntime.ClientState.RUNNING);
        when(pool.getTotalConnections()).thenReturn(10);
        when(pool.getBorrowedConnections()).thenReturn(3);
        when(pool.getIdleConnections()).thenReturn(7);
        when(pool.getPendingAcquires()).thenReturn(0);
        return runtime;
    }
}
