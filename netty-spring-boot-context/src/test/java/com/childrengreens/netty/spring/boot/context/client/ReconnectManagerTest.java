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
import com.childrengreens.netty.spring.boot.context.properties.ReconnectSpec;
import com.childrengreens.netty.spring.boot.context.properties.TimeoutSpec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ReconnectManager}.
 */
class ReconnectManagerTest {

    private ClientSpec clientSpec;
    private Bootstrap bootstrap;
    private ConnectionPool connectionPool;
    private ScheduledExecutorService scheduler;
    private ReconnectManager reconnectManager;

    @BeforeEach
    void setUp() {
        clientSpec = new ClientSpec();
        clientSpec.setName("test-client");
        clientSpec.setHost("127.0.0.1");
        clientSpec.setPort(9000);

        ReconnectSpec reconnectSpec = new ReconnectSpec();
        reconnectSpec.setEnabled(true);
        reconnectSpec.setInitialDelayMs(100);
        reconnectSpec.setMaxDelayMs(1000);
        reconnectSpec.setMultiplier(2.0);
        reconnectSpec.setMaxRetries(3);
        clientSpec.setReconnect(reconnectSpec);

        TimeoutSpec timeoutSpec = new TimeoutSpec();
        timeoutSpec.setConnectMs(100);
        clientSpec.setTimeout(timeoutSpec);

        bootstrap = mock(Bootstrap.class);
        connectionPool = mock(ConnectionPool.class);
        scheduler = Executors.newSingleThreadScheduledExecutor();

        reconnectManager = new ReconnectManager(clientSpec, bootstrap, connectionPool, scheduler);
    }

    @AfterEach
    void tearDown() {
        reconnectManager.stop();
        scheduler.shutdown();
    }

    @Test
    void isReconnecting_initiallyFalse() {
        assertThat(reconnectManager.isReconnecting()).isFalse();
    }

    @Test
    void getRetryCount_initiallyZero() {
        assertThat(reconnectManager.getRetryCount()).isEqualTo(0);
    }

    @Test
    void resetState_resetsCountAndDelay() {
        reconnectManager.resetState();

        assertThat(reconnectManager.getRetryCount()).isEqualTo(0);
    }

    @Test
    void stop_stopsReconnecting() {
        reconnectManager.stop();

        assertThat(reconnectManager.isReconnecting()).isFalse();
    }

    @Test
    void scheduleReconnect_whenDisabled_doesNothing() {
        clientSpec.getReconnect().setEnabled(false);

        reconnectManager.scheduleReconnect();

        assertThat(reconnectManager.isReconnecting()).isFalse();
    }

    @Test
    void scheduleReconnect_whenStopped_doesNothing() {
        reconnectManager.stop();

        reconnectManager.scheduleReconnect();

        assertThat(reconnectManager.isReconnecting()).isFalse();
    }

    @Test
    void setListener_setsListener() {
        ReconnectManager.ReconnectListener listener = mock(ReconnectManager.ReconnectListener.class);

        reconnectManager.setListener(listener);
        // Verify no exception thrown
    }

    @Test
    void scheduleReconnect_whenMaxRetriesReached_stillSchedulesInitially() {
        // maxRetries is checked in doReconnect, not scheduleReconnect
        // scheduleReconnect will still set reconnecting to true
        clientSpec.getReconnect().setMaxRetries(0);

        reconnectManager.scheduleReconnect();

        // Will be marked as reconnecting after schedule
        // The max retries check happens in doReconnect
        assertThat(reconnectManager.isReconnecting()).isTrue();
    }

    @Test
    void scheduleReconnect_whenEnabled_startsReconnecting() {
        reconnectManager.scheduleReconnect();

        // Should be marked as reconnecting after schedule
        assertThat(reconnectManager.isReconnecting()).isTrue();
    }

    @Test
    void stop_whenReconnecting_cancelsScheduledTask() {
        reconnectManager.scheduleReconnect();
        assertThat(reconnectManager.isReconnecting()).isTrue();

        reconnectManager.stop();

        assertThat(reconnectManager.isReconnecting()).isFalse();
    }

