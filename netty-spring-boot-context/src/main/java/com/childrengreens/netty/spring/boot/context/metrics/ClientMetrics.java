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

import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics collector for Netty client statistics.
 *
 * <p>This class collects various metrics for a Netty client including:
 * <ul>
 * <li>Request counts and latency</li>
 * <li>Reconnection attempts</li>
 * </ul>
 *
 * <p>Connection pool metrics are obtained directly from {@link com.childrengreens.netty.spring.boot.context.client.ConnectionPool}.
 *
 * <p>All methods are thread-safe and use atomic operations.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class ClientMetrics {

    private final String clientName;

    /**
     * Total number of requests sent.
     */
    private final LongAdder requestsTotal = new LongAdder();

    /**
     * Total request processing time in nanoseconds (for calculating average latency).
     */
    private final LongAdder requestLatencyTotalNanos = new LongAdder();

    /**
     * Total number of reconnection attempts.
     */
    private final LongAdder reconnectCount = new LongAdder();

    /**
     * Create a new ClientMetrics instance.
     * @param clientName the name of the client
     */
    public ClientMetrics(String clientName) {
        this.clientName = clientName;
    }

    /**
     * Return the client name.
     * @return the client name
     */
    public String getClientName() {
        return clientName;
    }

    // ==================== Request Metrics ====================

    /**
     * Record a completed request with its latency.
     * @param latencyNanos the request latency in nanoseconds
     */
    public void recordRequest(long latencyNanos) {
        requestsTotal.increment();
        requestLatencyTotalNanos.add(latencyNanos);
    }

    /**
     * Increment the request counter without recording latency.
     */
    public void incrementRequests() {
        requestsTotal.increment();
    }

    /**
     * Return the total number of requests sent.
     * @return the total request count
     */
    public long getRequestsTotal() {
        return requestsTotal.sum();
    }

    /**
     * Return the total request latency in nanoseconds.
     * @return the total latency in nanoseconds
     */
    public long getRequestLatencyTotalNanos() {
        return requestLatencyTotalNanos.sum();
    }

    // ==================== Reconnect Metrics ====================

    /**
     * Increment the reconnection attempt counter.
     */
    public void incrementReconnectCount() {
        reconnectCount.increment();
    }

    /**
     * Return the total number of reconnection attempts.
     * @return the reconnect count
     */
    public long getReconnectCount() {
        return reconnectCount.sum();
    }

    @Override
    public String toString() {
        return "ClientMetrics{" +
                "clientName='" + clientName + '\'' +
                ", requestsTotal=" + requestsTotal.sum() +
                ", reconnectCount=" + reconnectCount.sum() +
                '}';
    }

}
