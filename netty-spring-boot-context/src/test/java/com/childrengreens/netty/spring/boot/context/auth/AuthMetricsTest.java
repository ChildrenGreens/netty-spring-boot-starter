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

package com.childrengreens.netty.spring.boot.context.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AuthMetrics}.
 */
class AuthMetricsTest {

    @Test
    void constructor_setsServerName() {
        AuthMetrics metrics = new AuthMetrics("test-server");

        assertThat(metrics.getServerName()).isEqualTo("test-server");
    }

    @Test
    void initialCountsAreZero() {
        AuthMetrics metrics = new AuthMetrics("test-server");

        assertThat(metrics.getSuccessCount()).isZero();
        assertThat(metrics.getFailureCount()).isZero();
        assertThat(metrics.getTimeoutCount()).isZero();
        assertThat(metrics.getKickedCount()).isZero();
    }

    @Test
    void incrementSuccess_incrementsCount() {
        AuthMetrics metrics = new AuthMetrics("test-server");

        metrics.incrementSuccess();
        metrics.incrementSuccess();
        metrics.incrementSuccess();

        assertThat(metrics.getSuccessCount()).isEqualTo(3);
    }

    @Test
    void incrementFailure_incrementsCount() {
        AuthMetrics metrics = new AuthMetrics("test-server");

        metrics.incrementFailure();
        metrics.incrementFailure();

        assertThat(metrics.getFailureCount()).isEqualTo(2);
    }

    @Test
    void incrementTimeout_incrementsCount() {
        AuthMetrics metrics = new AuthMetrics("test-server");

        metrics.incrementTimeout();

        assertThat(metrics.getTimeoutCount()).isEqualTo(1);
    }

    @Test
    void incrementKicked_incrementsCount() {
        AuthMetrics metrics = new AuthMetrics("test-server");

        metrics.incrementKicked();
        metrics.incrementKicked();
        metrics.incrementKicked();
        metrics.incrementKicked();

        assertThat(metrics.getKickedCount()).isEqualTo(4);
    }

    @Test
    void toString_containsAllFields() {
        AuthMetrics metrics = new AuthMetrics("test-server");
        metrics.incrementSuccess();
        metrics.incrementFailure();
        metrics.incrementTimeout();
        metrics.incrementKicked();

        String str = metrics.toString();

        assertThat(str).contains("test-server");
        assertThat(str).contains("successCount=1");
        assertThat(str).contains("failureCount=1");
        assertThat(str).contains("timeoutCount=1");
        assertThat(str).contains("kickedCount=1");
    }
}
