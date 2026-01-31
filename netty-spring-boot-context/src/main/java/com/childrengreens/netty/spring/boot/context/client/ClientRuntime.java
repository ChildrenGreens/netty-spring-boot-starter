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

package com.childrengreens.netty.spring.boot.context.client;

import com.childrengreens.netty.spring.boot.context.metrics.ClientMetrics;
import com.childrengreens.netty.spring.boot.context.properties.ClientSpec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;

/**
 * Runtime information for a client instance.
 *
 * <p>This class holds the runtime state of a client, including its
 * bootstrap, event loop group, connection pool, and managers.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class ClientRuntime {

    private final ClientSpec clientSpec;
    private final Bootstrap bootstrap;
    private final EventLoopGroup workerGroup;
    private final ConnectionPool connectionPool;
    private final ReconnectManager reconnectManager;
    private final HeartbeatManager heartbeatManager;
    private final RequestInvoker requestInvoker;
    private final ClientMetrics metrics;

    private volatile ClientState state = ClientState.CREATED;

    /**
     * Client states.
     */
    public enum ClientState {
        CREATED,
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED
    }

    /**
     * Create a new ClientRuntime.
     * @param clientSpec the client specification
     * @param bootstrap the bootstrap
     * @param workerGroup the worker event loop group
     * @param connectionPool the connection pool
     * @param reconnectManager the reconnect manager
     * @param heartbeatManager the heartbeat manager
     * @param requestInvoker the request invoker
     */
    public ClientRuntime(ClientSpec clientSpec, Bootstrap bootstrap, EventLoopGroup workerGroup,
                         ConnectionPool connectionPool, ReconnectManager reconnectManager,
                         HeartbeatManager heartbeatManager, RequestInvoker requestInvoker) {
        this(clientSpec, bootstrap, workerGroup, connectionPool, reconnectManager,
                heartbeatManager, requestInvoker, new ClientMetrics(clientSpec.getName()));
    }

    /**
     * Create a new ClientRuntime with metrics.
     * @param clientSpec the client specification
     * @param bootstrap the bootstrap
     * @param workerGroup the worker event loop group
     * @param connectionPool the connection pool
     * @param reconnectManager the reconnect manager
     * @param heartbeatManager the heartbeat manager
     * @param requestInvoker the request invoker
     * @param metrics the client metrics
     * @since 0.0.2
     */
    public ClientRuntime(ClientSpec clientSpec, Bootstrap bootstrap, EventLoopGroup workerGroup,
                         ConnectionPool connectionPool, ReconnectManager reconnectManager,
                         HeartbeatManager heartbeatManager, RequestInvoker requestInvoker,
                         ClientMetrics metrics) {
        this.clientSpec = clientSpec;
        this.bootstrap = bootstrap;
        this.workerGroup = workerGroup;
        this.connectionPool = connectionPool;
        this.reconnectManager = reconnectManager;
        this.heartbeatManager = heartbeatManager;
        this.requestInvoker = requestInvoker;
        this.metrics = metrics;
    }

    /**
     * Return the client specification.
     * @return the client spec
     */
    public ClientSpec getClientSpec() {
        return this.clientSpec;
    }

    /**
     * Return the bootstrap.
     * @return the bootstrap
     */
    public Bootstrap getBootstrap() {
        return this.bootstrap;
    }

    /**
     * Return the worker event loop group.
     * @return the worker group
     */
    public EventLoopGroup getWorkerGroup() {
        return this.workerGroup;
    }

    /**
     * Return the connection pool.
     * @return the connection pool
     */
    public ConnectionPool getConnectionPool() {
        return this.connectionPool;
    }

    /**
     * Return the reconnect manager.
     * @return the reconnect manager
     */
    public ReconnectManager getReconnectManager() {
        return this.reconnectManager;
    }

    /**
     * Return the heartbeat manager.
     * @return the heartbeat manager
     */
    public HeartbeatManager getHeartbeatManager() {
        return this.heartbeatManager;
    }

    /**
     * Return the request invoker.
     * @return the request invoker
     */
    public RequestInvoker getRequestInvoker() {
        return this.requestInvoker;
    }

    /**
     * Return the client metrics.
     * @return the metrics
     * @since 0.0.2
     */
    public ClientMetrics getMetrics() {
        return this.metrics;
    }

    /**
     * Return the current state.
     * @return the state
     */
    public ClientState getState() {
        return this.state;
    }

    /**
     * Set the current state.
     * @param state the state
     */
    public void setState(ClientState state) {
        this.state = state;
    }

    /**
     * Return the client name.
     * @return the client name
     */
    public String getName() {
        return clientSpec.getName();
    }

}
