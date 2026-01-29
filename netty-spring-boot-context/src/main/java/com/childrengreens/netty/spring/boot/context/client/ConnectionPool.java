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

import com.childrengreens.netty.spring.boot.context.properties.ClientSpec;
import com.childrengreens.netty.spring.boot.context.properties.PoolSpec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connection pool for managing Netty channel connections.
 *
 * <p>This pool maintains a fixed number of connections to a remote server,
 * supports connection validation, and provides automatic connection creation.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see ClientSpec
 * @see PoolSpec
 */
public class ConnectionPool {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);

    private final ClientSpec clientSpec;
    private final Bootstrap bootstrap;
    private final BlockingQueue<Channel> idleChannels;
    private final Set<Channel> borrowedChannels = ConcurrentHashMap.newKeySet();
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler;

    private ReconnectManager reconnectManager;

    /**
     * Create a new ConnectionPool.
     * @param clientSpec the client specification
     * @param bootstrap the bootstrap for creating connections
     */
    public ConnectionPool(ClientSpec clientSpec, Bootstrap bootstrap) {
        this.clientSpec = clientSpec;
        this.bootstrap = bootstrap;
        this.idleChannels = new LinkedBlockingQueue<>(clientSpec.getPool().getMaxConnections());
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pool-maintenance-" + clientSpec.getName());
            t.setDaemon(true);
            return t;
        });

        // Start maintenance task
        startMaintenanceTask();
    }

    /**
     * Set the reconnect manager for handling disconnected channels.
     * @param reconnectManager the reconnect manager
     */
    public void setReconnectManager(ReconnectManager reconnectManager) {
        this.reconnectManager = reconnectManager;
    }

    /**
     * Acquire a channel from the pool.
     * @return the acquired channel
     * @throws Exception if unable to acquire a channel
     */
    public Channel acquire() throws Exception {
        if (closed.get()) {
            throw new IllegalStateException("Connection pool is closed");
        }

        PoolSpec poolSpec = clientSpec.getPool();

        // Try to get an idle channel
        Channel channel = idleChannels.poll();

        if (channel != null) {
            if (isChannelHealthy(channel)) {
                borrowedChannels.add(channel);
                logger.debug("Acquired existing channel from pool: {}", channel);
                return channel;
            } else {
                // Channel is not healthy, close it and try to create a new one
                closeChannel(channel);
            }
        }

        // Try to create a new channel if under max limit
        if (totalConnections.get() < poolSpec.getMaxConnections()) {
            channel = createChannel();
            borrowedChannels.add(channel);
            return channel;
        }

        // Wait for an available channel
        channel = idleChannels.poll(poolSpec.getAcquireTimeoutMs(), TimeUnit.MILLISECONDS);
        if (channel != null) {
            if (isChannelHealthy(channel)) {
                borrowedChannels.add(channel);
                return channel;
            } else {
                closeChannel(channel);
                channel = createChannel();
                borrowedChannels.add(channel);
                return channel;
            }
        }

        throw new TimeoutException("Timeout waiting for available connection");
    }

    /**
     * Return a channel to the pool.
     * @param channel the channel to return
     */
    public void release(Channel channel) {
        if (channel == null) {
            return;
        }

        // Remove from borrowed set
        borrowedChannels.remove(channel);

        if (closed.get()) {
            closeChannel(channel);
            return;
        }

        if (isChannelHealthy(channel)) {
            boolean offered = idleChannels.offer(channel);
            if (!offered) {
                // Pool is full, close the channel
                closeChannel(channel);
            } else {
                logger.debug("Released channel back to pool: {}", channel);
            }
        } else {
            closeChannel(channel);
            // Trigger reconnection if enabled
            if (reconnectManager != null && clientSpec.getReconnect().isEnabled()) {
                reconnectManager.scheduleReconnect();
            }
        }
    }

    /**
     * Create a new channel.
     * @return the created channel
     * @throws Exception if channel creation fails
     */
    private Channel createChannel() throws Exception {
        int current = totalConnections.incrementAndGet();

        // Check if pool was closed after increment
        if (closed.get()) {
            totalConnections.decrementAndGet();
            throw new IllegalStateException("Connection pool is closed");
        }

        if (current > clientSpec.getPool().getMaxConnections()) {
            totalConnections.decrementAndGet();
            throw new IllegalStateException("Max connections reached");
        }

        ChannelFuture future = null;
        try {
            future = bootstrap.connect(clientSpec.getHost(), clientSpec.getPort());
            boolean completed = future.await(clientSpec.getTimeout().getConnectMs(), TimeUnit.MILLISECONDS);

            if (!completed) {
                // Timeout: cancel the connection to prevent orphan channels
                future.cancel(true);
                totalConnections.decrementAndGet();
                throw new TimeoutException("Connection timeout to " + clientSpec.getHost() + ":" + clientSpec.getPort());
            }

            if (future.isSuccess()) {
                Channel channel = future.channel();

                // Check again if pool was closed during connection
                if (closed.get()) {
                    channel.close();
                    totalConnections.decrementAndGet();
                    throw new IllegalStateException("Connection pool is closed");
                }

                logger.info("Created new connection to {}:{}", clientSpec.getHost(), clientSpec.getPort());
                return channel;
            } else {
                totalConnections.decrementAndGet();
                throw new Exception("Failed to connect to " + clientSpec.getHost() + ":" + clientSpec.getPort(),
                        future.cause());
            }
        } catch (InterruptedException e) {
            // Cancel pending connection on interrupt
            future.cancel(true);
            totalConnections.decrementAndGet();
            Thread.currentThread().interrupt();
            throw new Exception("Connection interrupted", e);
        } catch (RuntimeException e) {
            // Ensure connection count is correct and close any established connection
            if (future != null && future.isSuccess()) {
                future.channel().close();
            }
            totalConnections.decrementAndGet();
            throw e;
        }
    }

    /**
     * Check if a channel is healthy.
     * @param channel the channel to check
     * @return {@code true} if the channel is healthy
     */
    private boolean isChannelHealthy(Channel channel) {
        return channel != null && channel.isActive() && channel.isOpen();
    }

    /**
     * Close a channel and decrement the connection count.
     * @param channel the channel to close
     */
    private void closeChannel(Channel channel) {
        if (channel != null) {
            totalConnections.decrementAndGet();
            channel.close();
            logger.debug("Closed channel: {}", channel);
        }
    }

    /**
     * Start the maintenance task for managing idle connections.
     */
    private void startMaintenanceTask() {
        PoolSpec poolSpec = clientSpec.getPool();
        long interval = poolSpec.getMaxIdleMs() / 2;

        scheduler.scheduleAtFixedRate(() -> {
            if (closed.get()) {
                return;
            }

            // Check all idle connections and close unhealthy ones
            int size = idleChannels.size();
            for (int i = 0; i < size; i++) {
                Channel channel = idleChannels.poll();
                if (channel == null) {
                    break;
                }
                if (isChannelHealthy(channel)) {
                    idleChannels.offer(channel);
                } else {
                    closeChannel(channel);
                }
            }

            // Ensure minimum idle connections
            ensureMinIdle();

        }, interval, interval, TimeUnit.MILLISECONDS);
    }

    /**
     * Ensure minimum idle connections are maintained.
     */
    private void ensureMinIdle() {
        PoolSpec poolSpec = clientSpec.getPool();
        int currentIdle = idleChannels.size();
        int toCreate = poolSpec.getMinIdle() - currentIdle;

        for (int i = 0; i < toCreate && totalConnections.get() < poolSpec.getMaxConnections(); i++) {
            try {
                Channel channel = createChannel();
                release(channel);
            } catch (Exception e) {
                logger.warn("Failed to create idle connection: {}", e.getMessage());
                break;
            }
        }
    }

    /**
     * Return the number of total connections.
     * @return the total connections
     */
    public int getTotalConnections() {
        return totalConnections.get();
    }

    /**
     * Return the number of idle connections.
     * @return the idle connections
     */
    public int getIdleConnections() {
        return idleChannels.size();
    }

    /**
     * Return the number of borrowed (in-use) connections.
     * @return the borrowed connections
     */
    public int getBorrowedConnections() {
        return borrowedChannels.size();
    }

    /**
     * Close the connection pool.
     */
    public void close() {
        if (closed.compareAndSet(false, true)) {
            scheduler.shutdown();

            // Close idle connections (closeChannel will decrement totalConnections)
            Channel channel;
            while ((channel = idleChannels.poll()) != null) {
                closeChannel(channel);
            }

            // Close borrowed connections
            int borrowedCount = borrowedChannels.size();
            if (borrowedCount > 0) {
                logger.warn("Closing {} borrowed connection(s) for client [{}]",
                        borrowedCount, clientSpec.getName());
            }
            for (Channel borrowed : borrowedChannels) {
                closeChannel(borrowed);
            }
            borrowedChannels.clear();

            // Do not reset totalConnections to 0 - let it naturally reach 0
            // through closeChannel decrements to avoid race conditions with in-flight connections

            logger.info("Connection pool closed for client [{}]", clientSpec.getName());
        }
    }

}
