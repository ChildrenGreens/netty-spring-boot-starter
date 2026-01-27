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

    private NettyServerOrchestrator orchestrator;
    private NettyEndpoint endpoint;

    @BeforeEach
    void setUp() {
        orchestrator = mock(NettyServerOrchestrator.class);
        endpoint = new NettyEndpoint(orchestrator);
    }

    @Test
    void servers_withNoServers_returnsEmptyServersMap() {
        when(orchestrator.getAllRuntimes()).thenReturn(new HashMap<>());

        Map<String, Object> result = endpoint.servers();

        assertThat(result).containsKey("servers");
        assertThat(result).containsEntry("serverCount", 0);
    }

    @Test
    void servers_withServers_returnsServerInfo() {
        Map<String, ServerRuntime> runtimes = new HashMap<>();
        runtimes.put("testServer", createRuntime("testServer", true));
        when(orchestrator.getAllRuntimes()).thenReturn(runtimes);

        Map<String, Object> result = endpoint.servers();

        assertThat(result).containsKey("servers");
        assertThat(result).containsEntry("serverCount", 1);
    }

    @Test
    void server_withExistingServer_returnsServerInfo() {
        ServerRuntime runtime = createRuntime("testServer", true);
        when(orchestrator.getRuntime("testServer")).thenReturn(runtime);

        Map<String, Object> result = endpoint.server("testServer");

        assertThat(result).isNotNull();
        assertThat(result).containsEntry("name", "testServer");
    }

    @Test
    void server_withNonExistingServer_returnsNull() {
        when(orchestrator.getRuntime("unknown")).thenReturn(null);

        Map<String, Object> result = endpoint.server("unknown");

        assertThat(result).isNull();
    }

    private ServerRuntime createRuntime(String name, boolean running) {
        ServerRuntime runtime = mock(ServerRuntime.class);
        ServerSpec spec = mock(ServerSpec.class);
        FeaturesSpec features = mock(FeaturesSpec.class);
        RoutingSpec routing = mock(RoutingSpec.class);

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
        return runtime;
    }
}
