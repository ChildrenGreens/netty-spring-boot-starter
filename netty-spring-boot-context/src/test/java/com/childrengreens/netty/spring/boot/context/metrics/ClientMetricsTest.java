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

package com.childrengreens.netty.spring.boot.context.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ClientMetrics}.
 */
class ClientMetricsTest {

    private ClientMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new ClientMetrics("test-client");
    }

    @Test
    void getClientName_returnsConfiguredName() {
        assertEquals("test-client", metrics.getClientName());
    }

    @Test
    void recordRequest_incrementsCountAndAccumulatesLatency() {
        metrics.recordRequest(1000);
        metrics.recordRequest(2000);

        assertEquals(2, metrics.getRequestsTotal());
        assertEquals(3000, metrics.getRequestLatencyTotalNanos());
    }

    @Test
    void incrementRequests_onlyIncrementsCount() {
        metrics.incrementRequests();
        metrics.incrementRequests();
        metrics.incrementRequests();

        assertEquals(3, metrics.getRequestsTotal());
        assertEquals(0, metrics.getRequestLatencyTotalNanos());
    }

    @Test
    void incrementReconnectCount_incrementsCounter() {
        metrics.incrementReconnectCount();
        metrics.incrementReconnectCount();

        assertEquals(2, metrics.getReconnectCount());
    }

    @Test
    void toString_containsAllMetrics() {
        metrics.recordRequest(500);
        metrics.incrementReconnectCount();

        String str = metrics.toString();

        assertTrue(str.contains("clientName='test-client'"));
        assertTrue(str.contains("requestsTotal=1"));
        assertTrue(str.contains("reconnectCount=1"));
    }

    @Test
    void initialValues_areZero() {
        assertEquals(0, metrics.getRequestsTotal());
        assertEquals(0, metrics.getRequestLatencyTotalNanos());
        assertEquals(0, metrics.getReconnectCount());
    }
}
