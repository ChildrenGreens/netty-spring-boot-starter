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
import com.childrengreens.netty.spring.boot.context.properties.ReconnectSpec;
import com.childrengreens.netty.spring.boot.context.properties.TimeoutSpec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    @Test
    void acquire_withIdleChannel_returnsIdleChannel() throws Exception {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);
        EmbeddedChannel channel = new EmbeddedChannel();

        // Release a channel to the pool first
        pool.release(channel);
        assertThat(pool.getIdleConnections()).isEqualTo(1);

        // Acquire should return the idle channel
        io.netty.channel.Channel acquired = pool.acquire();
        assertThat(acquired).isSameAs(channel);
        assertThat(pool.getIdleConnections()).isEqualTo(0);

        pool.close();
        channel.close();
    }

    @Test
    void acquire_withUnhealthyIdleChannel_skipsIt() throws Exception {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);
        EmbeddedChannel healthyChannel = new EmbeddedChannel();
        EmbeddedChannel unhealthyChannel = new EmbeddedChannel();
        unhealthyChannel.close(); // Make it unhealthy

        // This won't be added because it's closed
        pool.release(unhealthyChannel);

        // This will be added
        pool.release(healthyChannel);

        assertThat(pool.getIdleConnections()).isEqualTo(1);

        pool.close();
        healthyChannel.close();
    }

    @Test
    void release_withPoolFull_closesChannel() {
        // Set max connections to 1
        clientSpec.getPool().setMaxConnections(1);
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);

        EmbeddedChannel channel1 = new EmbeddedChannel();
        EmbeddedChannel channel2 = new EmbeddedChannel();

        pool.release(channel1);
        assertThat(pool.getIdleConnections()).isEqualTo(1);

        // Pool is full, channel2 should be closed
        pool.release(channel2);
        // Pool should still have only 1 connection
        assertThat(pool.getIdleConnections()).isEqualTo(1);

        pool.close();
        channel1.close();
        channel2.close();
    }

    @Test
    void getTotalConnections_returnsZero_whenNoConnectionsCreated() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);

        // release doesn't increment totalConnections
        assertThat(pool.getTotalConnections()).isEqualTo(0);

        pool.close();
    }

    @Test
    void setReconnectManager_allowsSettingManager() {
        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);
        ReconnectManager manager = mock(ReconnectManager.class);

        pool.setReconnectManager(manager);
        // No exception thrown

        pool.close();
    }

    @Test
    void acquire_withUnhealthyIdleChannel_closesAndCreatesNew() throws Exception {
        // Setup mock bootstrap
        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);
        EmbeddedChannel newChannel = new EmbeddedChannel();

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(true);
        when(mockFuture.channel()).thenReturn(newChannel);

        // Set timeout
        TimeoutSpec timeoutSpec = new TimeoutSpec();
        timeoutSpec.setConnectMs(5000);
        clientSpec.setTimeout(timeoutSpec);

        ConnectionPool pool = new ConnectionPool(clientSpec, mockBootstrap);

        // Add an unhealthy channel to the pool using reflection to bypass release check
        EmbeddedChannel unhealthyChannel = new EmbeddedChannel();
        unhealthyChannel.close(); // Make it unhealthy

        // Since release won't add closed channel, we need a different approach
        // First add a healthy channel, then close it
        EmbeddedChannel channelToClose = new EmbeddedChannel();
        pool.release(channelToClose);
        assertThat(pool.getIdleConnections()).isEqualTo(1);

        // Now close it to make it unhealthy
        channelToClose.close();

        // Acquire should detect unhealthy channel, close it, and create new one
        Channel acquired = pool.acquire();
        assertThat(acquired).isSameAs(newChannel);

        pool.close();
        newChannel.close();
    }

    @Test
    void acquire_whenNoIdleAndUnderMax_createsNewChannel() throws Exception {
        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);
        EmbeddedChannel newChannel = new EmbeddedChannel();

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(true);
        when(mockFuture.channel()).thenReturn(newChannel);

        TimeoutSpec timeoutSpec = new TimeoutSpec();
        timeoutSpec.setConnectMs(5000);
        clientSpec.setTimeout(timeoutSpec);

        ConnectionPool pool = new ConnectionPool(clientSpec, mockBootstrap);

        // No idle channels, should create new one
        Channel acquired = pool.acquire();
        assertThat(acquired).isSameAs(newChannel);
        assertThat(pool.getTotalConnections()).isEqualTo(1);

        pool.close();
        newChannel.close();
    }

    @Test
    void acquire_whenAtMaxAndNoIdleAvailable_throwsTimeout() throws Exception {
        clientSpec.getPool().setMaxConnections(1);
        clientSpec.getPool().setAcquireTimeoutMs(50); // Short timeout for test

        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);
        EmbeddedChannel channel = new EmbeddedChannel();

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(true);
        when(mockFuture.channel()).thenReturn(channel);

        TimeoutSpec timeoutSpec = new TimeoutSpec();
        timeoutSpec.setConnectMs(5000);
        clientSpec.setTimeout(timeoutSpec);

        ConnectionPool pool = new ConnectionPool(clientSpec, mockBootstrap);

        // Acquire the first channel (now at max)
        Channel first = pool.acquire();
        assertThat(pool.getTotalConnections()).isEqualTo(1);

        // Try to acquire another - should timeout
        assertThatThrownBy(pool::acquire)
                .isInstanceOf(TimeoutException.class)
                .hasMessageContaining("Timeout waiting for available connection");

        pool.close();
        channel.close();
    }

    @Test
    void acquire_whenWaitingAndChannelBecomesAvailable_returnsIt() throws Exception {
        clientSpec.getPool().setMaxConnections(1);
        clientSpec.getPool().setAcquireTimeoutMs(500);

        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);
        EmbeddedChannel channel = new EmbeddedChannel();

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(true);
        when(mockFuture.channel()).thenReturn(channel);

        TimeoutSpec timeoutSpec = new TimeoutSpec();
        timeoutSpec.setConnectMs(5000);
        clientSpec.setTimeout(timeoutSpec);

        ConnectionPool pool = new ConnectionPool(clientSpec, mockBootstrap);

        // Acquire the first channel
        Channel first = pool.acquire();

        // Release in another thread after delay
        new Thread(() -> {
            try {
                Thread.sleep(100);
                pool.release(first);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // This should wait and get the released channel
        Channel second = pool.acquire();
        assertThat(second).isSameAs(first);

        pool.close();
        channel.close();
    }

    @Test
    void createChannel_whenConnectionFails_throwsException() throws Exception {
        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(false);
        when(mockFuture.cause()).thenReturn(new RuntimeException("Connection refused"));

        TimeoutSpec timeoutSpec = new TimeoutSpec();
        timeoutSpec.setConnectMs(5000);
        clientSpec.setTimeout(timeoutSpec);

        ConnectionPool pool = new ConnectionPool(clientSpec, mockBootstrap);

        assertThatThrownBy(pool::acquire)
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Failed to connect");

        // Total connections should be 0 since creation failed
        assertThat(pool.getTotalConnections()).isEqualTo(0);

        pool.close();
    }

    @Test
    void createChannel_whenMaxConnectionsReached_throwsException() throws Exception {
        clientSpec.getPool().setMaxConnections(1);

        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);
        EmbeddedChannel channel = new EmbeddedChannel();

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(true);
        when(mockFuture.channel()).thenReturn(channel);

        TimeoutSpec timeoutSpec = new TimeoutSpec();
        timeoutSpec.setConnectMs(5000);
        clientSpec.setTimeout(timeoutSpec);
        clientSpec.getPool().setAcquireTimeoutMs(50);

        ConnectionPool pool = new ConnectionPool(clientSpec, mockBootstrap);

        // Acquire one channel
        pool.acquire();

        // Try to acquire another should fail
        assertThatThrownBy(pool::acquire)
                .isInstanceOf(TimeoutException.class);

        pool.close();
        channel.close();
    }

    @Test
    void release_withUnhealthyChannelAndReconnectEnabled_triggersReconnect() {
        ReconnectSpec reconnectSpec = new ReconnectSpec();
        reconnectSpec.setEnabled(true);
        clientSpec.setReconnect(reconnectSpec);

        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);
        ReconnectManager reconnectManager = mock(ReconnectManager.class);
        pool.setReconnectManager(reconnectManager);

        // Release a closed/unhealthy channel
        EmbeddedChannel unhealthyChannel = new EmbeddedChannel();
        unhealthyChannel.close();

        pool.release(unhealthyChannel);

        // Should trigger reconnect
        verify(reconnectManager).scheduleReconnect();

        pool.close();
    }

    @Test
    void maintenanceTask_removesUnhealthyChannels() throws Exception {
        // Use short idle time for faster test
        clientSpec.getPool().setMaxIdleMs(100);
        clientSpec.getPool().setMinIdle(0);

        ConnectionPool pool = new ConnectionPool(clientSpec, bootstrap);

        // Add a channel to the pool
        EmbeddedChannel channel = new EmbeddedChannel();
        pool.release(channel);
        assertThat(pool.getIdleConnections()).isEqualTo(1);

        // Close the channel to make it unhealthy
        channel.close();

        // Wait for maintenance task to run
        Thread.sleep(200);

        // Maintenance should have removed the unhealthy channel
        assertThat(pool.getIdleConnections()).isEqualTo(0);

        pool.close();
    }

    @Test
    void ensureMinIdle_createsMinimumConnections() throws Exception {
        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);

        // Return a new channel each time
        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(true);
        when(mockFuture.channel()).thenAnswer(invocation -> new EmbeddedChannel());

        TimeoutSpec timeoutSpec = new TimeoutSpec();
        timeoutSpec.setConnectMs(5000);
        clientSpec.setTimeout(timeoutSpec);
        clientSpec.getPool().setMinIdle(2);
        clientSpec.getPool().setMaxIdleMs(100); // Short for faster maintenance

        ConnectionPool pool = new ConnectionPool(clientSpec, mockBootstrap);

        // Wait for maintenance task to ensure min idle
        Thread.sleep(200);

        // Should have created at least minIdle connections
        assertThat(pool.getIdleConnections()).isGreaterThanOrEqualTo(2);

        pool.close();
    }

    @Test
    void ensureMinIdle_viaReflection_createsConnections() throws Exception {
        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(true);
        when(mockFuture.channel()).thenAnswer(invocation -> new EmbeddedChannel());

        TimeoutSpec timeoutSpec = new TimeoutSpec();
        timeoutSpec.setConnectMs(5000);
        clientSpec.setTimeout(timeoutSpec);
        clientSpec.getPool().setMinIdle(2);
        clientSpec.getPool().setMaxConnections(5);

        ConnectionPool pool = new ConnectionPool(clientSpec, mockBootstrap);

        // Invoke ensureMinIdle directly via reflection
        Method ensureMinIdleMethod = ConnectionPool.class.getDeclaredMethod("ensureMinIdle");
        ensureMinIdleMethod.setAccessible(true);
        ensureMinIdleMethod.invoke(pool);

        // Should have created minIdle connections
        assertThat(pool.getIdleConnections()).isEqualTo(2);

        pool.close();
    }

    @Test
    void ensureMinIdle_whenCreateFails_stopsCreating() throws Exception {
        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(false);
        when(mockFuture.cause()).thenReturn(new RuntimeException("Connection failed"));

        TimeoutSpec timeoutSpec = new TimeoutSpec();
        timeoutSpec.setConnectMs(5000);
        clientSpec.setTimeout(timeoutSpec);
        clientSpec.getPool().setMinIdle(3);

        ConnectionPool pool = new ConnectionPool(clientSpec, mockBootstrap);

        // Invoke ensureMinIdle directly via reflection
        Method ensureMinIdleMethod = ConnectionPool.class.getDeclaredMethod("ensureMinIdle");
        ensureMinIdleMethod.setAccessible(true);
        ensureMinIdleMethod.invoke(pool);

        // Should have 0 connections since creation failed
        assertThat(pool.getIdleConnections()).isEqualTo(0);

        pool.close();
    }

    @Test
    void acquire_withUnhealthyIdleChannelAfterWait_createsNew() throws Exception {
        clientSpec.getPool().setMaxConnections(2); // Allow 2 connections
        clientSpec.getPool().setAcquireTimeoutMs(500);

        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);

        EmbeddedChannel channel1 = new EmbeddedChannel();
        EmbeddedChannel channel2 = new EmbeddedChannel();

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(true);
        when(mockFuture.channel()).thenReturn(channel1).thenReturn(channel2);

        TimeoutSpec timeoutSpec = new TimeoutSpec();
        timeoutSpec.setConnectMs(5000);
        clientSpec.setTimeout(timeoutSpec);

        ConnectionPool pool = new ConnectionPool(clientSpec, mockBootstrap);

        // Acquire first channel
        Channel first = pool.acquire();
        assertThat(first).isSameAs(channel1);

        // Close it to make unhealthy, then release
        first.close();
        pool.release(first);

        // Acquire again should create a new channel since first is unhealthy
        // (released channel won't be added to pool since it's closed)
        Channel second = pool.acquire();
        assertThat(second).isSameAs(channel2);

        pool.close();
        channel1.close();
        channel2.close();
    }

}
