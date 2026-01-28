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

package com.childrengreens.netty.spring.boot.context.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Netty Spring Boot Starter.
 *
 * <p>This class provides a declarative way to configure multiple Netty servers
 * supporting various protocols (TCP, UDP, HTTP, WebSocket) through
 * {@code application.yml} or {@code application.properties}.
 *
 * <p>Example configuration:
 * <pre>{@code
 * netty:
 *   enabled: true
 *   servers:
 *     - name: tcp-server
 *       transport: TCP
 *       port: 9000
 *       profile: tcp-lengthfield-json
 * }</pre>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see ServerSpec
 * @see DefaultsSpec
 */
@ConfigurationProperties(prefix = "netty")
public class NettyProperties {

    /**
     * Whether to enable Netty auto-configuration.
     */
    private boolean enabled = true;

    /**
     * Default settings applied to all servers unless overridden.
     */
    @NestedConfigurationProperty
    private DefaultsSpec defaults = new DefaultsSpec();

    /**
     * List of server specifications to be started.
     */
    private List<ServerSpec> servers = new ArrayList<>();

    /**
     * List of client specifications to be created.
     */
    private List<ClientSpec> clients = new ArrayList<>();

    /**
     * Observability settings for metrics and health checks.
     */
    @NestedConfigurationProperty
    private ObservabilitySpec observability = new ObservabilitySpec();

    /**
     * Return whether Netty auto-configuration is enabled.
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Set whether to enable Netty auto-configuration.
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the default settings for all servers.
     * @return the defaults specification
     */
    public DefaultsSpec getDefaults() {
        return this.defaults;
    }

    /**
     * Set the default settings for all servers.
     * @param defaults the defaults specification
     */
    public void setDefaults(DefaultsSpec defaults) {
        this.defaults = defaults;
    }

    /**
     * Return the list of server specifications.
     * @return the server specifications
     */
    public List<ServerSpec> getServers() {
        return this.servers;
    }

    /**
     * Set the list of server specifications.
     * @param servers the server specifications
     */
    public void setServers(List<ServerSpec> servers) {
        this.servers = servers;
    }

    /**
     * Return the list of client specifications.
     * @return the client specifications
     */
    public List<ClientSpec> getClients() {
        return this.clients;
    }

    /**
     * Set the list of client specifications.
     * @param clients the client specifications
     */
    public void setClients(List<ClientSpec> clients) {
        this.clients = clients;
    }

    /**
     * Return the observability settings.
     * @return the observability specification
     */
    public ObservabilitySpec getObservability() {
        return this.observability;
    }

    /**
     * Set the observability settings.
     * @param observability the observability specification
     */
    public void setObservability(ObservabilitySpec observability) {
        this.observability = observability;
    }

}
