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
import com.childrengreens.netty.spring.boot.context.properties.PoolSpec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConnectionPool}.
 */
class ConnectionPoolTest {

    private ClientSpec clientSpec;
    private Bootstrap bootstrap;

    @BeforeEach
    void setUp() {
        clientSpec = new ClientSpec();
        clientSpec.setName("test-client");
        clientSpec.setHost("127.0.0.1");
        clientSpec.setPort(9000);

        PoolSpec poolSpec = new PoolSpec();
        poolSpec.setMaxConnections(5);
        poolSpec.setMinIdle(1);
        poolSpec.setAcquireTimeoutMs(100);
        clientSpec.setPool(poolSpec);

        bootstrap = mock(Bootstrap.class);
    }

    @Test
    void constructor_initializesCorrectly() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);

        assertThat(pool.getTotalConnections()).isEqualTo(0);
        assertThat(pool.getIdleConnections()).isEqualTo(0);

        pool.close();
    }

    @Test
    void release_withHealthyChannel_returnsToPool() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);
        EmbeddedChannel channel = new EmbeddedChannel();

        pool.release(channel);

        assertThat(pool.getIdleConnections()).isEqualTo(1);

        pool.close();
        channel.close();
    }

    @Test
    void release_withNullChannel_doesNothing() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);

        pool.release(null);

        assertThat(pool.getIdleConnections()).isEqualTo(0);

        pool.close();
    }

    @Test
    void release_withClosedChannel_doesNotReturnToPool() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.close();

        pool.release(channel);

        assertThat(pool.getIdleConnections()).isEqualTo(0);

        pool.close();
    }

    @Test
    void close_closesAllConnections() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);
        EmbeddedChannel channel1 = new EmbeddedChannel();
        EmbeddedChannel channel2 = new EmbeddedChannel();

        pool.release(channel1);
        pool.release(channel2);

        assertThat(pool.getIdleConnections()).isEqualTo(2);

        pool.close();

        assertThat(pool.getIdleConnections()).isEqualTo(0);
    }

    @Test
    void acquire_whenPoolClosed_throwsException() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);
        pool.close();

        assertThatThrownBy(pool::acquire)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Connection pool is closed");
    }

    @Test
    void setReconnectManager_setsManager() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);
        ReconnectManager reconnectManager = mock(ReconnectManager.class);

        pool.setReconnectManager(reconnectManager);

        pool.close();
    }

    @Test
    void release_withInactiveChannel_doesNotReturnToPool() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.close();

        pool.release(channel);

        assertThat(pool.getIdleConnections()).isEqualTo(0);

        pool.close();
    }

    @Test
    void getIdleConnections_afterRelease_increments() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);
        EmbeddedChannel channel = new EmbeddedChannel();

        pool.release(channel);

        // release adds to idle but doesn't increment totalConnections
        // totalConnections is only incremented by createChannel
        assertThat(pool.getIdleConnections()).isEqualTo(1);

        pool.close();
        channel.close();
    }

    @Test
    void close_calledTwice_doesNotThrow() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);

        pool.close();
        pool.close();

        // No exception thrown
    }

    @Test
    void release_afterClose_doesNotAddToPool() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);
        pool.close();

        EmbeddedChannel channel = new EmbeddedChannel();
        pool.release(channel);

        assertThat(pool.getIdleConnections()).isEqualTo(0);
        channel.close();
    }

}
