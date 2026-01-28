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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

}
