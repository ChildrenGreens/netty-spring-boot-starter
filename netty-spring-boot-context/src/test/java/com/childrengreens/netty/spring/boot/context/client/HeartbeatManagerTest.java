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
import com.childrengreens.netty.spring.boot.context.properties.HeartbeatSpec;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link HeartbeatManager}.
 */
class HeartbeatManagerTest {

    private ClientSpec clientSpec;
    private ConnectionPool connectionPool;
    private RequestInvoker requestInvoker;
    private ScheduledExecutorService scheduler;
    private HeartbeatManager heartbeatManager;

    @BeforeEach
    void setUp() {
        clientSpec = new ClientSpec();
        clientSpec.setName("test-client");
        clientSpec.setHost("127.0.0.1");
        clientSpec.setPort(9000);

        HeartbeatSpec heartbeatSpec = new HeartbeatSpec();
        heartbeatSpec.setEnabled(true);
        heartbeatSpec.setIntervalMs(1000);
        heartbeatSpec.setTimeoutMs(500);
        heartbeatSpec.setMessage("{\"type\":\"heartbeat\"}");
        clientSpec.setHeartbeat(heartbeatSpec);

        connectionPool = mock(ConnectionPool.class);
        requestInvoker = mock(RequestInvoker.class);
        scheduler = Executors.newSingleThreadScheduledExecutor();

        heartbeatManager = new HeartbeatManager(clientSpec, connectionPool, requestInvoker, scheduler);
    }

    @AfterEach
    void tearDown() {
        heartbeatManager.stop();
        scheduler.shutdown();
    }

    @Test
    void isRunning_initiallyFalse() {
        assertThat(heartbeatManager.isRunning()).isFalse();
    }

    @Test
    void getConsecutiveFailures_initiallyZero() {
        assertThat(heartbeatManager.getConsecutiveFailures()).isEqualTo(0);
    }

    @Test
    void start_whenEnabled_startsRunning() {
        heartbeatManager.start();

        assertThat(heartbeatManager.isRunning()).isTrue();
    }

    @Test
    void start_whenDisabled_doesNotStart() {
        clientSpec.getHeartbeat().setEnabled(false);

        heartbeatManager.start();

        assertThat(heartbeatManager.isRunning()).isFalse();
    }

    @Test
    void start_whenAlreadyRunning_doesNothing() {
        heartbeatManager.start();
        assertThat(heartbeatManager.isRunning()).isTrue();

        // Start again should not throw
        heartbeatManager.start();
        assertThat(heartbeatManager.isRunning()).isTrue();
    }

    @Test
    void stop_whenRunning_stopsRunning() {
        heartbeatManager.start();
        assertThat(heartbeatManager.isRunning()).isTrue();

        heartbeatManager.stop();

        assertThat(heartbeatManager.isRunning()).isFalse();
    }

    @Test
    void stop_whenNotRunning_doesNothing() {
        assertThat(heartbeatManager.isRunning()).isFalse();

        heartbeatManager.stop();

        assertThat(heartbeatManager.isRunning()).isFalse();
    }

    @Test
    void setListener_setsListener() {
        HeartbeatManager.HeartbeatListener listener = mock(HeartbeatManager.HeartbeatListener.class);

        heartbeatManager.setListener(listener);
        // Verify no exception thrown
    }

    @Test
    void stop_calledTwice_doesNotThrow() {
        heartbeatManager.start();
        heartbeatManager.stop();
        heartbeatManager.stop();

        assertThat(heartbeatManager.isRunning()).isFalse();
    }

    @Test
    void start_calledTwice_onlyStartsOnce() {
        heartbeatManager.start();
        assertThat(heartbeatManager.isRunning()).isTrue();

        heartbeatManager.start();
        assertThat(heartbeatManager.isRunning()).isTrue();

        heartbeatManager.stop();
        assertThat(heartbeatManager.isRunning()).isFalse();
    }

    @Test
    void consecutiveFailures_remainsZeroInitially() {
        assertThat(heartbeatManager.getConsecutiveFailures()).isZero();
    }

    @Test
    void start_withDisabledHeartbeat_remainsNotRunning() {
        clientSpec.getHeartbeat().setEnabled(false);

        heartbeatManager.start();

        assertThat(heartbeatManager.isRunning()).isFalse();
        assertThat(heartbeatManager.getConsecutiveFailures()).isZero();
    }

    @Test
    void stop_beforeStart_noExceptionThrown() {
        assertThat(heartbeatManager.isRunning()).isFalse();

        heartbeatManager.stop();

        assertThat(heartbeatManager.isRunning()).isFalse();
    }

    @Test
    void sendHeartbeat_whenSuccessful_resetsConsecutiveFailures() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        when(connectionPool.acquire()).thenReturn(channel);

        CompletableFuture<Object> future = CompletableFuture.completedFuture("pong");
        when(requestInvoker.invoke(any(Channel.class), anyString(), any(), anyLong())).thenReturn(future);

        AtomicBoolean successCalled = new AtomicBoolean(false);
        heartbeatManager.setListener(new HeartbeatManager.HeartbeatListener() {
            @Override
            public void onHeartbeatSuccess() {
                successCalled.set(true);
            }

            @Override
            public void onHeartbeatFailure(Throwable cause) {
            }

            @Override
            public void onConnectionUnhealthy() {
            }
        });

        clientSpec.getHeartbeat().setIntervalMs(100);
        heartbeatManager.start();

        // Wait for heartbeat to be sent
        Thread.sleep(200);

        assertThat(successCalled.get()).isTrue();
        assertThat(heartbeatManager.getConsecutiveFailures()).isEqualTo(0);

        verify(connectionPool, atLeastOnce()).release(channel);

