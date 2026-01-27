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

    private NettyServerOrchestrator orchestrator;
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        orchestrator = mock(NettyServerOrchestrator.class);
        registry = new SimpleMeterRegistry();
    }

    @Test
    void bindTo_withNoServers_registersNoMetrics() {
        when(orchestrator.getAllRuntimes()).thenReturn(new HashMap<>());

        NettyMetricsBinder binder = new NettyMetricsBinder(orchestrator);
        binder.bindTo(registry);

        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void bindTo_withServer_registersServerStateGauge() {
        Map<String, ServerRuntime> runtimes = new HashMap<>();
        runtimes.put("testServer", createRuntime("testServer", true));
        when(orchestrator.getAllRuntimes()).thenReturn(runtimes);

        NettyMetricsBinder binder = new NettyMetricsBinder(orchestrator);
        binder.bindTo(registry);

        assertThat(registry.find("netty.server.state").gauge()).isNotNull();
    }

    @Test
    void serverStateGauge_whenRunning_returns1() {
        Map<String, ServerRuntime> runtimes = new HashMap<>();
        ServerRuntime runtime = createRuntime("testServer", true);
        runtimes.put("testServer", runtime);
        when(orchestrator.getAllRuntimes()).thenReturn(runtimes);

        NettyMetricsBinder binder = new NettyMetricsBinder(orchestrator);
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
        ServerRuntime runtime = createRuntime("testServer", false);
        runtimes.put("testServer", runtime);
        when(orchestrator.getAllRuntimes()).thenReturn(runtimes);

        NettyMetricsBinder binder = new NettyMetricsBinder(orchestrator);
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
        runtimes.put("server1", createRuntime("server1", true));
        runtimes.put("server2", createRuntime("server2", true));
        when(orchestrator.getAllRuntimes()).thenReturn(runtimes);

        NettyMetricsBinder binder = new NettyMetricsBinder(orchestrator);
        binder.bindTo(registry);

        assertThat(registry.find("netty.server.state").tag("server", "server1").gauge()).isNotNull();
        assertThat(registry.find("netty.server.state").tag("server", "server2").gauge()).isNotNull();
    }

    private ServerRuntime createRuntime(String name, boolean running) {
        ServerRuntime runtime = mock(ServerRuntime.class);
        ServerSpec spec = mock(ServerSpec.class);
        when(spec.getName()).thenReturn(name);
        when(spec.getTransport()).thenReturn(TransportType.TCP);
        when(spec.getProfile()).thenReturn("tcp-lengthfield-json");
        when(runtime.getSpec()).thenReturn(spec);
        when(runtime.isRunning()).thenReturn(running);
        return runtime;
    }
}
