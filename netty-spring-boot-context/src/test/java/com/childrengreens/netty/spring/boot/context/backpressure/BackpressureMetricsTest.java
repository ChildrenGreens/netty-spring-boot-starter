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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BackpressureMetrics}.
 */
class BackpressureMetricsTest {

    private BackpressureMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new BackpressureMetrics("test-server");
    }

    @Test
    void getServerName_returnsConfiguredName() {
        assertThat(metrics.getServerName()).isEqualTo("test-server");
    }

    @Test
    void initialCountsAreZero() {
        assertThat(metrics.getSuspendCount()).isZero();
        assertThat(metrics.getResumeCount()).isZero();
        assertThat(metrics.getDroppedCount()).isZero();
        assertThat(metrics.getDisconnectCount()).isZero();
    }

    @Test
    void incrementSuspend_incrementsCount() {
        metrics.incrementSuspend();
        metrics.incrementSuspend();
        metrics.incrementSuspend();

        assertThat(metrics.getSuspendCount()).isEqualTo(3);
    }

    @Test
    void incrementResume_incrementsCount() {
        metrics.incrementResume();
        metrics.incrementResume();

        assertThat(metrics.getResumeCount()).isEqualTo(2);
    }

    @Test
    void incrementDropped_incrementsCount() {
        metrics.incrementDropped();

        assertThat(metrics.getDroppedCount()).isEqualTo(1);
    }

    @Test
    void incrementDisconnect_incrementsCount() {
        metrics.incrementDisconnect();
        metrics.incrementDisconnect();
        metrics.incrementDisconnect();
        metrics.incrementDisconnect();

        assertThat(metrics.getDisconnectCount()).isEqualTo(4);
    }

    @Test
    void toString_containsAllFields() {
        metrics.incrementSuspend();
        metrics.incrementResume();
        metrics.incrementDropped();
        metrics.incrementDisconnect();

        String str = metrics.toString();

        assertThat(str).contains("test-server");
        assertThat(str).contains("suspendCount=1");
        assertThat(str).contains("resumeCount=1");
        assertThat(str).contains("droppedCount=1");
        assertThat(str).contains("disconnectCount=1");
    }
}
