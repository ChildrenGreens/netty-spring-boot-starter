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

package com.childrengreens.netty.spring.boot.context.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BackpressureSpec}.
 */
class BackpressureSpecTest {

    @Test
    void defaultValues() {
        BackpressureSpec spec = new BackpressureSpec();

        assertThat(spec.isEnabled()).isFalse();
        assertThat(spec.getHighWaterMark()).isEqualTo(64 * 1024); // 64KB
        assertThat(spec.getLowWaterMark()).isEqualTo(32 * 1024); // 32KB
        assertThat(spec.getStrategy()).isEqualTo(BackpressureStrategy.SUSPEND_READ);
        assertThat(spec.getDropPolicy()).isEqualTo(DropPolicy.NEWEST);
        assertThat(spec.getOverflowThreshold()).isEqualTo(10 * 1024 * 1024); // 10MB
        assertThat(spec.isMetrics()).isTrue(); // Metrics enabled by default
    }

    @Test
    void setEnabled_updatesValue() {
        BackpressureSpec spec = new BackpressureSpec();
        spec.setEnabled(true);

        assertThat(spec.isEnabled()).isTrue();
    }

    @Test
    void setHighWaterMark_updatesValue() {
        BackpressureSpec spec = new BackpressureSpec();
        spec.setHighWaterMark(128 * 1024);

        assertThat(spec.getHighWaterMark()).isEqualTo(128 * 1024);
    }

    @Test
    void setLowWaterMark_updatesValue() {
        BackpressureSpec spec = new BackpressureSpec();
        spec.setLowWaterMark(16 * 1024);

        assertThat(spec.getLowWaterMark()).isEqualTo(16 * 1024);
    }

    @Test
    void setStrategy_updatesValue() {
        BackpressureSpec spec = new BackpressureSpec();
        spec.setStrategy(BackpressureStrategy.DROP);

        assertThat(spec.getStrategy()).isEqualTo(BackpressureStrategy.DROP);
    }

    @Test
    void setDropPolicy_updatesValue() {
        BackpressureSpec spec = new BackpressureSpec();
        spec.setDropPolicy(DropPolicy.OLDEST);

        assertThat(spec.getDropPolicy()).isEqualTo(DropPolicy.OLDEST);
    }

    @Test
    void setOverflowThreshold_updatesValue() {
        BackpressureSpec spec = new BackpressureSpec();
        spec.setOverflowThreshold(5 * 1024 * 1024);

        assertThat(spec.getOverflowThreshold()).isEqualTo(5 * 1024 * 1024);
    }

    @Test
    void setMetrics_updatesValue() {
        BackpressureSpec spec = new BackpressureSpec();
        spec.setMetrics(true);

        assertThat(spec.isMetrics()).isTrue();
    }
}
