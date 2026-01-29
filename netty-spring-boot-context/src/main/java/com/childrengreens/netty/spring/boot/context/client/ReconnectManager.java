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
import com.childrengreens.netty.spring.boot.context.properties.ReconnectSpec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manager for auto-reconnection with exponential backoff.
 *
 * <p>This manager handles reconnection attempts when connections are lost,
 * using an exponential backoff strategy to avoid overwhelming the server.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see ClientSpec
 * @see ReconnectSpec
 */
public class ReconnectManager {

    private static final Logger logger = LoggerFactory.getLogger(ReconnectManager.class);

    private final ClientSpec clientSpec;
    private final Bootstrap bootstrap;
    private final ConnectionPool connectionPool;
    private final ScheduledExecutorService scheduler;

    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private final AtomicLong currentDelay = new AtomicLong(0);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private ScheduledFuture<?> reconnectFuture;
    private ReconnectListener listener;

    /**
     * Listener for reconnection events.
     */
    public interface ReconnectListener {
        /**
         * Called when reconnection is successful.
         * @param channel the new channel
         */
        void onReconnectSuccess(Channel channel);

        /**
         * Called when reconnection fails.
         * @param cause the failure cause
         */
        void onReconnectFailure(Throwable cause);

        /**
         * Called when maximum retries are exhausted.
         */
        void onReconnectExhausted();
    }

    /**
     * Create a new ReconnectManager.
     * @param clientSpec the client specification
     * @param bootstrap the bootstrap for creating connections
     * @param connectionPool the connection pool
     * @param scheduler the scheduler for scheduling reconnects
     */
    public ReconnectManager(ClientSpec clientSpec, Bootstrap bootstrap,
                            ConnectionPool connectionPool, ScheduledExecutorService scheduler) {
        this.clientSpec = clientSpec;
        this.bootstrap = bootstrap;
        this.connectionPool = connectionPool;
        this.scheduler = scheduler;
        this.currentDelay.set(clientSpec.getReconnect().getInitialDelayMs());
    }

    /**
     * Set the reconnection listener.
     * @param listener the listener
     */
    public void setListener(ReconnectListener listener) {
        this.listener = listener;
    }

    /**
     * Schedule a reconnection attempt.
     */
    public void scheduleReconnect() {
        if (!clientSpec.getReconnect().isEnabled() || stopped.get()) {
            return;
        }

        if (!reconnecting.compareAndSet(false, true)) {
            // Already reconnecting
            return;
        }

        ReconnectSpec reconnectSpec = clientSpec.getReconnect();
        long delay = currentDelay.get();

        logger.info("Scheduling reconnection for client [{}] in {} ms (attempt {})",
                clientSpec.getName(), delay, retryCount.get() + 1);

        reconnectFuture = scheduler.schedule(this::doReconnect, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Perform the reconnection attempt.
     */
    private void doReconnect() {
        if (stopped.get()) {
            reconnecting.set(false);
            return;
        }

        ReconnectSpec reconnectSpec = clientSpec.getReconnect();
        int maxRetries = reconnectSpec.getMaxRetries();
        int currentRetry = retryCount.incrementAndGet();

        // Check if max retries exhausted
        if (maxRetries >= 0 && currentRetry > maxRetries) {
            logger.error("Max reconnection retries ({}) exhausted for client [{}]",
                    maxRetries, clientSpec.getName());
            reconnecting.set(false);
            if (listener != null) {
                listener.onReconnectExhausted();
            }
            return;
        }

        logger.info("Attempting reconnection for client [{}] (attempt {}/{})",
                clientSpec.getName(), currentRetry, maxRetries < 0 ? "∞" : maxRetries);

        try {
            ChannelFuture future = bootstrap.connect(clientSpec.getHost(), clientSpec.getPort());
            future.await(clientSpec.getTimeout().getConnectMs(), TimeUnit.MILLISECONDS);

            if (future.isSuccess()) {
                Channel channel = future.channel();
                logger.info("Reconnection successful for client [{}]", clientSpec.getName());

                // Reset retry count and delay
                resetState();
                reconnecting.set(false);

                // Release to pool
                connectionPool.release(channel);

                if (listener != null) {
                    listener.onReconnectSuccess(channel);
                }
            } else {
                handleReconnectFailure(future.cause());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            reconnecting.set(false);
        } catch (Exception e) {
            handleReconnectFailure(e);
        }
    }

    /**
     * Handle reconnection failure.
     * @param cause the failure cause
     */
    private void handleReconnectFailure(Throwable cause) {
        logger.warn("Reconnection failed for client [{}]: {}",
                clientSpec.getName(), cause != null ? cause.getMessage() : "unknown");

        if (listener != null) {
            listener.onReconnectFailure(cause);
        }

        // Calculate next delay with exponential backoff
        ReconnectSpec reconnectSpec = clientSpec.getReconnect();
        long nextDelay = (long) (currentDelay.get() * reconnectSpec.getMultiplier());
        currentDelay.set(Math.min(nextDelay, reconnectSpec.getMaxDelayMs()));

        reconnecting.set(false);

        // Schedule next attempt
        scheduleReconnect();
    }

    /**
     * Reset the reconnection state.
     */
    public void resetState() {
        retryCount.set(0);
        currentDelay.set(clientSpec.getReconnect().getInitialDelayMs());
    }

    /**
     * Stop all reconnection attempts.
     */
    public void stop() {
        stopped.set(true);
        if (reconnectFuture != null) {
            reconnectFuture.cancel(true);
        }
        reconnecting.set(false);
    }

    /**
     * Return whether reconnection is in progress.
     * @return {@code true} if reconnecting
     */
    public boolean isReconnecting() {
        return reconnecting.get();
    }

    /**
     * Return the current retry count.
     * @return the retry count
     */
    public int getRetryCount() {
        return retryCount.get();
    }

}
