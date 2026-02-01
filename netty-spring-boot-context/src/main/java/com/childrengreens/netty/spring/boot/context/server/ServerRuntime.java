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

import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureMetrics;
import com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import com.childrengreens.netty.spring.boot.context.properties.ShutdownSpec;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Runtime representation of a running Netty server.
 *
 * <p>Encapsulates the server's event loop groups, bind channel, and state.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class ServerRuntime {

    private static final Logger logger = LoggerFactory.getLogger(ServerRuntime.class);

    private final ServerSpec spec;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final Channel bindChannel;
    private final ServerMetrics metrics;
    private final BackpressureMetrics backpressureMetrics;
    private volatile ServerState state;

    /**
     * Create a new ServerRuntime.
     * @param spec the server specification
     * @param bossGroup the boss event loop group
     * @param workerGroup the worker event loop group
     * @param bindChannel the bound server channel
     * @param state the initial state
     */
    public ServerRuntime(ServerSpec spec, EventLoopGroup bossGroup,
                         EventLoopGroup workerGroup, Channel bindChannel, ServerState state) {
        this(spec, bossGroup, workerGroup, bindChannel, state, new ServerMetrics(spec.getName()), null);
    }

    /**
     * Create a new ServerRuntime with metrics.
     * @param spec the server specification
     * @param bossGroup the boss event loop group
     * @param workerGroup the worker event loop group
     * @param bindChannel the bound server channel
     * @param state the initial state
     * @param metrics the server metrics
     * @since 0.0.2
     */
    public ServerRuntime(ServerSpec spec, EventLoopGroup bossGroup,
                         EventLoopGroup workerGroup, Channel bindChannel, ServerState state,
                         ServerMetrics metrics) {
        this(spec, bossGroup, workerGroup, bindChannel, state, metrics, null);
    }

    /**
     * Create a new ServerRuntime with metrics and backpressure metrics.
     * @param spec the server specification
     * @param bossGroup the boss event loop group
     * @param workerGroup the worker event loop group
     * @param bindChannel the bound server channel
     * @param state the initial state
     * @param metrics the server metrics
     * @param backpressureMetrics the backpressure metrics (may be null)
     * @since 0.0.2
     */
    public ServerRuntime(ServerSpec spec, EventLoopGroup bossGroup,
                         EventLoopGroup workerGroup, Channel bindChannel, ServerState state,
                         ServerMetrics metrics, @Nullable BackpressureMetrics backpressureMetrics) {
        this.spec = spec;
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.bindChannel = bindChannel;
        this.state = state;
        this.metrics = metrics;
        this.backpressureMetrics = backpressureMetrics;
    }

    /**
     * Return the server specification.
     * @return the spec
     */
    public ServerSpec getSpec() {
        return this.spec;
    }

    /**
     * Return the boss event loop group.
     * @return the boss group
     */
    public EventLoopGroup getBossGroup() {
        return this.bossGroup;
    }

    /**
     * Return the worker event loop group.
     * @return the worker group
     */
    public EventLoopGroup getWorkerGroup() {
        return this.workerGroup;
    }

    /**
     * Return the bound server channel.
     * @return the bind channel
     */
    public Channel getBindChannel() {
        return this.bindChannel;
    }

    /**
     * Return the server metrics.
     * @return the metrics
     * @since 0.0.2
     */
    public ServerMetrics getMetrics() {
        return this.metrics;
    }

    /**
     * Return the backpressure metrics.
     * @return the backpressure metrics, or {@code null} if backpressure is not enabled
     * @since 0.0.2
     */
    @Nullable
    public BackpressureMetrics getBackpressureMetrics() {
        return this.backpressureMetrics;
    }

    /**
     * Return the current server state.
     * @return the state
     */
    public ServerState getState() {
        return this.state;
    }

    /**
     * Set the server state.
     * @param state the new state
     */
    public void setState(ServerState state) {
        this.state = state;
    }

    /**
     * Return whether the server is running.
     * @return {@code true} if running
     */
    public boolean isRunning() {
        return this.state == ServerState.RUNNING;
    }

    /**
     * Stop the server gracefully.
     * @param shutdown the shutdown configuration
     */
    public void stop(ShutdownSpec shutdown) {
        if (this.state == ServerState.STOPPED || this.state == ServerState.STOPPING) {
            return;
        }

        this.state = ServerState.STOPPING;
        String serverName = spec.getName();
        logger.info("Stopping server [{}]...", serverName);

        try {
            // Close the server channel first
            if (bindChannel != null && bindChannel.isOpen()) {
                bindChannel.close().sync();
            }

            long quietPeriod = shutdown.isGraceful() ? shutdown.getQuietPeriodMs() : 0;
            long timeout = shutdown.getTimeoutMs();

            // Shutdown worker group
            if (workerGroup != null && !workerGroup.isShutdown()) {
                workerGroup.shutdownGracefully(quietPeriod, timeout, TimeUnit.MILLISECONDS).sync();
            }

            // Shutdown boss group
            if (bossGroup != null && !bossGroup.isShutdown()) {
                bossGroup.shutdownGracefully(quietPeriod, timeout, TimeUnit.MILLISECONDS).sync();
            }

            this.state = ServerState.STOPPED;
            logger.info("Server [{}] stopped", serverName);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while stopping server [{}]", serverName, e);
            this.state = ServerState.FAILED;
        } catch (Exception e) {
            logger.error("Error stopping server [{}]", serverName, e);
            this.state = ServerState.FAILED;
        }
    }

}
