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

import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Specification for a single Netty server instance.
 *
 * <p>Each server specification defines the transport protocol, binding address,
 * port, protocol profile, routing mode, and optional features.
 *
 * <p>Example:
 * <pre>{@code
 * - name: tcp-9000
 *   transport: TCP
 *   host: 0.0.0.0
 *   port: 9000
 *   profile: tcp-lengthfield-json
 *   routing:
 *     mode: MESSAGE_TYPE
 * }</pre>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see TransportType
 * @see RoutingMode
 */
public class ServerSpec {

    /**
     * Unique name for this server instance.
     */
    private String name;

    /**
     * Transport protocol type (TCP, UDP, HTTP).
     */
    private TransportType transport = TransportType.TCP;

    /**
     * Host address to bind to.
     */
    private String host = "0.0.0.0";

    /**
     * Port number to listen on.
     */
    private int port;

    /**
     * Protocol profile name defining the pipeline template.
     */
    private String profile;

    /**
     * Routing configuration for message dispatching.
     */
    @NestedConfigurationProperty
    private RoutingSpec routing = new RoutingSpec();

    /**
     * Feature configurations for this server.
     */
    @NestedConfigurationProperty
    private FeaturesSpec features = new FeaturesSpec();

    /**
     * Thread configuration overrides for this server.
     */
    @NestedConfigurationProperty
    private ThreadsSpec threads;

    /**
     * Return the unique name of this server.
     * @return the server name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the unique name for this server.
     * @param name the server name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the transport protocol type.
     * @return the transport type
     */
    public TransportType getTransport() {
        return this.transport;
    }

    /**
     * Set the transport protocol type.
     * @param transport the transport type
     */
    public void setTransport(TransportType transport) {
        this.transport = transport;
    }

    /**
     * Return the host address to bind to.
     * @return the host address
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Set the host address to bind to.
     * @param host the host address
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Return the port number.
     * @return the port number
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Set the port number to listen on.
     * @param port the port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Return the protocol profile name.
     * @return the profile name
     */
    public String getProfile() {
        return this.profile;
    }

    /**
     * Set the protocol profile name.
     * @param profile the profile name
     */
    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * Return the routing configuration.
     * @return the routing specification
     */
    public RoutingSpec getRouting() {
        return this.routing;
    }

    /**
     * Set the routing configuration.
     * @param routing the routing specification
     */
    public void setRouting(RoutingSpec routing) {
        this.routing = routing;
    }

    /**
     * Return the features configuration.
     * @return the features specification
     */
    public FeaturesSpec getFeatures() {
        return this.features;
    }

    /**
     * Set the features configuration.
     * @param features the features specification
     */
    public void setFeatures(FeaturesSpec features) {
        this.features = features;
    }

    /**
     * Return the thread configuration overrides.
     * @return the threads specification, or {@code null} if using defaults
     */
    public ThreadsSpec getThreads() {
        return this.threads;
    }

    /**
     * Set the thread configuration overrides.
     * @param threads the threads specification
     */
    public void setThreads(ThreadsSpec threads) {
        this.threads = threads;
    }

}
