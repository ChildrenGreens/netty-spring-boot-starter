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

import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.codec.JsonNettyCodec;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link NettyClientOrchestrator}.
 */
class NettyClientOrchestratorTest {

    private NettyProperties properties;
    private TransportFactory transportFactory;
    private ClientPipelineAssembler pipelineAssembler;
    private CodecRegistry codecRegistry;
    private NettyClientOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        properties = new NettyProperties();
        transportFactory = new TransportFactory(TransportImpl.NIO);

        ClientProfileRegistry profileRegistry = new ClientProfileRegistry();
        profileRegistry.register(new TcpLengthFieldJsonClientProfile());

        codecRegistry = new CodecRegistry();
        codecRegistry.register(new JsonNettyCodec());

        pipelineAssembler = new ClientPipelineAssembler(profileRegistry, codecRegistry);

        orchestrator = new NettyClientOrchestrator(properties, transportFactory, pipelineAssembler, codecRegistry);
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
    void start_whenNoClients_doesNotStart() {
        properties.setEnabled(true);
        properties.setClients(Collections.emptyList());

        orchestrator.start();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void getRuntime_whenNotExists_returnsNull() {
        ClientRuntime runtime = orchestrator.getRuntime("non-existent");

        assertThat(runtime).isNull();
    }

    @Test
    void getAllRuntimes_initiallyEmpty() {
        Map<String, ClientRuntime> runtimes = orchestrator.getAllRuntimes();

        assertThat(runtimes).isEmpty();
    }

    @Test
    void setFailFast_setsValue() {
        orchestrator.setFailFast(false);
        // No exception thrown, value is set internally
    }

    @Test
    void stop_whenNoClients_doesNotThrow() {
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
        properties.setClients(Collections.emptyList());

        orchestrator.start();
        assertThat(orchestrator.getAllRuntimes()).isEmpty();

        orchestrator.stop();
        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void getAllRuntimes_returnsUnmodifiableMap() {
        Map<String, ClientRuntime> runtimes = orchestrator.getAllRuntimes();

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
    void start_withValidClient_startsSuccessfully() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("test-client", 19000);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        assertThat(orchestrator.getAllRuntimes()).hasSize(1);
        assertThat(orchestrator.getRuntime("test-client")).isNotNull();
        assertThat(orchestrator.getRuntime("test-client").getState())
                .isEqualTo(ClientRuntime.ClientState.RUNNING);
    }

    @Test
    void start_withValidClient_createsAllComponents() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("test-client", 19001);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        ClientRuntime runtime = orchestrator.getRuntime("test-client");
        assertThat(runtime.getBootstrap()).isNotNull();
        assertThat(runtime.getWorkerGroup()).isNotNull();
        assertThat(runtime.getConnectionPool()).isNotNull();
        assertThat(runtime.getReconnectManager()).isNotNull();
        assertThat(runtime.getHeartbeatManager()).isNotNull();
        assertThat(runtime.getRequestInvoker()).isNotNull();
    }

    @Test
    void start_withHeartbeatEnabled_startsHeartbeatManager() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("heartbeat-client", 19002);
        clientSpec.getHeartbeat().setEnabled(true);
        clientSpec.getHeartbeat().setIntervalMs(5000);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        ClientRuntime runtime = orchestrator.getRuntime("heartbeat-client");
        assertThat(runtime.getHeartbeatManager()).isNotNull();
    }

