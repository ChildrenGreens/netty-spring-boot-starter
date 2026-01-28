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

import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.codec.NettyCodec;
import com.childrengreens.netty.spring.boot.context.properties.ClientSpec;
import com.childrengreens.netty.spring.boot.context.properties.DefaultsSpec;
import com.childrengreens.netty.spring.boot.context.properties.NettyProperties;
import com.childrengreens.netty.spring.boot.context.properties.ThreadsSpec;
import com.childrengreens.netty.spring.boot.context.transport.TransportFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrator for managing multiple Netty client instances.
 *
 * <p>This class is responsible for:
 * <ul>
 * <li>Reading client specifications from configuration</li>
 * <li>Creating and initializing client instances</li>
 * <li>Managing client lifecycle (start, stop)</li>
 * <li>Handling graceful shutdown</li>
 * </ul>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see ClientRuntime
 * @see NettyProperties
 */
public class NettyClientOrchestrator implements InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(NettyClientOrchestrator.class);

    private final NettyProperties properties;
    private final TransportFactory transportFactory;
    private final ClientPipelineAssembler pipelineAssembler;
    private final CodecRegistry codecRegistry;
    private final Map<String, ClientRuntime> runtimes = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduledExecutor;
    private boolean failFast = true;

    /**
     * Create a new NettyClientOrchestrator.
     * @param properties the Netty properties
     * @param transportFactory the transport factory
     * @param pipelineAssembler the pipeline assembler
     * @param codecRegistry the codec registry
     */
    public NettyClientOrchestrator(NettyProperties properties, TransportFactory transportFactory,
                                    ClientPipelineAssembler pipelineAssembler, CodecRegistry codecRegistry) {
        this.properties = properties;
        this.transportFactory = transportFactory;
        this.pipelineAssembler = pipelineAssembler;
        this.codecRegistry = codecRegistry;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    @Override
    public void destroy() throws Exception {
        stop();
    }

    /**
     * Start all configured clients.
     */
    public void start() {
        if (!properties.isEnabled()) {
            logger.info("Netty is disabled, skipping client startup");
            return;
        }

        if (properties.getClients().isEmpty()) {
            logger.debug("No clients configured");
            return;
        }

        // Create shared scheduler
        scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "netty-client-scheduler");
            t.setDaemon(true);
            return t;
        });

        for (ClientSpec spec : properties.getClients()) {
            try {
                startClient(spec);
            } catch (Exception e) {
                logger.error("Failed to start client [{}]", spec.getName(), e);
                if (failFast) {
                    throw new IllegalStateException("Failed to start client: " + spec.getName(), e);
                }
            }
        }

        logger.info("Started {} Netty client(s)", runtimes.size());
    }

    /**
     * Stop all running clients.
     */
    public void stop() {
        logger.info("Stopping {} Netty client(s)...", runtimes.size());

        for (ClientRuntime runtime : runtimes.values()) {
            stopClient(runtime);
        }

        runtimes.clear();

        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        logger.info("All Netty clients stopped");
    }

    /**
     * Start a single client.
     */
    private void startClient(ClientSpec spec) throws Exception {
        logger.info("Starting client [{}] connecting to {}:{} with profile [{}]",
                spec.getName(), spec.getHost(), spec.getPort(), spec.getProfile());

        // Resolve thread configuration
        ThreadsSpec threads = resolveThreads(spec);
        int workerThreads = threads.getWorker();

        // Create event loop group
        EventLoopGroup workerGroup = transportFactory.createWorkerGroup(workerThreads);

        // Get codec
        NettyCodec codec = codecRegistry.getDefaultCodec();

        // Create request invoker
        RequestInvoker requestInvoker = new RequestInvoker(spec, codec);

        // Create bootstrap
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(transportFactory.getClientChannelClass())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) spec.getTimeout().getConnectMs())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        pipelineAssembler.assemble(ch.pipeline(), spec, requestInvoker);
                    }
                });

        // Create connection pool
        ConnectionPool connectionPool = new ConnectionPool(spec, bootstrap);

        // Create reconnect manager
        ReconnectManager reconnectManager = new ReconnectManager(spec, bootstrap, connectionPool, scheduledExecutor);
        connectionPool.setReconnectManager(reconnectManager);

        // Create heartbeat manager
        HeartbeatManager heartbeatManager = new HeartbeatManager(spec, connectionPool, requestInvoker, scheduledExecutor);

        // Create runtime
        ClientRuntime runtime = new ClientRuntime(spec, bootstrap, workerGroup,
                connectionPool, reconnectManager, heartbeatManager, requestInvoker);
        runtime.setState(ClientRuntime.ClientState.RUNNING);

        // Start heartbeat if enabled
        if (spec.getHeartbeat().isEnabled()) {
            heartbeatManager.start();
        }

        runtimes.put(spec.getName(), runtime);
        logger.info("Client [{}] started successfully", spec.getName());
    }

    /**
     * Stop a single client.
     */
    private void stopClient(ClientRuntime runtime) {
        logger.info("Stopping client [{}]...", runtime.getName());

        runtime.setState(ClientRuntime.ClientState.STOPPING);

        // Stop heartbeat
        if (runtime.getHeartbeatManager() != null) {
            runtime.getHeartbeatManager().stop();
        }

        // Stop reconnect
        if (runtime.getReconnectManager() != null) {
            runtime.getReconnectManager().stop();
        }

        // Close connection pool
        if (runtime.getConnectionPool() != null) {
            runtime.getConnectionPool().close();
        }

        // Close request invoker
        if (runtime.getRequestInvoker() != null) {
            runtime.getRequestInvoker().close();
        }

        // Shutdown worker group
        if (runtime.getWorkerGroup() != null) {
            runtime.getWorkerGroup().shutdownGracefully();
        }

        runtime.setState(ClientRuntime.ClientState.STOPPED);
        logger.info("Client [{}] stopped", runtime.getName());
    }

    /**
     * Resolve thread configuration, merging defaults with client-specific overrides.
     */
    private ThreadsSpec resolveThreads(ClientSpec spec) {
        DefaultsSpec defaults = properties.getDefaults();
        ThreadsSpec defaultThreads = defaults.getThreads();
        ThreadsSpec clientThreads = spec.getThreads();

        if (clientThreads == null) {
            return defaultThreads;
        }

        ThreadsSpec resolved = new ThreadsSpec();
        resolved.setWorker(clientThreads.getWorker() >= 0 ? clientThreads.getWorker() : defaultThreads.getWorker());
        return resolved;
    }

    /**
     * Get a client runtime by name.
     * @param name the client name
     * @return the runtime, or {@code null} if not found
     */
    public ClientRuntime getRuntime(String name) {
        return runtimes.get(name);
    }

    /**
     * Get all client runtimes.
     * @return an unmodifiable map of runtimes
     */
    public Map<String, ClientRuntime> getAllRuntimes() {
        return Collections.unmodifiableMap(runtimes);
    }

    /**
     * Set whether to fail fast on client startup errors.
     * @param failFast {@code true} to fail fast
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

}
