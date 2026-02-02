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

package com.childrengreens.netty.spring.boot.actuator.health;

import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import com.childrengreens.netty.spring.boot.context.properties.TransportType;
import com.childrengreens.netty.spring.boot.context.client.NettyClientOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.ServerRuntime;
import com.childrengreens.netty.spring.boot.context.server.ServerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link NettyHealthIndicator}.
 */
class NettyHealthIndicatorTest {

    private NettyServerOrchestrator orchestrator;
    private NettyClientOrchestrator clientOrchestrator;
    private NettyHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        orchestrator = mock(NettyServerOrchestrator.class);
        clientOrchestrator = mock(NettyClientOrchestrator.class);
        healthIndicator = new NettyHealthIndicator(orchestrator, clientOrchestrator);
        when(clientOrchestrator.getAllRuntimes()).thenReturn(new HashMap<>());
    }

    @Test
    void health_withNoServers_returnsUnknown() {
        when(orchestrator.getAllRuntimes()).thenReturn(new HashMap<>());
        when(clientOrchestrator.getAllRuntimes()).thenReturn(new HashMap<>());

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("message", "No Netty servers or clients configured");
    }

    @Test
    void health_withAllServersRunning_returnsUp() {
        Map<String, ServerRuntime> runtimes = new HashMap<>();
        runtimes.put("server1", createRuntime("server1", true));
        runtimes.put("server2", createRuntime("server2", true));
        when(orchestrator.getAllRuntimes()).thenReturn(runtimes);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("servers");
        @SuppressWarnings("unchecked")
        Map<String, Object> servers = (Map<String, Object>) health.getDetails().get("servers");
        assertThat(servers).containsKey("server1");
        assertThat(servers).containsKey("server2");
    }

    @Test
    void health_withServerNotRunning_returnsDown() {
        Map<String, ServerRuntime> runtimes = new HashMap<>();
        runtimes.put("server1", createRuntime("server1", true));
        runtimes.put("server2", createRuntime("server2", false));
        when(orchestrator.getAllRuntimes()).thenReturn(runtimes);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    private ServerRuntime createRuntime(String name, boolean running) {
        ServerRuntime runtime = mock(ServerRuntime.class);
        ServerSpec spec = mock(ServerSpec.class);
        when(spec.getName()).thenReturn(name);
        when(spec.getTransport()).thenReturn(TransportType.TCP);
        when(spec.getHost()).thenReturn("0.0.0.0");
        when(spec.getPort()).thenReturn(8080);
        when(spec.getProfile()).thenReturn("tcp-lengthfield-json");
        when(runtime.getSpec()).thenReturn(spec);
        when(runtime.isRunning()).thenReturn(running);
        when(runtime.getState()).thenReturn(running ? ServerState.RUNNING : ServerState.STOPPED);
        return runtime;
    }
}
