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
 * Tests for {@link ShutdownSpec}.
 */
class ShutdownSpecTest {

    @Test
    void defaultValues() {
        ShutdownSpec spec = new ShutdownSpec();

        assertThat(spec.isGraceful()).isTrue();
        assertThat(spec.getQuietPeriodMs()).isEqualTo(200);
        assertThat(spec.getTimeoutMs()).isEqualTo(3000);
    }

    @Test
    void setGraceful() {
        ShutdownSpec spec = new ShutdownSpec();

        spec.setGraceful(false);

        assertThat(spec.isGraceful()).isFalse();
    }

    @Test
    void setQuietPeriodMs() {
        ShutdownSpec spec = new ShutdownSpec();

        spec.setQuietPeriodMs(500);

        assertThat(spec.getQuietPeriodMs()).isEqualTo(500);
    }

    @Test
    void setTimeoutMs() {
        ShutdownSpec spec = new ShutdownSpec();

        spec.setTimeoutMs(5000);

        assertThat(spec.getTimeoutMs()).isEqualTo(5000);
    }

}
