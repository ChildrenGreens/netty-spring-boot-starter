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
 * Tests for {@link HeartbeatSpec}.
 */
class HeartbeatSpecTest {

    @Test
    void defaultValues_areSetCorrectly() {
        HeartbeatSpec spec = new HeartbeatSpec();

        assertThat(spec.isEnabled()).isTrue();
        assertThat(spec.getIntervalMs()).isEqualTo(30000);
        assertThat(spec.getTimeoutMs()).isEqualTo(5000);
        assertThat(spec.getMessage()).isEqualTo("{\"type\":\"heartbeat\"}");
        assertThat(spec.getResponseType()).isEqualTo("heartbeat");
    }

    @Test
    void settersAndGetters_workCorrectly() {
        HeartbeatSpec spec = new HeartbeatSpec();

        spec.setEnabled(false);
        spec.setIntervalMs(60000);
        spec.setTimeoutMs(10000);
        spec.setMessage("{\"type\":\"ping\"}");
        spec.setResponseType("pong");

        assertThat(spec.isEnabled()).isFalse();
        assertThat(spec.getIntervalMs()).isEqualTo(60000);
        assertThat(spec.getTimeoutMs()).isEqualTo(10000);
        assertThat(spec.getMessage()).isEqualTo("{\"type\":\"ping\"}");
        assertThat(spec.getResponseType()).isEqualTo("pong");
    }

}
