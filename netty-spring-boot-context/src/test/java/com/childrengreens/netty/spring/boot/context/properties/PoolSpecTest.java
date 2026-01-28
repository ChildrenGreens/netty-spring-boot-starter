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
 * Tests for {@link PoolSpec}.
 */
class PoolSpecTest {

    @Test
    void defaultValues_areSetCorrectly() {
        PoolSpec spec = new PoolSpec();

        assertThat(spec.getMaxConnections()).isEqualTo(10);
        assertThat(spec.getMinIdle()).isEqualTo(2);
        assertThat(spec.getMaxIdleMs()).isEqualTo(60000);
        assertThat(spec.getAcquireTimeoutMs()).isEqualTo(5000);
        assertThat(spec.isTestOnBorrow()).isTrue();
    }

    @Test
    void settersAndGetters_workCorrectly() {
        PoolSpec spec = new PoolSpec();

        spec.setMaxConnections(20);
        spec.setMinIdle(5);
        spec.setMaxIdleMs(120000);
        spec.setAcquireTimeoutMs(10000);
        spec.setTestOnBorrow(false);

        assertThat(spec.getMaxConnections()).isEqualTo(20);
        assertThat(spec.getMinIdle()).isEqualTo(5);
        assertThat(spec.getMaxIdleMs()).isEqualTo(120000);
        assertThat(spec.getAcquireTimeoutMs()).isEqualTo(10000);
        assertThat(spec.isTestOnBorrow()).isFalse();
    }

}
