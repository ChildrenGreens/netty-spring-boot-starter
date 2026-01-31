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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics collector for Netty server statistics.
 *
 * <p>This class collects various metrics for a Netty server including:
 * <ul>
 * <li>Connection counts (current and total)</li>
 * <li>Bytes transferred (in and out)</li>
 * <li>Request counts and latency</li>
 * </ul>
 *
 * <p>All methods are thread-safe and use atomic operations.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class ServerMetrics {

    private final String serverName;

    /**
     * Current number of active connections.
     */
    private final AtomicInteger currentConnections = new AtomicInteger(0);

    /**
     * Total number of connections ever established.
     */
    private final LongAdder totalConnections = new LongAdder();

    /**
     * Total bytes received.
     */
    private final LongAdder bytesIn = new LongAdder();

    /**
     * Total bytes sent.
     */
    private final LongAdder bytesOut = new LongAdder();

    /**
     * Total number of requests processed.
     */
    private final LongAdder requestsTotal = new LongAdder();

    /**
     * Total request processing time in nanoseconds (for calculating average latency).
     */
    private final LongAdder requestLatencyTotalNanos = new LongAdder();

    /**
     * Create a new ServerMetrics instance.
     * @param serverName the name of the server
     */
    public ServerMetrics(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Return the server name.
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }

    // ==================== Connection Metrics ====================

    /**
     * Increment the connection count when a new connection is established.
     */
    public void connectionOpened() {
        currentConnections.incrementAndGet();
        totalConnections.increment();
    }

    /**
     * Decrement the connection count when a connection is closed.
     */
    public void connectionClosed() {
        currentConnections.decrementAndGet();
    }

    /**
     * Return the current number of active connections.
     * @return the current connection count
     */
    public int getCurrentConnections() {
        return currentConnections.get();
    }

    /**
     * Return the total number of connections ever established.
     * @return the total connection count
     */
    public long getTotalConnections() {
        return totalConnections.sum();
    }

    // ==================== Bytes Metrics ====================

    /**
     * Add to the bytes received counter.
     * @param bytes the number of bytes received
     */
    public void addBytesIn(long bytes) {
        bytesIn.add(bytes);
    }

    /**
     * Add to the bytes sent counter.
     * @param bytes the number of bytes sent
     */
    public void addBytesOut(long bytes) {
        bytesOut.add(bytes);
    }

    /**
     * Return the total bytes received.
     * @return the total bytes in
     */
    public long getBytesIn() {
        return bytesIn.sum();
    }

    /**
     * Return the total bytes sent.
     * @return the total bytes out
     */
    public long getBytesOut() {
        return bytesOut.sum();
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
     * Return the total number of requests processed.
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

    @Override
    public String toString() {
        return "ServerMetrics{" +
                "serverName='" + serverName + '\'' +
                ", currentConnections=" + currentConnections.get() +
                ", totalConnections=" + totalConnections.sum() +
                ", bytesIn=" + bytesIn.sum() +
                ", bytesOut=" + bytesOut.sum() +
                ", requestsTotal=" + requestsTotal.sum() +
                '}';
    }

}