        heartbeatManager.stop();
        channel.close();
    }

    @Test
    void sendHeartbeat_whenFailed_incrementsConsecutiveFailures() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        when(connectionPool.acquire()).thenReturn(channel);

        CompletableFuture<Object> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Heartbeat failed"));
        when(requestInvoker.invoke(any(Channel.class), anyString(), any(), anyLong())).thenReturn(future);

        AtomicBoolean failureCalled = new AtomicBoolean(false);
        heartbeatManager.setListener(new HeartbeatManager.HeartbeatListener() {
            @Override
            public void onHeartbeatSuccess() {
            }

            @Override
            public void onHeartbeatFailure(Throwable cause) {
                failureCalled.set(true);
            }

            @Override
            public void onConnectionUnhealthy() {
            }
        });

        clientSpec.getHeartbeat().setIntervalMs(100);
        heartbeatManager.start();

        // Wait for heartbeat to be sent
        Thread.sleep(200);

        assertThat(failureCalled.get()).isTrue();

        heartbeatManager.stop();
        channel.close();
    }

    @Test
    void sendHeartbeat_whenThreeConsecutiveFailures_triggersUnhealthy() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        when(connectionPool.acquire()).thenReturn(channel);

        CompletableFuture<Object> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Heartbeat failed"));
        when(requestInvoker.invoke(any(Channel.class), anyString(), any(), anyLong())).thenReturn(future);

        AtomicBoolean unhealthyCalled = new AtomicBoolean(false);
        AtomicInteger failureCount = new AtomicInteger(0);
        heartbeatManager.setListener(new HeartbeatManager.HeartbeatListener() {
            @Override
            public void onHeartbeatSuccess() {
            }

            @Override
            public void onHeartbeatFailure(Throwable cause) {
                failureCount.incrementAndGet();
            }

            @Override
            public void onConnectionUnhealthy() {
                unhealthyCalled.set(true);
            }
        });

        clientSpec.getHeartbeat().setIntervalMs(50);
        heartbeatManager.start();

        // Wait for 3+ heartbeats
        Thread.sleep(300);

        assertThat(failureCount.get()).isGreaterThanOrEqualTo(3);
        assertThat(unhealthyCalled.get()).isTrue();

        heartbeatManager.stop();
        channel.close();
    }

    @Test
    void sendHeartbeat_whenNotRunning_doesNotSend() throws Exception {
        // Don't start the heartbeat manager
        assertThat(heartbeatManager.isRunning()).isFalse();

        // Wait a bit
        Thread.sleep(200);

        // Should not have attempted any heartbeats
        verify(connectionPool, never()).acquire();
        verify(requestInvoker, never()).invoke(any(), anyString(), any(), anyLong());
    }

    @Test
    void sendHeartbeat_releasesChannelAfterCompletion() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        when(connectionPool.acquire()).thenReturn(channel);

        CompletableFuture<Object> future = CompletableFuture.completedFuture("pong");
        when(requestInvoker.invoke(any(Channel.class), anyString(), any(), anyLong())).thenReturn(future);

        clientSpec.getHeartbeat().setIntervalMs(100);
        heartbeatManager.start();

        Thread.sleep(200);

        verify(connectionPool, atLeastOnce()).release(channel);

        heartbeatManager.stop();
        channel.close();
    }

    @Test
    void sendHeartbeat_releasesChannelEvenOnFailure() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        when(connectionPool.acquire()).thenReturn(channel);

        CompletableFuture<Object> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Failed"));
        when(requestInvoker.invoke(any(Channel.class), anyString(), any(), anyLong())).thenReturn(future);

        clientSpec.getHeartbeat().setIntervalMs(100);
        heartbeatManager.start();

        Thread.sleep(200);

        verify(connectionPool, atLeastOnce()).release(channel);

        heartbeatManager.stop();
        channel.close();
    }

    @Test
    void sendHeartbeat_withAcquireException_handlesGracefully() throws Exception {
        when(connectionPool.acquire()).thenThrow(new IllegalStateException("Pool closed"));

        AtomicBoolean failureCalled = new AtomicBoolean(false);
        heartbeatManager.setListener(new HeartbeatManager.HeartbeatListener() {
            @Override
            public void onHeartbeatSuccess() {
            }

            @Override
            public void onHeartbeatFailure(Throwable cause) {
                failureCalled.set(true);
            }

            @Override
            public void onConnectionUnhealthy() {
            }
        });

        clientSpec.getHeartbeat().setIntervalMs(100);
        heartbeatManager.start();

        Thread.sleep(200);

        assertThat(failureCalled.get()).isTrue();

        heartbeatManager.stop();
    }

    @Test
    void sendHeartbeat_successAfterFailures_resetsCounter() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        when(connectionPool.acquire()).thenReturn(channel);

        // First 2 calls fail, then succeed
        CompletableFuture<Object> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Failed"));

        CompletableFuture<Object> successFuture = CompletableFuture.completedFuture("pong");

        when(requestInvoker.invoke(any(Channel.class), anyString(), any(), anyLong()))
                .thenReturn(failedFuture)
                .thenReturn(failedFuture)
                .thenReturn(successFuture);

        AtomicInteger successCount = new AtomicInteger(0);
        heartbeatManager.setListener(new HeartbeatManager.HeartbeatListener() {
            @Override
            public void onHeartbeatSuccess() {
                successCount.incrementAndGet();
            }

            @Override
            public void onHeartbeatFailure(Throwable cause) {
            }

            @Override
            public void onConnectionUnhealthy() {
            }
        });

        clientSpec.getHeartbeat().setIntervalMs(50);
        heartbeatManager.start();

        Thread.sleep(300);

        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
        assertThat(heartbeatManager.getConsecutiveFailures()).isEqualTo(0);

        heartbeatManager.stop();
        channel.close();
    }

}
