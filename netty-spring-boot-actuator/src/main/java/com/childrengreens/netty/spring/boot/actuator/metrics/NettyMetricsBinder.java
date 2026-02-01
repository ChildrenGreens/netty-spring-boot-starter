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

package com.childrengreens.netty.spring.boot.actuator.metrics;

import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureMetrics;
import com.childrengreens.netty.spring.boot.context.client.ClientRuntime;
import com.childrengreens.netty.spring.boot.context.client.ConnectionPool;
import com.childrengreens.netty.spring.boot.context.client.NettyClientOrchestrator;
import com.childrengreens.netty.spring.boot.context.metrics.ClientMetrics;
import com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics;
import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.ServerRuntime;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * Micrometer metrics binder for Netty servers and clients.
 *
 * <p>Registers the following server metrics:
 * <ul>
 * <li>{@code netty.server.state} - Current state of each server (1=running, 0=not running)</li>
 * <li>{@code netty.server.connections.current} - Number of current active connections</li>
 * <li>{@code netty.server.connections.total} - Total connections ever established</li>
 * <li>{@code netty.server.bytes.in} - Total bytes received</li>
 * <li>{@code netty.server.bytes.out} - Total bytes sent</li>
 * <li>{@code netty.server.requests.total} - Total requests processed</li>
 * <li>{@code netty.server.request.latency.total} - Total request latency in nanoseconds</li>
 * <li>{@code netty.server.backpressure.suspend} - Total times reading was suspended (backpressure)</li>
 * <li>{@code netty.server.backpressure.resume} - Total times reading was resumed (backpressure)</li>
 * <li>{@code netty.server.backpressure.dropped} - Total messages dropped due to backpressure</li>
 * <li>{@code netty.server.backpressure.disconnect} - Total disconnections due to backpressure</li>
 * </ul>
 *
 * <p>Registers the following client metrics:
 * <ul>
 * <li>{@code netty.client.connections.current} - Number of current connections in pool</li>
 * <li>{@code netty.client.pool.size} - Total connections in pool</li>
 * <li>{@code netty.client.pool.pending} - Pending acquire requests</li>
 * <li>{@code netty.client.requests.total} - Total requests sent</li>
 * <li>{@code netty.client.request.latency.total} - Total request latency in nanoseconds</li>
 * <li>{@code netty.client.reconnect.count} - Total reconnection attempts</li>
 * </ul>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class NettyMetricsBinder implements MeterBinder {

    private static final Logger logger = LoggerFactory.getLogger(NettyMetricsBinder.class);

    private static final String METRIC_PREFIX = "netty";

    private final NettyServerOrchestrator serverOrchestrator;
    private final NettyClientOrchestrator clientOrchestrator;

    /**
     * Create a new NettyMetricsBinder for servers only.
     * @param serverOrchestrator the server orchestrator
     */
    public NettyMetricsBinder(NettyServerOrchestrator serverOrchestrator) {
        this(serverOrchestrator, null);
    }

    /**
     * Create a new NettyMetricsBinder for servers and clients.
     * @param serverOrchestrator the server orchestrator (may be null)
     * @param clientOrchestrator the client orchestrator (may be null)
     * @since 0.0.2
     */
    public NettyMetricsBinder(@Nullable NettyServerOrchestrator serverOrchestrator,
                               @Nullable NettyClientOrchestrator clientOrchestrator) {
        this.serverOrchestrator = serverOrchestrator;
        this.clientOrchestrator = clientOrchestrator;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        if (serverOrchestrator != null) {
            bindServerMetrics(registry);
        }
        if (clientOrchestrator != null) {
            bindClientMetrics(registry);
        }
    }

    /**
     * Bind server metrics to the registry.
     */
    private void bindServerMetrics(MeterRegistry registry) {
        Map<String, ServerRuntime> runtimes = serverOrchestrator.getAllRuntimes();

        for (Map.Entry<String, ServerRuntime> entry : runtimes.entrySet()) {
            String serverName = entry.getKey();
            ServerRuntime runtime = entry.getValue();
            ServerMetrics metrics = runtime.getMetrics();

            Tags tags = Tags.of(
                    "server", serverName,
                    "transport", runtime.getSpec().getTransport().name(),
                    "profile", runtime.getSpec().getProfile()
            );

            // Server state gauge
            Gauge.builder(METRIC_PREFIX + ".server.state", runtime,
                            r -> r.isRunning() ? 1.0 : 0.0)
                    .tags(tags)
                    .description("Server state (1=running, 0=not running)")
                    .register(registry);

            // Connection metrics
            Gauge.builder(METRIC_PREFIX + ".server.connections.current", metrics,
                            ServerMetrics::getCurrentConnections)
                    .tags(tags)
                    .description("Current number of active connections")
                    .register(registry);

            Gauge.builder(METRIC_PREFIX + ".server.connections.total", metrics,
                            ServerMetrics::getTotalConnections)
                    .tags(tags)
                    .description("Total connections ever established")
                    .register(registry);

            // Bytes metrics
            Gauge.builder(METRIC_PREFIX + ".server.bytes.in", metrics,
                            ServerMetrics::getBytesIn)
                    .tags(tags)
                    .baseUnit("bytes")
                    .description("Total bytes received")
                    .register(registry);

            Gauge.builder(METRIC_PREFIX + ".server.bytes.out", metrics,
                            ServerMetrics::getBytesOut)
                    .tags(tags)
                    .baseUnit("bytes")
                    .description("Total bytes sent")
                    .register(registry);

            // Request metrics
            Gauge.builder(METRIC_PREFIX + ".server.requests.total", metrics,
                            ServerMetrics::getRequestsTotal)
                    .tags(tags)
                    .description("Total requests processed")
                    .register(registry);

            Gauge.builder(METRIC_PREFIX + ".server.request.latency.total", metrics,
                            ServerMetrics::getRequestLatencyTotalNanos)
                    .tags(tags)
                    .baseUnit("nanoseconds")
                    .description("Total request latency in nanoseconds")
                    .register(registry);

            // Backpressure metrics (if enabled)
            BackpressureMetrics backpressureMetrics = runtime.getBackpressureMetrics();
            if (backpressureMetrics != null) {
                Gauge.builder(METRIC_PREFIX + ".server.backpressure.suspend", backpressureMetrics,
                                BackpressureMetrics::getSuspendCount)
                        .tags(tags)
                        .description("Total times reading was suspended due to backpressure")
                        .register(registry);

                Gauge.builder(METRIC_PREFIX + ".server.backpressure.resume", backpressureMetrics,
                                BackpressureMetrics::getResumeCount)
                        .tags(tags)
                        .description("Total times reading was resumed after backpressure")
                        .register(registry);

                Gauge.builder(METRIC_PREFIX + ".server.backpressure.dropped", backpressureMetrics,
                                BackpressureMetrics::getDroppedCount)
                        .tags(tags)
                        .description("Total messages dropped due to backpressure")
                        .register(registry);

                Gauge.builder(METRIC_PREFIX + ".server.backpressure.disconnect", backpressureMetrics,
                                BackpressureMetrics::getDisconnectCount)
                        .tags(tags)
                        .description("Total disconnections due to backpressure")
                        .register(registry);
            }

            logger.debug("Registered metrics for server: {}", serverName);
        }
    }

    /**
     * Bind client metrics to the registry.
     */
    private void bindClientMetrics(MeterRegistry registry) {
        Map<String, ClientRuntime> runtimes = clientOrchestrator.getAllRuntimes();

        for (Map.Entry<String, ClientRuntime> entry : runtimes.entrySet()) {
            String clientName = entry.getKey();
            ClientRuntime runtime = entry.getValue();
            ClientMetrics metrics = runtime.getMetrics();
            ConnectionPool connectionPool = runtime.getConnectionPool();

            Tags tags = Tags.of(
                    "client", clientName,
                    "host", runtime.getClientSpec().getHost(),
                    "port", String.valueOf(runtime.getClientSpec().getPort()),
                    "profile", runtime.getClientSpec().getProfile()
            );

            // Connection pool metrics
            Gauge.builder(METRIC_PREFIX + ".client.connections.current", connectionPool,
                            ConnectionPool::getBorrowedConnections)
                    .tags(tags)
                    .description("Current number of in-use connections")
                    .register(registry);

            Gauge.builder(METRIC_PREFIX + ".client.pool.size", connectionPool,
                            ConnectionPool::getTotalConnections)
                    .tags(tags)
                    .description("Total connections in pool")
                    .register(registry);

            Gauge.builder(METRIC_PREFIX + ".client.pool.pending", connectionPool,
                            ConnectionPool::getPendingAcquires)
                    .tags(tags)
                    .description("Pending acquire requests")
                    .register(registry);

            // Request metrics
            Gauge.builder(METRIC_PREFIX + ".client.requests.total", metrics,
                            ClientMetrics::getRequestsTotal)
                    .tags(tags)
                    .description("Total requests sent")
                    .register(registry);

            Gauge.builder(METRIC_PREFIX + ".client.request.latency.total", metrics,
                            ClientMetrics::getRequestLatencyTotalNanos)
                    .tags(tags)
                    .baseUnit("nanoseconds")
                    .description("Total request latency in nanoseconds")
                    .register(registry);

            // Reconnect metrics
            Gauge.builder(METRIC_PREFIX + ".client.reconnect.count", metrics,
                            ClientMetrics::getReconnectCount)
                    .tags(tags)
                    .description("Total reconnection attempts")
                    .register(registry);

            logger.debug("Registered metrics for client: {}", clientName);
        }
    }

}
