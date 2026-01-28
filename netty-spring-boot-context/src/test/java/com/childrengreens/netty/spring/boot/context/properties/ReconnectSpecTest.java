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
 * Tests for {@link ReconnectSpec}.
 */
class ReconnectSpecTest {

    @Test
    void defaultValues_areSetCorrectly() {
        ReconnectSpec spec = new ReconnectSpec();

        assertThat(spec.isEnabled()).isTrue();
        assertThat(spec.getInitialDelayMs()).isEqualTo(1000);
        assertThat(spec.getMaxDelayMs()).isEqualTo(30000);
        assertThat(spec.getMultiplier()).isEqualTo(2.0);
        assertThat(spec.getMaxRetries()).isEqualTo(-1);
    }

    @Test
    void settersAndGetters_workCorrectly() {
        ReconnectSpec spec = new ReconnectSpec();

        spec.setEnabled(false);
        spec.setInitialDelayMs(2000);
        spec.setMaxDelayMs(60000);
        spec.setMultiplier(1.5);
        spec.setMaxRetries(10);

        assertThat(spec.isEnabled()).isFalse();
        assertThat(spec.getInitialDelayMs()).isEqualTo(2000);
        assertThat(spec.getMaxDelayMs()).isEqualTo(60000);
        assertThat(spec.getMultiplier()).isEqualTo(1.5);
        assertThat(spec.getMaxRetries()).isEqualTo(10);
    }

    @Test
    void infiniteRetries_isIndicatedByMinusOne() {
        ReconnectSpec spec = new ReconnectSpec();

        assertThat(spec.getMaxRetries()).isEqualTo(-1);
    }

}
