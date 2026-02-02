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

package com.childrengreens.netty.spring.boot.actuator.endpoint;

import com.childrengreens.netty.spring.boot.context.client.ClientRuntime;
import com.childrengreens.netty.spring.boot.context.client.NettyClientOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.ServerRuntime;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * Actuator endpoint for Netty server and client information.
 *
 * <p>Exposes information about all configured Netty servers and clients at
 * {@code /actuator/netty}.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
@Endpoint(id = "netty")
public class NettyEndpoint {

    @Nullable
    private final NettyServerOrchestrator serverOrchestrator;

    @Nullable
    private final NettyClientOrchestrator clientOrchestrator;

    /**
     * Create a new NettyEndpoint.
     * @param serverOrchestrator the server orchestrator (nullable)
     * @param clientOrchestrator the client orchestrator (nullable)
     */
    public NettyEndpoint(@Nullable NettyServerOrchestrator serverOrchestrator,
                         @Nullable NettyClientOrchestrator clientOrchestrator) {
        this.serverOrchestrator = serverOrchestrator;
        this.clientOrchestrator = clientOrchestrator;
    }

    /**
     * Get an overview of configured Netty servers and clients.
     * @return summary information
     */
    @ReadOperation
    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("servers", new ArrayList<>(getServerRuntimes().keySet()));
        result.put("clients", new ArrayList<>(getClientRuntimes().keySet()));
        return result;
    }

    /**
     * Get information about all Netty servers or clients.
     * @param type the group name ("servers" or "clients")
     * @return details map, or null if type is not recognized
     */
    @ReadOperation
    public Map<String, Object> group(@Selector String type) {
        if ("servers".equalsIgnoreCase(type)) {
            return buildServers();
        }
        if ("clients".equalsIgnoreCase(type)) {
            return buildClients();
        }
        return null;
    }

    /**
     * Get information about a specific Netty server or client.
     * @param type the group name ("servers" or "clients")
     * @param name the server/client name
     * @return details map or null if not found
     */
    @ReadOperation
    public Map<String, Object> group(@Selector String type, @Selector String name) {
        if ("servers".equalsIgnoreCase(type)) {
            return server(name);
        }
        if ("clients".equalsIgnoreCase(type)) {
            return client(name);
        }
        return null;
    }

    /**
     * Get information about all Netty servers.
     * @return server information map
     */
    private Map<String, Object> buildServers() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, ServerRuntime> runtimes = getServerRuntimes();

        result.put("serverCount", runtimes.size());

        Map<String, Object> servers = new LinkedHashMap<>();
        for (Map.Entry<String, ServerRuntime> entry : runtimes.entrySet()) {
            servers.put(entry.getKey(), buildServerInfo(entry.getValue()));
        }
        result.put("servers", servers);

        return result;
    }

    /**
     * Get information about a specific Netty server.
     * @param serverName the server name
     * @return server information, or null if not found
     */
    public Map<String, Object> server(String serverName) {
        if (serverOrchestrator == null) {
            return null;
        }
        ServerRuntime runtime = serverOrchestrator.getRuntime(serverName);
        if (runtime == null) {
            return null;
        }
        return buildServerInfo(runtime);
    }

    /**
     * Get information about all Netty clients.
     * @return client information map
     */
    private Map<String, Object> buildClients() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, ClientRuntime> runtimes = getClientRuntimes();

        result.put("clientCount", runtimes.size());

        Map<String, Object> clients = new LinkedHashMap<>();
        for (Map.Entry<String, ClientRuntime> entry : runtimes.entrySet()) {
            clients.put(entry.getKey(), buildClientInfo(entry.getValue()));
        }
        result.put("clients", clients);

        return result;
    }

    /**
     * Get information about a specific Netty client.
     * @param clientName the client name
     * @return client information, or null if not found
     */
    public Map<String, Object> client(String clientName) {
        if (clientOrchestrator == null) {
            return null;
        }
        ClientRuntime runtime = clientOrchestrator.getRuntime(clientName);
        if (runtime == null) {
            return null;
        }
        return buildClientInfo(runtime);
    }

    /**
     * Build server information map.
     */
    private Map<String, Object> buildServerInfo(ServerRuntime runtime) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", runtime.getSpec().getName());
        String state = runtime.getState().name();
        info.put("state", state);
        info.put("status", state);
        info.put("transport", runtime.getSpec().getTransport().name());
        info.put("host", runtime.getSpec().getHost());
        info.put("port", runtime.getSpec().getPort());
        info.put("profile", runtime.getSpec().getProfile());
        info.put("routingMode", runtime.getSpec().getRouting().getMode().name());

        // Add feature status
        Map<String, Boolean> features = new HashMap<>();
        if (runtime.getSpec().getFeatures().getSsl() != null) {
            features.put("ssl", runtime.getSpec().getFeatures().getSsl().isEnabled());
        }
        if (runtime.getSpec().getFeatures().getIdle() != null) {
            features.put("idle", runtime.getSpec().getFeatures().getIdle().isEnabled());
        }
        if (runtime.getSpec().getFeatures().getLogging() != null) {
            features.put("logging", runtime.getSpec().getFeatures().getLogging().isEnabled());
        }
        info.put("features", features);

        Map<String, Object> connections = new LinkedHashMap<>();
        connections.put("current", runtime.getMetrics().getCurrentConnections());
        connections.put("total", runtime.getMetrics().getTotalConnections());
        info.put("connections", connections);

        return info;
    }

    /**
     * Build client information map.
     */
    private Map<String, Object> buildClientInfo(ClientRuntime runtime) {
        Map<String, Object> info = new LinkedHashMap<>();
        String state = runtime.getState().name();
        info.put("name", runtime.getClientSpec().getName());
        info.put("state", state);
        info.put("status", state);
        info.put("host", runtime.getClientSpec().getHost());
        info.put("port", runtime.getClientSpec().getPort());
        info.put("profile", runtime.getClientSpec().getProfile());

        Map<String, Object> pool = new LinkedHashMap<>();
        pool.put("size", runtime.getConnectionPool().getTotalConnections());
        pool.put("active", runtime.getConnectionPool().getBorrowedConnections());
        pool.put("idle", runtime.getConnectionPool().getIdleConnections());
        pool.put("pending", runtime.getConnectionPool().getPendingAcquires());
        info.put("pool", pool);

        return info;
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
