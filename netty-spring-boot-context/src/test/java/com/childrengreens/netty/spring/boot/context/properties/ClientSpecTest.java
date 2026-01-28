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

package com.childrengreens.netty.spring.boot.context.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClientSpec}.
 */
class ClientSpecTest {

    @Test
    void defaultValues_areSetCorrectly() {
        ClientSpec spec = new ClientSpec();

        assertThat(spec.getHost()).isEqualTo("127.0.0.1");
        assertThat(spec.getPort()).isEqualTo(0);
        assertThat(spec.getPool()).isNotNull();
        assertThat(spec.getReconnect()).isNotNull();
        assertThat(spec.getHeartbeat()).isNotNull();
        assertThat(spec.getTimeout()).isNotNull();
        assertThat(spec.getFeatures()).isNotNull();
    }

    @Test
    void settersAndGetters_workCorrectly() {
        ClientSpec spec = new ClientSpec();

        spec.setName("test-client");
        spec.setHost("192.168.1.1");
        spec.setPort(9000);
        spec.setProfile("tcp-lengthfield-json");

        assertThat(spec.getName()).isEqualTo("test-client");
        assertThat(spec.getHost()).isEqualTo("192.168.1.1");
        assertThat(spec.getPort()).isEqualTo(9000);
        assertThat(spec.getProfile()).isEqualTo("tcp-lengthfield-json");
    }

    @Test
    void nestedSpecs_canBeSet() {
        ClientSpec spec = new ClientSpec();

        PoolSpec pool = new PoolSpec();
        pool.setMaxConnections(20);
        spec.setPool(pool);

        ReconnectSpec reconnect = new ReconnectSpec();
        reconnect.setEnabled(false);
        spec.setReconnect(reconnect);

        HeartbeatSpec heartbeat = new HeartbeatSpec();
        heartbeat.setIntervalMs(60000);
        spec.setHeartbeat(heartbeat);

        TimeoutSpec timeout = new TimeoutSpec();
        timeout.setConnectMs(10000);
        spec.setTimeout(timeout);

        assertThat(spec.getPool().getMaxConnections()).isEqualTo(20);
        assertThat(spec.getReconnect().isEnabled()).isFalse();
        assertThat(spec.getHeartbeat().getIntervalMs()).isEqualTo(60000);
        assertThat(spec.getTimeout().getConnectMs()).isEqualTo(10000);
    }

}
