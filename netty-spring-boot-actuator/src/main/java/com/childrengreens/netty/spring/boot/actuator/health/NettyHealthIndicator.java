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

import com.childrengreens.netty.spring.boot.context.client.ClientRuntime;
import com.childrengreens.netty.spring.boot.context.client.NettyClientOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.ServerRuntime;
import com.childrengreens.netty.spring.boot.context.server.ServerState;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health indicator for Netty servers and clients.
 *
 * <p>Reports the health status of all configured Netty servers and clients including:
 * <ul>
 * <li>Server state (running, stopped, failed)</li>
 * <li>Binding address and port</li>
 * <li>Transport type</li>
 * </ul>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class NettyHealthIndicator implements HealthIndicator {

    @Nullable
    private final NettyServerOrchestrator serverOrchestrator;

    @Nullable
    private final NettyClientOrchestrator clientOrchestrator;

    /**
     * Create a new NettyHealthIndicator.
     * @param serverOrchestrator the server orchestrator (nullable)
     * @param clientOrchestrator the client orchestrator (nullable)
     */
    public NettyHealthIndicator(@Nullable NettyServerOrchestrator serverOrchestrator,
                                @Nullable NettyClientOrchestrator clientOrchestrator) {
        this.serverOrchestrator = serverOrchestrator;
        this.clientOrchestrator = clientOrchestrator;
    }

    @Override
    public Health health() {
        Map<String, ServerRuntime> serverRuntimes = getServerRuntimes();
        Map<String, ClientRuntime> clientRuntimes = getClientRuntimes();

        if (serverRuntimes.isEmpty() && clientRuntimes.isEmpty()) {
            return Health.unknown()
                    .withDetail("message", "No Netty servers or clients configured")
                    .build();
        }

        boolean allHealthy = true;
        Map<String, Object> details = new LinkedHashMap<>();

        if (!serverRuntimes.isEmpty()) {
            Map<String, Object> servers = new LinkedHashMap<>();
            for (Map.Entry<String, ServerRuntime> entry : serverRuntimes.entrySet()) {
                String name = entry.getKey();
                ServerRuntime runtime = entry.getValue();
                boolean serverHealthy = runtime.getState() == ServerState.RUNNING;
                servers.put(name, serverHealthy ? "UP" : "DOWN");
                if (!serverHealthy) {
                    allHealthy = false;
                }
            }
            details.put("servers", servers);
        }

        if (!clientRuntimes.isEmpty()) {
            Map<String, Object> clients = new LinkedHashMap<>();
            for (Map.Entry<String, ClientRuntime> entry : clientRuntimes.entrySet()) {
                String name = entry.getKey();
                ClientRuntime runtime = entry.getValue();
                boolean clientHealthy = runtime.getState() == ClientRuntime.ClientState.RUNNING;
                clients.put(name, clientHealthy ? "UP" : "DOWN");
                if (!clientHealthy) {
                    allHealthy = false;
                }
            }
            details.put("clients", clients);
        }

        Health.Builder builder = allHealthy ? Health.up() : Health.down();
        builder.withDetails(details);

        return builder.build();
    }

    private Map<String, ServerRuntime> getServerRuntimes() {
        if (serverOrchestrator == null) {
            return Collections.emptyMap();
        }
        Map<String, ServerRuntime> runtimes = serverOrchestrator.getAllRuntimes();
        return runtimes != null ? runtimes : Collections.emptyMap();
    }

    private Map<String, ClientRuntime> getClientRuntimes() {
        if (clientOrchestrator == null) {
            return Collections.emptyMap();
        }
        Map<String, ClientRuntime> runtimes = clientOrchestrator.getAllRuntimes();
        return runtimes != null ? runtimes : Collections.emptyMap();
    }

}
