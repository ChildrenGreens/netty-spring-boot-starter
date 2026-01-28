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
 * Specification for a single Netty client instance.
 *
 * <p>Each client specification defines the target host, port, protocol profile,
 * connection pool, reconnection, heartbeat, and timeout settings.
 *
 * <p>Example:
 * <pre>{@code
 * - name: order-service
 *   host: 127.0.0.1
 *   port: 9000
 *   profile: tcp-lengthfield-json
 *   pool:
 *     maxConnections: 10
 *   reconnect:
 *     enabled: true
 * }</pre>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see PoolSpec
 * @see ReconnectSpec
 * @see HeartbeatSpec
 * @see TimeoutSpec
 */
public class ClientSpec {

    /**
     * Unique name for this client instance.
     */
    private String name;

    /**
     * Target host address to connect to.
     */
    private String host = "127.0.0.1";

    /**
     * Target port number to connect to.
     */
    private int port;

    /**
     * Protocol profile name defining the pipeline template.
     */
    private String profile;

    /**
     * Connection pool configuration.
     */
    @NestedConfigurationProperty
    private PoolSpec pool = new PoolSpec();

    /**
     * Auto-reconnection configuration.
     */
    @NestedConfigurationProperty
    private ReconnectSpec reconnect = new ReconnectSpec();

    /**
     * Heartbeat/keep-alive configuration.
     */
    @NestedConfigurationProperty
    private HeartbeatSpec heartbeat = new HeartbeatSpec();

    /**
     * Timeout configuration.
     */
    @NestedConfigurationProperty
    private TimeoutSpec timeout = new TimeoutSpec();

    /**
     * Feature configurations for this client.
     */
    @NestedConfigurationProperty
    private FeaturesSpec features = new FeaturesSpec();

    /**
     * Thread configuration for this client.
     */
    @NestedConfigurationProperty
    private ThreadsSpec threads;

    /**
     * Return the unique name of this client.
     * @return the client name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the unique name for this client.
     * @param name the client name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the target host address.
     * @return the host address
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Set the target host address.
     * @param host the host address
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Return the target port number.
     * @return the port number
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Set the target port number.
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
     * Return the connection pool configuration.
     * @return the pool specification
     */
    public PoolSpec getPool() {
        return this.pool;
    }

    /**
     * Set the connection pool configuration.
     * @param pool the pool specification
     */
    public void setPool(PoolSpec pool) {
        this.pool = pool;
    }

    /**
     * Return the reconnection configuration.
     * @return the reconnect specification
     */
    public ReconnectSpec getReconnect() {
        return this.reconnect;
    }

    /**
     * Set the reconnection configuration.
     * @param reconnect the reconnect specification
     */
    public void setReconnect(ReconnectSpec reconnect) {
        this.reconnect = reconnect;
    }

    /**
     * Return the heartbeat configuration.
     * @return the heartbeat specification
     */
    public HeartbeatSpec getHeartbeat() {
        return this.heartbeat;
    }

    /**
     * Set the heartbeat configuration.
     * @param heartbeat the heartbeat specification
     */
    public void setHeartbeat(HeartbeatSpec heartbeat) {
        this.heartbeat = heartbeat;
    }

    /**
     * Return the timeout configuration.
     * @return the timeout specification
     */
    public TimeoutSpec getTimeout() {
        return this.timeout;
    }

    /**
     * Set the timeout configuration.
     * @param timeout the timeout specification
     */
    public void setTimeout(TimeoutSpec timeout) {
        this.timeout = timeout;
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
     * Return the thread configuration.
     * @return the threads specification, or {@code null} if using defaults
     */
    public ThreadsSpec getThreads() {
        return this.threads;
    }

    /**
     * Set the thread configuration.
     * @param threads the threads specification
     */
    public void setThreads(ThreadsSpec threads) {
        this.threads = threads;
    }

}
