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

package com.childrengreens.netty.spring.boot.actuator.health;

import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.ServerRuntime;
import com.childrengreens.netty.spring.boot.context.server.ServerState;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for Netty servers.
 *
 * <p>Reports the health status of all configured Netty servers including:
 * <ul>
 * <li>Server state (running, stopped, failed)</li>
 * <li>Binding address and port</li>
 * <li>Transport type</li>
 * </ul>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public class NettyHealthIndicator implements HealthIndicator {

    private final NettyServerOrchestrator orchestrator;

    /**
     * Create a new NettyHealthIndicator.
     * @param orchestrator the server orchestrator
     */
    public NettyHealthIndicator(NettyServerOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public Health health() {
        Map<String, ServerRuntime> runtimes = orchestrator.getAllRuntimes();

        if (runtimes.isEmpty()) {
            return Health.unknown()
                    .withDetail("message", "No Netty servers configured")
                    .build();
        }

        boolean allHealthy = true;
        Map<String, Object> details = new HashMap<>();

        for (Map.Entry<String, ServerRuntime> entry : runtimes.entrySet()) {
            String name = entry.getKey();
            ServerRuntime runtime = entry.getValue();

            Map<String, Object> serverInfo = new HashMap<>();
            serverInfo.put("state", runtime.getState().name());
            serverInfo.put("transport", runtime.getSpec().getTransport().name());
            serverInfo.put("host", runtime.getSpec().getHost());
            serverInfo.put("port", runtime.getSpec().getPort());
            serverInfo.put("profile", runtime.getSpec().getProfile());

            if (runtime.getState() != ServerState.RUNNING) {
                allHealthy = false;
            }

            details.put(name, serverInfo);
        }

        Health.Builder builder = allHealthy ? Health.up() : Health.down();
        builder.withDetails(details);

        return builder.build();
    }

}
