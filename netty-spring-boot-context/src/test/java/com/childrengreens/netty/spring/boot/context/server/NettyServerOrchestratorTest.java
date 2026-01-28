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

package com.childrengreens.netty.spring.boot.context.server;

import com.childrengreens.netty.spring.boot.context.pipeline.PipelineAssembler;
import com.childrengreens.netty.spring.boot.context.properties.*;
import com.childrengreens.netty.spring.boot.context.transport.TransportFactory;
import io.netty.channel.EventLoopGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link NettyServerOrchestrator}.
 */
class NettyServerOrchestratorTest {

    private NettyProperties properties;
    private TransportFactory transportFactory;
    private PipelineAssembler pipelineAssembler;
    private NettyServerOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        properties = new NettyProperties();
        transportFactory = new TransportFactory(TransportImpl.NIO);
        pipelineAssembler = mock(PipelineAssembler.class);

        orchestrator = new NettyServerOrchestrator(properties, transportFactory, pipelineAssembler);
    }

    @AfterEach
    void tearDown() {
        if (orchestrator != null) {
            orchestrator.stop();
        }
    }

    @Test
    void start_whenDisabled_doesNotStart() {
        properties.setEnabled(false);

        orchestrator.start();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void start_whenNoServers_doesNotStart() {
        properties.setEnabled(true);
        properties.setServers(Collections.emptyList());

        orchestrator.start();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void getRuntime_whenNotExists_returnsNull() {
        ServerRuntime runtime = orchestrator.getRuntime("non-existent");

        assertThat(runtime).isNull();
    }

    @Test
    void getAllRuntimes_initiallyEmpty() {
        Map<String, ServerRuntime> runtimes = orchestrator.getAllRuntimes();

        assertThat(runtimes).isEmpty();
    }

    @Test
    void setFailFast_setsValue() {
        orchestrator.setFailFast(false);
        // No exception thrown, value is set internally
    }

    @Test
    void stop_whenNoServers_doesNotThrow() {
        orchestrator.stop();
        // No exception thrown
    }

    @Test
    void afterPropertiesSet_callsStart() throws Exception {
        properties.setEnabled(false);

        orchestrator.afterPropertiesSet();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void destroy_callsStop() throws Exception {
        orchestrator.destroy();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void start_stop_lifecycle() {
        properties.setEnabled(true);
        properties.setServers(Collections.emptyList());

        orchestrator.start();
        assertThat(orchestrator.getAllRuntimes()).isEmpty();

        orchestrator.stop();
        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void getAllRuntimes_returnsUnmodifiableMap() {
        Map<String, ServerRuntime> runtimes = orchestrator.getAllRuntimes();

        assertThat(runtimes).isNotNull();
        // Verify it's unmodifiable by checking the type
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> runtimes.put("test", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void setFailFast_toFalse_allowsNonFailFastBehavior() {
        orchestrator.setFailFast(false);
        orchestrator.setFailFast(true);
        // No exception, just verifying the method can be called
    }

    @Test
    void start_withValidServer_startsSuccessfully() {
        properties.setEnabled(true);

        ServerSpec serverSpec = createValidServerSpec("test-server", 18000);
        properties.setServers(Collections.singletonList(serverSpec));

        orchestrator.start();

        assertThat(orchestrator.getAllRuntimes()).hasSize(1);
        assertThat(orchestrator.getRuntime("test-server")).isNotNull();
        assertThat(orchestrator.getRuntime("test-server").getState()).isEqualTo(ServerState.RUNNING);
    }

    @Test
    void start_withMultipleServers_startsAll() {
        properties.setEnabled(true);

        List<ServerSpec> servers = new ArrayList<>();
        servers.add(createValidServerSpec("server-1", 18001));
        servers.add(createValidServerSpec("server-2", 18002));
        properties.setServers(servers);

        orchestrator.start();

        assertThat(orchestrator.getAllRuntimes()).hasSize(2);
        assertThat(orchestrator.getRuntime("server-1")).isNotNull();
        assertThat(orchestrator.getRuntime("server-2")).isNotNull();
    }

    @Test
    void stop_afterStart_stopsAllServers() {
        properties.setEnabled(true);

        ServerSpec serverSpec = createValidServerSpec("stop-test-server", 18003);
        properties.setServers(Collections.singletonList(serverSpec));

        orchestrator.start();
        assertThat(orchestrator.getAllRuntimes()).hasSize(1);

        ServerRuntime runtime = orchestrator.getRuntime("stop-test-server");
        assertThat(runtime.getState()).isEqualTo(ServerState.RUNNING);

        orchestrator.stop();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
        assertThat(runtime.getState()).isEqualTo(ServerState.STOPPED);
    }

    @Test
    void start_withServerSpecificThreads_usesServerThreads() {
        properties.setEnabled(true);

        ServerSpec serverSpec = createValidServerSpec("threads-server", 18004);
        ThreadsSpec serverThreads = new ThreadsSpec();
        serverThreads.setBoss(2);
        serverThreads.setWorker(4);
        serverSpec.setThreads(serverThreads);
        properties.setServers(Collections.singletonList(serverSpec));

        orchestrator.start();

        assertThat(orchestrator.getRuntime("threads-server")).isNotNull();
    }

    @Test
    void start_withNullServerThreads_usesDefaultThreads() {
        properties.setEnabled(true);

        ServerSpec serverSpec = createValidServerSpec("default-threads-server", 18005);
        serverSpec.setThreads(null);
        properties.setServers(Collections.singletonList(serverSpec));

        orchestrator.start();

        assertThat(orchestrator.getRuntime("default-threads-server")).isNotNull();
    }

    @Test
    void start_withZeroBossThreads_usesDefaultBossThreads() {
        properties.setEnabled(true);

        ServerSpec serverSpec = createValidServerSpec("zero-boss-server", 18006);
        ThreadsSpec serverThreads = new ThreadsSpec();
        serverThreads.setBoss(0); // Zero should use defaults
        serverThreads.setWorker(2);
        serverSpec.setThreads(serverThreads);
        properties.setServers(Collections.singletonList(serverSpec));

        orchestrator.start();

        assertThat(orchestrator.getRuntime("zero-boss-server")).isNotNull();
    }

    @Test
    void start_withNegativeWorkerThreads_usesDefaultWorkerThreads() {
        properties.setEnabled(true);

        ServerSpec serverSpec = createValidServerSpec("negative-worker-server", 18007);
        ThreadsSpec serverThreads = new ThreadsSpec();
        serverThreads.setBoss(1);
        serverThreads.setWorker(-1); // Negative should use defaults
        serverSpec.setThreads(serverThreads);
        properties.setServers(Collections.singletonList(serverSpec));

        orchestrator.start();

        assertThat(orchestrator.getRuntime("negative-worker-server")).isNotNull();
    }

    @Test
    void start_withFailFastFalse_continuesOnError() {
        TransportFactory failingTransportFactory = mock(TransportFactory.class);
        when(failingTransportFactory.createBossGroup(anyInt()))
                .thenThrow(new RuntimeException("Simulated failure"));

        NettyServerOrchestrator failingOrchestrator =
                new NettyServerOrchestrator(properties, failingTransportFactory, pipelineAssembler);

        properties.setEnabled(true);
        ServerSpec serverSpec = createValidServerSpec("failing-server", 18008);
        properties.setServers(Collections.singletonList(serverSpec));

        failingOrchestrator.setFailFast(false);

        // Should not throw even though server fails
        failingOrchestrator.start();

        assertThat(failingOrchestrator.getAllRuntimes()).isEmpty();

        failingOrchestrator.stop();
    }

    @Test
    void start_withFailFastTrue_throwsOnError() {
        TransportFactory failingTransportFactory = mock(TransportFactory.class);
        when(failingTransportFactory.createBossGroup(anyInt()))
                .thenThrow(new RuntimeException("Simulated failure"));

        NettyServerOrchestrator failingOrchestrator =
                new NettyServerOrchestrator(properties, failingTransportFactory, pipelineAssembler);

        properties.setEnabled(true);
        ServerSpec serverSpec = createValidServerSpec("failing-server", 18009);
        properties.setServers(Collections.singletonList(serverSpec));

        failingOrchestrator.setFailFast(true);

        assertThatThrownBy(failingOrchestrator::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to start server");

        failingOrchestrator.stop();
    }

    @Test
    void afterPropertiesSet_startsServers() throws Exception {
        properties.setEnabled(true);

        ServerSpec serverSpec = createValidServerSpec("afterprops-server", 18010);
        properties.setServers(Collections.singletonList(serverSpec));

        orchestrator.afterPropertiesSet();

        assertThat(orchestrator.getAllRuntimes()).hasSize(1);
    }

    @Test
    void destroy_stopsAllServers() throws Exception {
        properties.setEnabled(true);

        ServerSpec serverSpec = createValidServerSpec("destroy-server", 18011);
        properties.setServers(Collections.singletonList(serverSpec));

        orchestrator.start();
        assertThat(orchestrator.getAllRuntimes()).hasSize(1);

        orchestrator.destroy();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void getRuntime_afterStart_returnsRuntime() {
        properties.setEnabled(true);

        ServerSpec serverSpec = createValidServerSpec("get-runtime-server", 18012);
        properties.setServers(Collections.singletonList(serverSpec));

        orchestrator.start();

        ServerRuntime runtime = orchestrator.getRuntime("get-runtime-server");
        assertThat(runtime).isNotNull();
        assertThat(runtime.getSpec().getName()).isEqualTo("get-runtime-server");
        assertThat(runtime.getSpec()).isEqualTo(serverSpec);
    }

    @Test
    void stop_shutdownsEventLoopGroups() {
        properties.setEnabled(true);

        ServerSpec serverSpec = createValidServerSpec("shutdown-server", 18013);
        properties.setServers(Collections.singletonList(serverSpec));

        orchestrator.start();

        ServerRuntime runtime = orchestrator.getRuntime("shutdown-server");
        EventLoopGroup bossGroup = runtime.getBossGroup();
        EventLoopGroup workerGroup = runtime.getWorkerGroup();

        assertThat(bossGroup.isShutdown()).isFalse();
        assertThat(workerGroup.isShutdown()).isFalse();

        orchestrator.stop();

        // Groups should be shutting down
        assertThat(bossGroup.isShuttingDown() || bossGroup.isShutdown()).isTrue();
        assertThat(workerGroup.isShuttingDown() || workerGroup.isShutdown()).isTrue();
    }

    @Test
    void start_withUdpTransport_startsSuccessfully() {
        properties.setEnabled(true);

        ServerSpec serverSpec = createValidServerSpec("udp-server", 18014);
        serverSpec.setTransport(TransportType.UDP);
        properties.setServers(Collections.singletonList(serverSpec));

        orchestrator.start();

        assertThat(orchestrator.getAllRuntimes()).hasSize(1);
        assertThat(orchestrator.getRuntime("udp-server")).isNotNull();
    }

    private ServerSpec createValidServerSpec(String name, int port) {
        ServerSpec spec = new ServerSpec();
        spec.setName(name);
        spec.setHost("0.0.0.0");
        spec.setPort(port);
        spec.setTransport(TransportType.TCP);
        spec.setProfile("tcp-lengthfield-json");

        return spec;
    }

}