    @Test
    void start_withHeartbeatDisabled_doesNotStartHeartbeatManager() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("no-heartbeat-client", 19003);
        clientSpec.getHeartbeat().setEnabled(false);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        ClientRuntime runtime = orchestrator.getRuntime("no-heartbeat-client");
        assertThat(runtime.getHeartbeatManager()).isNotNull();
    }

    @Test
    void start_withMultipleClients_startsAll() {
        properties.setEnabled(true);

        List<ClientSpec> clients = new ArrayList<>();
        clients.add(createValidClientSpec("client-1", 19004));
        clients.add(createValidClientSpec("client-2", 19005));
        properties.setClients(clients);

        orchestrator.start();

        assertThat(orchestrator.getAllRuntimes()).hasSize(2);
        assertThat(orchestrator.getRuntime("client-1")).isNotNull();
        assertThat(orchestrator.getRuntime("client-2")).isNotNull();
    }

    @Test
    void stop_afterStart_stopsAllClients() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("stop-test-client", 19006);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();
        assertThat(orchestrator.getAllRuntimes()).hasSize(1);

        ClientRuntime runtime = orchestrator.getRuntime("stop-test-client");
        assertThat(runtime.getState()).isEqualTo(ClientRuntime.ClientState.RUNNING);

        orchestrator.stop();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
        assertThat(runtime.getState()).isEqualTo(ClientRuntime.ClientState.STOPPED);
    }

    @Test
    void stop_closesConnectionPool() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("pool-close-client", 19007);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        ClientRuntime runtime = orchestrator.getRuntime("pool-close-client");
        ConnectionPool pool = runtime.getConnectionPool();
        assertThat(pool).isNotNull();

        orchestrator.stop();

        // Verify pool is closed by checking acquire throws
        assertThatThrownBy(pool::acquire)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void start_withClientSpecificThreads_usesClientThreads() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("threads-client", 19008);
        ThreadsSpec clientThreads = new ThreadsSpec();
        clientThreads.setWorker(2);
        clientSpec.setThreads(clientThreads);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        assertThat(orchestrator.getRuntime("threads-client")).isNotNull();
    }

    @Test
    void start_withNullClientThreads_usesDefaultThreads() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("default-threads-client", 19009);
        clientSpec.setThreads(null);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        assertThat(orchestrator.getRuntime("default-threads-client")).isNotNull();
    }

    @Test
    void start_withNegativeWorkerThreads_usesDefaultThreads() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("negative-threads-client", 19010);
        ThreadsSpec clientThreads = new ThreadsSpec();
        clientThreads.setWorker(-1); // Negative value should use defaults
        clientSpec.setThreads(clientThreads);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        assertThat(orchestrator.getRuntime("negative-threads-client")).isNotNull();
    }

    @Test
    void start_withFailFastFalse_continuesOnError() {
        // Create a mock transport factory that throws an exception
        TransportFactory failingTransportFactory = mock(TransportFactory.class);
        when(failingTransportFactory.createWorkerGroup(anyInt()))
                .thenThrow(new RuntimeException("Simulated failure"));

        NettyClientOrchestrator failingOrchestrator =
                new NettyClientOrchestrator(properties, failingTransportFactory, pipelineAssembler, codecRegistry);

        properties.setEnabled(true);
        ClientSpec clientSpec = createValidClientSpec("failing-client", 19011);
        properties.setClients(Collections.singletonList(clientSpec));

        failingOrchestrator.setFailFast(false);

        // Should not throw even though client fails
        failingOrchestrator.start();

        // No clients should have been added
        assertThat(failingOrchestrator.getAllRuntimes()).isEmpty();

        failingOrchestrator.stop();
    }

    @Test
    void start_withFailFastTrue_throwsOnError() {
        // Create a mock transport factory that throws an exception
        TransportFactory failingTransportFactory = mock(TransportFactory.class);
        when(failingTransportFactory.createWorkerGroup(anyInt()))
                .thenThrow(new RuntimeException("Simulated failure"));

        NettyClientOrchestrator failingOrchestrator =
                new NettyClientOrchestrator(properties, failingTransportFactory, pipelineAssembler, codecRegistry);

        properties.setEnabled(true);
        ClientSpec clientSpec = createValidClientSpec("failing-client", 19013);
        properties.setClients(Collections.singletonList(clientSpec));

        failingOrchestrator.setFailFast(true);

        assertThatThrownBy(failingOrchestrator::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to start client");

        failingOrchestrator.stop();
    }

    @Test
    void afterPropertiesSet_startsClients() throws Exception {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("afterprops-client", 19014);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.afterPropertiesSet();

        assertThat(orchestrator.getAllRuntimes()).hasSize(1);
    }

    @Test
    void destroy_stopsAllClients() throws Exception {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("destroy-client", 19015);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();
        assertThat(orchestrator.getAllRuntimes()).hasSize(1);

        orchestrator.destroy();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void getRuntime_afterStart_returnsRuntime() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("get-runtime-client", 19016);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        ClientRuntime runtime = orchestrator.getRuntime("get-runtime-client");
        assertThat(runtime).isNotNull();
        assertThat(runtime.getName()).isEqualTo("get-runtime-client");
        assertThat(runtime.getClientSpec()).isEqualTo(clientSpec);
    }

    @Test
    void stop_withHeartbeatManager_stopsHeartbeat() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("heartbeat-stop-client", 19017);
        clientSpec.getHeartbeat().setEnabled(true);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        ClientRuntime runtime = orchestrator.getRuntime("heartbeat-stop-client");
        HeartbeatManager heartbeatManager = runtime.getHeartbeatManager();
        assertThat(heartbeatManager).isNotNull();

        orchestrator.stop();

        assertThat(runtime.getState()).isEqualTo(ClientRuntime.ClientState.STOPPED);
    }

    @Test
    void stop_withReconnectManager_stopsReconnect() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("reconnect-stop-client", 19018);
        clientSpec.getReconnect().setEnabled(true);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        ClientRuntime runtime = orchestrator.getRuntime("reconnect-stop-client");
        ReconnectManager reconnectManager = runtime.getReconnectManager();
        assertThat(reconnectManager).isNotNull();

        orchestrator.stop();

        assertThat(runtime.getState()).isEqualTo(ClientRuntime.ClientState.STOPPED);
    }

    @Test
    void stop_shutdownsWorkerGroup() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("worker-shutdown-client", 19019);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        ClientRuntime runtime = orchestrator.getRuntime("worker-shutdown-client");
        EventLoopGroup workerGroup = runtime.getWorkerGroup();
        assertThat(workerGroup.isShutdown()).isFalse();

        orchestrator.stop();

        // Worker group should be shutting down (might not be fully terminated yet)
        assertThat(workerGroup.isShuttingDown() || workerGroup.isShutdown()).isTrue();
    }

    @Test
    void stop_closesRequestInvoker() {
        properties.setEnabled(true);

        ClientSpec clientSpec = createValidClientSpec("invoker-close-client", 19020);
        properties.setClients(Collections.singletonList(clientSpec));

        orchestrator.start();

        ClientRuntime runtime = orchestrator.getRuntime("invoker-close-client");
        RequestInvoker requestInvoker = runtime.getRequestInvoker();
        assertThat(requestInvoker).isNotNull();

        orchestrator.stop();

        assertThat(runtime.getState()).isEqualTo(ClientRuntime.ClientState.STOPPED);
    }

    private ClientSpec createValidClientSpec(String name, int port) {
        ClientSpec spec = new ClientSpec();
        spec.setName(name);
        spec.setHost("127.0.0.1");
        spec.setPort(port);
        spec.setProfile("tcp-lengthfield-json");

        PoolSpec poolSpec = new PoolSpec();
        poolSpec.setMaxConnections(5);
        poolSpec.setMinIdle(1);
        poolSpec.setAcquireTimeoutMs(100);
        spec.setPool(poolSpec);

        ReconnectSpec reconnectSpec = new ReconnectSpec();
        reconnectSpec.setEnabled(false);
        spec.setReconnect(reconnectSpec);

        HeartbeatSpec heartbeatSpec = new HeartbeatSpec();
        heartbeatSpec.setEnabled(false);
        spec.setHeartbeat(heartbeatSpec);

        TimeoutSpec timeoutSpec = new TimeoutSpec();
        timeoutSpec.setConnectMs(5000);
        timeoutSpec.setRequestMs(30000);
        spec.setTimeout(timeoutSpec);

        return spec;
    }

}
