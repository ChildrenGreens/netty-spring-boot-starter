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
 * Tests for {@link ServerMetrics}.
 */
class ServerMetricsTest {

    private ServerMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new ServerMetrics("test-server");
    }

    @Test
    void getServerName_returnsConfiguredName() {
        assertEquals("test-server", metrics.getServerName());
    }

    @Test
    void connectionOpened_incrementsCurrentAndTotalConnections() {
        metrics.connectionOpened();
        metrics.connectionOpened();

        assertEquals(2, metrics.getCurrentConnections());
        assertEquals(2, metrics.getTotalConnections());
    }

    @Test
    void connectionClosed_decrementsCurrentConnections() {
        metrics.connectionOpened();
        metrics.connectionOpened();
        metrics.connectionClosed();

        assertEquals(1, metrics.getCurrentConnections());
        assertEquals(2, metrics.getTotalConnections()); // total unchanged
    }

    @Test
    void addBytesIn_accumulatesBytes() {
        metrics.addBytesIn(100);
        metrics.addBytesIn(200);

        assertEquals(300, metrics.getBytesIn());
    }

    @Test
    void addBytesOut_accumulatesBytes() {
        metrics.addBytesOut(150);
        metrics.addBytesOut(250);

        assertEquals(400, metrics.getBytesOut());
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
    void toString_containsAllMetrics() {
        metrics.connectionOpened();
        metrics.addBytesIn(100);
        metrics.addBytesOut(200);
        metrics.recordRequest(500);

        String str = metrics.toString();

        assertTrue(str.contains("serverName='test-server'"));
        assertTrue(str.contains("currentConnections=1"));
        assertTrue(str.contains("totalConnections=1"));
        assertTrue(str.contains("bytesIn=100"));
        assertTrue(str.contains("bytesOut=200"));
        assertTrue(str.contains("requestsTotal=1"));
    }

    @Test
    void initialValues_areZero() {
        assertEquals(0, metrics.getCurrentConnections());
        assertEquals(0, metrics.getTotalConnections());
        assertEquals(0, metrics.getBytesIn());
        assertEquals(0, metrics.getBytesOut());
        assertEquals(0, metrics.getRequestsTotal());
        assertEquals(0, metrics.getRequestLatencyTotalNanos());
    }
}
