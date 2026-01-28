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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

}
