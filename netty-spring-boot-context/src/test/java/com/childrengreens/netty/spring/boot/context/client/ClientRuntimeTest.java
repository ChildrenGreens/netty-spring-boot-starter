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

import com.childrengreens.netty.spring.boot.context.properties.ClientSpec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClientRuntime}.
 */
class ClientRuntimeTest {

    @Test
    void constructor_setsAllProperties() {
        ClientSpec spec = new ClientSpec();
        spec.setName("test-client");
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);
        ConnectionPool connectionPool = mock(ConnectionPool.class);
        ReconnectManager reconnectManager = mock(ReconnectManager.class);
        HeartbeatManager heartbeatManager = mock(HeartbeatManager.class);
        RequestInvoker requestInvoker = mock(RequestInvoker.class);

        ClientRuntime runtime = new ClientRuntime(spec, bootstrap, workerGroup,
                connectionPool, reconnectManager, heartbeatManager, requestInvoker);

        assertThat(runtime.getClientSpec()).isSameAs(spec);
        assertThat(runtime.getBootstrap()).isSameAs(bootstrap);
        assertThat(runtime.getWorkerGroup()).isSameAs(workerGroup);
        assertThat(runtime.getConnectionPool()).isSameAs(connectionPool);
        assertThat(runtime.getReconnectManager()).isSameAs(reconnectManager);
        assertThat(runtime.getHeartbeatManager()).isSameAs(heartbeatManager);
        assertThat(runtime.getRequestInvoker()).isSameAs(requestInvoker);

        workerGroup.shutdownGracefully();
    }

    @Test
    void getName_returnsClientSpecName() {
        ClientSpec spec = new ClientSpec();
        spec.setName("my-client");
        ClientRuntime runtime = createRuntimeWithSpec(spec);

        assertThat(runtime.getName()).isEqualTo("my-client");
    }

    @Test
    void state_defaultsToCreated() {
        ClientSpec spec = new ClientSpec();
        ClientRuntime runtime = createRuntimeWithSpec(spec);

        assertThat(runtime.getState()).isEqualTo(ClientRuntime.ClientState.CREATED);
    }

    @Test
    void setState_changesState() {
        ClientSpec spec = new ClientSpec();
        ClientRuntime runtime = createRuntimeWithSpec(spec);

        runtime.setState(ClientRuntime.ClientState.RUNNING);
        assertThat(runtime.getState()).isEqualTo(ClientRuntime.ClientState.RUNNING);

        runtime.setState(ClientRuntime.ClientState.STOPPING);
        assertThat(runtime.getState()).isEqualTo(ClientRuntime.ClientState.STOPPING);

        runtime.setState(ClientRuntime.ClientState.STOPPED);
        assertThat(runtime.getState()).isEqualTo(ClientRuntime.ClientState.STOPPED);
    }

    @Test
    void clientState_hasAllExpectedValues() {
        assertThat(ClientRuntime.ClientState.values()).containsExactly(
                ClientRuntime.ClientState.CREATED,
                ClientRuntime.ClientState.STARTING,
                ClientRuntime.ClientState.RUNNING,
                ClientRuntime.ClientState.STOPPING,
                ClientRuntime.ClientState.STOPPED
        );
    }

    private ClientRuntime createRuntimeWithSpec(ClientSpec spec) {
        return new ClientRuntime(spec, null, null, null, null, null, null);
    }

}
