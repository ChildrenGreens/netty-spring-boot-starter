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

import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.ServerRuntime;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * Micrometer metrics binder for Netty servers.
 *
 * <p>Registers the following metrics:
 * <ul>
 * <li>{@code netty.server.state} - Current state of each server (1=running, 0=not running)</li>
 * <li>{@code netty.connections.active} - Number of active connections</li>
 * <li>{@code netty.bytes.received} - Total bytes received</li>
 * <li>{@code netty.bytes.sent} - Total bytes sent</li>
 * </ul>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class NettyMetricsBinder implements MeterBinder {

    private static final Logger logger = LoggerFactory.getLogger(NettyMetricsBinder.class);

    private static final String METRIC_PREFIX = "netty";

    private final NettyServerOrchestrator orchestrator;

    /**
     * Create a new NettyMetricsBinder.
     * @param orchestrator the server orchestrator
     */
    public NettyMetricsBinder(NettyServerOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        Map<String, ServerRuntime> runtimes = orchestrator.getAllRuntimes();

        for (Map.Entry<String, ServerRuntime> entry : runtimes.entrySet()) {
            String serverName = entry.getKey();
            ServerRuntime runtime = entry.getValue();

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

            logger.debug("Registered metrics for server: {}", serverName);
        }
    }

}
