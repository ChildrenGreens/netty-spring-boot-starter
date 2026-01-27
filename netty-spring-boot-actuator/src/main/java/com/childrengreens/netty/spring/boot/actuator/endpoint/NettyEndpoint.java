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

import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.ServerRuntime;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Actuator endpoint for Netty server information.
 *
 * <p>Exposes information about all configured Netty servers at
 * {@code /actuator/netty}.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
@Endpoint(id = "netty")
public class NettyEndpoint {

    private final NettyServerOrchestrator orchestrator;

    /**
     * Create a new NettyEndpoint.
     * @param orchestrator the server orchestrator
     */
    public NettyEndpoint(NettyServerOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Get information about all Netty servers.
     * @return server information map
     */
    @ReadOperation
    public Map<String, Object> servers() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, ServerRuntime> runtimes = orchestrator.getAllRuntimes();

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
    @ReadOperation
    public Map<String, Object> server(@Selector String serverName) {
        ServerRuntime runtime = orchestrator.getRuntime(serverName);
        if (runtime == null) {
            return null;
        }
        return buildServerInfo(runtime);
    }

    /**
     * Build server information map.
     */
    private Map<String, Object> buildServerInfo(ServerRuntime runtime) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", runtime.getSpec().getName());
        info.put("state", runtime.getState().name());
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

        return info;
    }

}
