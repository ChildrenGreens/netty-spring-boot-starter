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

package com.childrengreens.netty.spring.boot.context.server;

import com.childrengreens.netty.spring.boot.context.pipeline.PipelineAssembler;
import com.childrengreens.netty.spring.boot.context.properties.*;
import com.childrengreens.netty.spring.boot.context.transport.TransportFactory;
import com.childrengreens.netty.spring.boot.context.transport.TransportStarter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrator for managing multiple Netty server instances.
 *
 * <p>This class is responsible for:
 * <ul>
 * <li>Reading server specifications from configuration</li>
 * <li>Creating and starting server instances</li>
 * <li>Managing server lifecycle (start, stop)</li>
 * <li>Handling graceful shutdown</li>
 * </ul>
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 * @see ServerRuntime
 * @see NettyProperties
 */
public class NettyServerOrchestrator implements InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(NettyServerOrchestrator.class);

    private final NettyProperties properties;
    private final TransportFactory transportFactory;
    private final PipelineAssembler pipelineAssembler;
    private final Map<String, ServerRuntime> runtimes = new ConcurrentHashMap<>();

    private boolean failFast = true;

    /**
     * Create a new NettyServerOrchestrator.
     * @param properties the Netty properties
     * @param transportFactory the transport factory
     * @param pipelineAssembler the pipeline assembler
     */
    public NettyServerOrchestrator(NettyProperties properties, TransportFactory transportFactory,
                                    PipelineAssembler pipelineAssembler) {
        this.properties = properties;
        this.transportFactory = transportFactory;
        this.pipelineAssembler = pipelineAssembler;
    }

    @Override
    public void destroy() throws Exception {
        stop();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    /**
     * Start all configured servers.
     */
    public void start() {
        if (!properties.isEnabled()) {
            logger.info("Netty is disabled, skipping server startup");
            return;
        }

        for (ServerSpec spec : properties.getServers()) {
            try {
                startServer(spec);
            } catch (Exception e) {
                logger.error("Failed to start server [{}]", spec.getName(), e);
                if (failFast) {
                    throw new IllegalStateException("Failed to start server: " + spec.getName(), e);
                }
            }
        }

        logger.info("Started {} Netty server(s)", runtimes.size());
    }

    /**
     * Stop all running servers.
     */
    public void stop() {
        logger.info("Stopping {} Netty server(s)...", runtimes.size());

        ShutdownSpec shutdown = properties.getDefaults().getShutdown();
        for (ServerRuntime runtime : runtimes.values()) {
            runtime.stop(shutdown);
        }

        runtimes.clear();
        logger.info("All Netty servers stopped");
    }

    /**
     * Start a single server.
     */
    private void startServer(ServerSpec spec) throws Exception {
        logger.info("Starting server [{}] on {}:{} with profile [{}]",
                spec.getName(), spec.getHost(), spec.getPort(), spec.getProfile());

        // Resolve thread configuration
        ThreadsSpec threads = resolveThreads(spec);
        int bossThreads = threads.getBoss();
        int workerThreads = threads.getWorker();

        // Create event loop groups
        EventLoopGroup bossGroup = transportFactory.createBossGroup(bossThreads);
        EventLoopGroup workerGroup = transportFactory.createWorkerGroup(workerThreads);

        // Create channel initializer
        ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                pipelineAssembler.assemble(ch.pipeline(), spec);
            }
        };

        // Start transport
        TransportStarter starter = transportFactory.getTransportStarter(spec.getTransport());
        ServerRuntime runtime = starter.start(spec, bossGroup, workerGroup, initializer);

        runtimes.put(spec.getName(), runtime);
    }

    /**
     * Resolve thread configuration, merging defaults with server-specific overrides.
     */
    private ThreadsSpec resolveThreads(ServerSpec spec) {
        DefaultsSpec defaults = properties.getDefaults();
        ThreadsSpec defaultThreads = defaults.getThreads();
        ThreadsSpec serverThreads = spec.getThreads();

        if (serverThreads == null) {
            return defaultThreads;
        }

        ThreadsSpec resolved = new ThreadsSpec();
        resolved.setBoss(serverThreads.getBoss() > 0 ? serverThreads.getBoss() : defaultThreads.getBoss());
        resolved.setWorker(serverThreads.getWorker() >= 0 ? serverThreads.getWorker() : defaultThreads.getWorker());
        return resolved;
    }

    /**
     * Get a server runtime by name.
     * @param name the server name
     * @return the runtime, or {@code null} if not found
     */
    public ServerRuntime getRuntime(String name) {
        return runtimes.get(name);
    }

    /**
     * Get all server runtimes.
     * @return an unmodifiable map of runtimes
     */
    public Map<String, ServerRuntime> getAllRuntimes() {
        return Collections.unmodifiableMap(runtimes);
    }

    /**
     * Set whether to fail fast on server startup errors.
     * @param failFast {@code true} to fail fast
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

}