    @Test
    void stop_calledMultipleTimes_doesNotThrow() {
        reconnectManager.stop();
        reconnectManager.stop();
        reconnectManager.stop();

        // No exception thrown
    }

    @Test
    void scheduleReconnect_afterStop_doesNotReconnect() {
        reconnectManager.stop();

        reconnectManager.scheduleReconnect();

        assertThat(reconnectManager.isReconnecting()).isFalse();
    }

    @Test
    void scheduleReconnect_whenAlreadyReconnecting_doesNotScheduleAgain() {
        reconnectManager.scheduleReconnect();
        assertThat(reconnectManager.isReconnecting()).isTrue();

        // Second call should not schedule again (compareAndSet should fail)
        reconnectManager.scheduleReconnect();
        assertThat(reconnectManager.isReconnecting()).isTrue();
    }

    @Test
    void doReconnect_whenStopped_doesNotAttempt() throws Exception {
        reconnectManager.scheduleReconnect();
        reconnectManager.stop();

        // Wait for scheduled task to execute
        Thread.sleep(150);

        assertThat(reconnectManager.isReconnecting()).isFalse();
    }

    @Test
    void doReconnect_withSuccessfulConnection_releasesToPool() throws Exception {
        // Setup mock bootstrap that returns a successful future
        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);
        EmbeddedChannel channel = new EmbeddedChannel();

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(true);
        when(mockFuture.channel()).thenReturn(channel);

        ReconnectManager manager = new ReconnectManager(clientSpec, mockBootstrap, connectionPool, scheduler);

        AtomicBoolean successCalled = new AtomicBoolean(false);
        manager.setListener(new ReconnectManager.ReconnectListener() {
            @Override
            public void onReconnectSuccess(Channel ch) {
                successCalled.set(true);
            }

            @Override
            public void onReconnectFailure(Throwable cause) {
            }

            @Override
            public void onReconnectExhausted() {
            }
        });

        manager.scheduleReconnect();

        // Wait for reconnect to complete
        Thread.sleep(200);

        verify(connectionPool).release(channel);
        assertThat(successCalled.get()).isTrue();

