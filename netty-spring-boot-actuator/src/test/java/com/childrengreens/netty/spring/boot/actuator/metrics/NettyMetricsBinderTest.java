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

package com.childrengreens.netty.spring.boot.actuator.metrics;

import com.childrengreens.netty.spring.boot.context.client.ClientRuntime;
import com.childrengreens.netty.spring.boot.context.client.ConnectionPool;
import com.childrengreens.netty.spring.boot.context.client.NettyClientOrchestrator;
import com.childrengreens.netty.spring.boot.context.metrics.ClientMetrics;
import com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics;
import com.childrengreens.netty.spring.boot.context.properties.ClientSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import com.childrengreens.netty.spring.boot.context.properties.TransportType;
import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.ServerRuntime;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link NettyMetricsBinder}.
 */
class NettyMetricsBinderTest {

    private NettyServerOrchestrator serverOrchestrator;
    private NettyClientOrchestrator clientOrchestrator;
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        serverOrchestrator = mock(NettyServerOrchestrator.class);
        clientOrchestrator = mock(NettyClientOrchestrator.class);
        registry = new SimpleMeterRegistry();
    }

    @Test
    void bindTo_withNoServers_registersNoMetrics() {
        when(serverOrchestrator.getAllRuntimes()).thenReturn(new HashMap<>());

        NettyMetricsBinder binder = new NettyMetricsBinder(serverOrchestrator);
        binder.bindTo(registry);

        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void bindTo_withServer_registersServerStateGauge() {
        Map<String, ServerRuntime> runtimes = new HashMap<>();
        runtimes.put("testServer", createServerRuntime("testServer", true));
        when(serverOrchestrator.getAllRuntimes()).thenReturn(runtimes);

        NettyMetricsBinder binder = new NettyMetricsBinder(serverOrchestrator);
        binder.bindTo(registry);

        assertThat(registry.find("netty.server.state").gauge()).isNotNull();
    }

    @Test
    void serverStateGauge_whenRunning_returns1() {
        Map<String, ServerRuntime> runtimes = new HashMap<>();
        ServerRuntime runtime = createServerRuntime("testServer", true);
        runtimes.put("testServer", runtime);
        when(serverOrchestrator.getAllRuntimes()).thenReturn(runtimes);

        NettyMetricsBinder binder = new NettyMetricsBinder(serverOrchestrator);
        binder.bindTo(registry);

        Double value = registry.find("netty.server.state")
                .tag("server", "testServer")
                .gauge()
                .value();
        assertThat(value).isEqualTo(1.0);
    }

    @Test
    void serverStateGauge_whenNotRunning_returns0() {
        Map<String, ServerRuntime> runtimes = new HashMap<>();
        ServerRuntime runtime = createServerRuntime("testServer", false);
        runtimes.put("testServer", runtime);
        when(serverOrchestrator.getAllRuntimes()).thenReturn(runtimes);

        NettyMetricsBinder binder = new NettyMetricsBinder(serverOrchestrator);
        binder.bindTo(registry);

        Double value = registry.find("netty.server.state")
                .tag("server", "testServer")
                .gauge()
                .value();
        assertThat(value).isEqualTo(0.0);
    }

    @Test
    void bindTo_multipleServers_registersMetricsForEach() {
        Map<String, ServerRuntime> runtimes = new HashMap<>();
        runtimes.put("server1", createServerRuntime("server1", true));
        runtimes.put("server2", createServerRuntime("server2", true));
        when(serverOrchestrator.getAllRuntimes()).thenReturn(runtimes);

        NettyMetricsBinder binder = new NettyMetricsBinder(serverOrchestrator);
        binder.bindTo(registry);

        assertThat(registry.find("netty.server.state").tag("server", "server1").gauge()).isNotNull();
        assertThat(registry.find("netty.server.state").tag("server", "server2").gauge()).isNotNull();
    }

    @Test
    void bindTo_withServer_registersAllServerMetrics() {
        Map<String, ServerRuntime> runtimes = new HashMap<>();
        runtimes.put("testServer", createServerRuntime("testServer", true));
        when(serverOrchestrator.getAllRuntimes()).thenReturn(runtimes);

        NettyMetricsBinder binder = new NettyMetricsBinder(serverOrchestrator);
        binder.bindTo(registry);

        assertThat(registry.find("netty.server.state").gauge()).isNotNull();
        assertThat(registry.find("netty.server.connections.current").gauge()).isNotNull();
        assertThat(registry.find("netty.server.connections.total").gauge()).isNotNull();
        assertThat(registry.find("netty.server.bytes.in").gauge()).isNotNull();
        assertThat(registry.find("netty.server.bytes.out").gauge()).isNotNull();
        assertThat(registry.find("netty.server.requests.total").gauge()).isNotNull();
        assertThat(registry.find("netty.server.request.latency.total").gauge()).isNotNull();
    }

    @Test
    void bindTo_withClient_registersAllClientMetrics() {
        Map<String, ClientRuntime> runtimes = new HashMap<>();
        runtimes.put("testClient", createClientRuntime("testClient"));
        when(clientOrchestrator.getAllRuntimes()).thenReturn(runtimes);

        NettyMetricsBinder binder = new NettyMetricsBinder(null, clientOrchestrator);
        binder.bindTo(registry);

        assertThat(registry.find("netty.client.connections.current").gauge()).isNotNull();
        assertThat(registry.find("netty.client.pool.size").gauge()).isNotNull();
        assertThat(registry.find("netty.client.pool.pending").gauge()).isNotNull();
        assertThat(registry.find("netty.client.requests.total").gauge()).isNotNull();
        assertThat(registry.find("netty.client.request.latency.total").gauge()).isNotNull();
        assertThat(registry.find("netty.client.reconnect.count").gauge()).isNotNull();
    }

    @Test
    void bindTo_withBothServerAndClient_registersAllMetrics() {
        Map<String, ServerRuntime> serverRuntimes = new HashMap<>();
        serverRuntimes.put("testServer", createServerRuntime("testServer", true));
        when(serverOrchestrator.getAllRuntimes()).thenReturn(serverRuntimes);

        Map<String, ClientRuntime> clientRuntimes = new HashMap<>();
        clientRuntimes.put("testClient", createClientRuntime("testClient"));
        when(clientOrchestrator.getAllRuntimes()).thenReturn(clientRuntimes);

        NettyMetricsBinder binder = new NettyMetricsBinder(serverOrchestrator, clientOrchestrator);
        binder.bindTo(registry);

        // Server metrics
        assertThat(registry.find("netty.server.state").gauge()).isNotNull();
        // Client metrics
        assertThat(registry.find("netty.client.connections.current").gauge()).isNotNull();
    }

    @Test
    void bindTo_withNullOrchestrators_doesNotThrow() {
        NettyMetricsBinder binder = new NettyMetricsBinder(null, null);
        binder.bindTo(registry);

        assertThat(registry.getMeters()).isEmpty();
    }

    private ServerRuntime createServerRuntime(String name, boolean running) {
        ServerRuntime runtime = mock(ServerRuntime.class);
        ServerSpec spec = mock(ServerSpec.class);
        ServerMetrics metrics = new ServerMetrics(name);

        when(spec.getName()).thenReturn(name);
        when(spec.getTransport()).thenReturn(TransportType.TCP);
        when(spec.getProfile()).thenReturn("tcp-lengthfield-json");
        when(runtime.getSpec()).thenReturn(spec);
        when(runtime.isRunning()).thenReturn(running);
        when(runtime.getMetrics()).thenReturn(metrics);
        return runtime;
    }

    private ClientRuntime createClientRuntime(String name) {
        ClientRuntime runtime = mock(ClientRuntime.class);
        ClientSpec spec = mock(ClientSpec.class);
        ConnectionPool connectionPool = mock(ConnectionPool.class);
        ClientMetrics metrics = new ClientMetrics(name);

        when(spec.getName()).thenReturn(name);
        when(spec.getHost()).thenReturn("localhost");
        when(spec.getPort()).thenReturn(8080);
        when(spec.getProfile()).thenReturn("tcp-lengthfield-json");
        when(runtime.getClientSpec()).thenReturn(spec);
        when(runtime.getMetrics()).thenReturn(metrics);
        when(runtime.getConnectionPool()).thenReturn(connectionPool);
        when(connectionPool.getBorrowedConnections()).thenReturn(5);
        when(connectionPool.getTotalConnections()).thenReturn(10);
        when(connectionPool.getPendingAcquires()).thenReturn(2);
        return runtime;
    }
}