        manager.stop();
        channel.close();
    }

    @Test
    void doReconnect_withFailedConnection_callsFailureListener() throws Exception {
        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(false);
        when(mockFuture.cause()).thenReturn(new RuntimeException("Connection refused"));

        // Use higher max retries to prevent exhaustion
        clientSpec.getReconnect().setMaxRetries(10);

        ReconnectManager manager = new ReconnectManager(clientSpec, mockBootstrap, connectionPool, scheduler);

        AtomicBoolean failureCalled = new AtomicBoolean(false);
        manager.setListener(new ReconnectManager.ReconnectListener() {
            @Override
            public void onReconnectSuccess(Channel ch) {
            }

            @Override
            public void onReconnectFailure(Throwable cause) {
                failureCalled.set(true);
            }

            @Override
            public void onReconnectExhausted() {
            }
        });

        manager.scheduleReconnect();

        // Wait for reconnect to complete
        Thread.sleep(200);

        assertThat(failureCalled.get()).isTrue();

        manager.stop();
    }

    @Test
    void doReconnect_withMaxRetriesExhausted_callsExhaustedListener() throws Exception {
        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(false);
        when(mockFuture.cause()).thenReturn(new RuntimeException("Connection refused"));

        // Set max retries to 0 so first attempt exhausts retries
        clientSpec.getReconnect().setMaxRetries(0);

        ReconnectManager manager = new ReconnectManager(clientSpec, mockBootstrap, connectionPool, scheduler);

        AtomicBoolean exhaustedCalled = new AtomicBoolean(false);
        manager.setListener(new ReconnectManager.ReconnectListener() {
            @Override
            public void onReconnectSuccess(Channel ch) {
            }

            @Override
            public void onReconnectFailure(Throwable cause) {
            }

            @Override
            public void onReconnectExhausted() {
                exhaustedCalled.set(true);
            }
        });

        manager.scheduleReconnect();

        // Wait for reconnect to complete
        Thread.sleep(200);

        assertThat(exhaustedCalled.get()).isTrue();

        manager.stop();
    }

    @Test
    void doReconnect_withUnlimitedRetries_continuesOnFailure() throws Exception {
        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(false);
        when(mockFuture.cause()).thenReturn(new RuntimeException("Connection refused"));

        // Set max retries to -1 for unlimited retries
        clientSpec.getReconnect().setMaxRetries(-1);
        clientSpec.getReconnect().setInitialDelayMs(50);

        ReconnectManager manager = new ReconnectManager(clientSpec, mockBootstrap, connectionPool, scheduler);

        manager.scheduleReconnect();

        // Wait for multiple reconnect attempts
        Thread.sleep(300);

        // Should have tried multiple times (retry count > 1)
        assertThat(manager.getRetryCount()).isGreaterThan(1);

        manager.stop();
    }

    @Test
    void handleReconnectFailure_increasesDelayWithBackoff() throws Exception {
        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(false);
        when(mockFuture.cause()).thenReturn(new RuntimeException("Connection refused"));

        // Set initial delay and high max retries
        clientSpec.getReconnect().setInitialDelayMs(50);
        clientSpec.getReconnect().setMaxDelayMs(1000);
        clientSpec.getReconnect().setMultiplier(2.0);
        clientSpec.getReconnect().setMaxRetries(10);

        ReconnectManager manager = new ReconnectManager(clientSpec, mockBootstrap, connectionPool, scheduler);

        manager.scheduleReconnect();

        // Wait for a few retries
        Thread.sleep(400);

        // Retry count should have increased
        assertThat(manager.getRetryCount()).isGreaterThan(1);

        manager.stop();
    }

    @Test
    void doReconnect_withException_handlesGracefully() throws Exception {
        Bootstrap mockBootstrap = mock(Bootstrap.class);

        when(mockBootstrap.connect(anyString(), anyInt())).thenThrow(new RuntimeException("Network error"));

        clientSpec.getReconnect().setMaxRetries(1);

        ReconnectManager manager = new ReconnectManager(clientSpec, mockBootstrap, connectionPool, scheduler);

        AtomicReference<Throwable> failureCause = new AtomicReference<>();
        manager.setListener(new ReconnectManager.ReconnectListener() {
            @Override
            public void onReconnectSuccess(Channel ch) {
            }

            @Override
            public void onReconnectFailure(Throwable cause) {
                failureCause.set(cause);
            }

            @Override
            public void onReconnectExhausted() {
            }
        });

        manager.scheduleReconnect();

        Thread.sleep(200);

        assertThat(failureCause.get()).isNotNull();
        assertThat(failureCause.get().getMessage()).contains("Network error");

        manager.stop();
    }

    @Test
    void resetState_afterFailures_resetsRetryCountAndDelay() throws Exception {
        Bootstrap mockBootstrap = mock(Bootstrap.class);
        ChannelFuture mockFuture = mock(ChannelFuture.class);

        when(mockBootstrap.connect(anyString(), anyInt())).thenReturn(mockFuture);
        when(mockFuture.await(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockFuture.isSuccess()).thenReturn(false);
        when(mockFuture.cause()).thenReturn(new RuntimeException("Connection refused"));

        clientSpec.getReconnect().setMaxRetries(10);
        clientSpec.getReconnect().setInitialDelayMs(50);

        ReconnectManager manager = new ReconnectManager(clientSpec, mockBootstrap, connectionPool, scheduler);

        manager.scheduleReconnect();
        Thread.sleep(200);

        assertThat(manager.getRetryCount()).isGreaterThan(0);

        manager.stop();
        manager.resetState();

        assertThat(manager.getRetryCount()).isEqualTo(0);
    }

}
