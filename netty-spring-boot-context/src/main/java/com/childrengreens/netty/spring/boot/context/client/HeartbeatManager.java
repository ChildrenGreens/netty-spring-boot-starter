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
import com.childrengreens.netty.spring.boot.context.properties.HeartbeatSpec;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manager for heartbeat/keep-alive functionality.
 *
 * <p>This manager sends periodic heartbeat messages to keep connections alive
 * and detect dead connections.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see ClientSpec
 * @see HeartbeatSpec
 */
public class HeartbeatManager {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatManager.class);

    private final ClientSpec clientSpec;
    private final ConnectionPool connectionPool;
    private final RequestInvoker requestInvoker;
    private final ScheduledExecutorService scheduler;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private ScheduledFuture<?> heartbeatFuture;
    private HeartbeatListener listener;

    /**
     * Listener for heartbeat events.
     */
    public interface HeartbeatListener {
        /**
         * Called when heartbeat succeeds.
         */
        void onHeartbeatSuccess();

        /**
         * Called when heartbeat fails.
         * @param cause the failure cause
         */
        void onHeartbeatFailure(Throwable cause);

        /**
         * Called when connection is detected as unhealthy.
         */
        void onConnectionUnhealthy();
    }

    /**
     * Create a new HeartbeatManager.
     * @param clientSpec the client specification
     * @param connectionPool the connection pool
     * @param requestInvoker the request invoker for sending heartbeat
     * @param scheduler the scheduler for scheduling heartbeats
     */
    public HeartbeatManager(ClientSpec clientSpec, ConnectionPool connectionPool,
                            RequestInvoker requestInvoker, ScheduledExecutorService scheduler) {
        this.clientSpec = clientSpec;
        this.connectionPool = connectionPool;
        this.requestInvoker = requestInvoker;
        this.scheduler = scheduler;
    }

    /**
     * Set the heartbeat listener.
     * @param listener the listener
     */
    public void setListener(HeartbeatListener listener) {
        this.listener = listener;
    }

    /**
     * Start the heartbeat scheduler.
     */
    public void start() {
        if (!clientSpec.getHeartbeat().isEnabled()) {
            return;
        }

        if (!running.compareAndSet(false, true)) {
            return;
        }

        HeartbeatSpec heartbeatSpec = clientSpec.getHeartbeat();
        long intervalMs = heartbeatSpec.getIntervalMs();

        logger.info("Starting heartbeat for client [{}] with interval {} ms",
                clientSpec.getName(), intervalMs);

        heartbeatFuture = scheduler.scheduleAtFixedRate(
                this::sendHeartbeat,
                intervalMs,
                intervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stop the heartbeat scheduler.
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
            heartbeatFuture = null;
        }

        logger.info("Stopped heartbeat for client [{}]", clientSpec.getName());
    }

    /**
     * Send a heartbeat message.
     */
    private void sendHeartbeat() {
        if (!running.get()) {
            return;
        }

        Channel channel = null;
        try {
            channel = connectionPool.acquire();
            HeartbeatSpec heartbeatSpec = clientSpec.getHeartbeat();

            // Send heartbeat and wait for response
            Object response = requestInvoker.invoke(
                    channel,
                    heartbeatSpec.getResponseType(),
                    heartbeatSpec.getMessage(),
                    heartbeatSpec.getTimeoutMs()
            ).get(heartbeatSpec.getTimeoutMs(), TimeUnit.MILLISECONDS);

            // Heartbeat successful
            consecutiveFailures.set(0);
            logger.debug("Heartbeat successful for client [{}]", clientSpec.getName());

            if (listener != null) {
                listener.onHeartbeatSuccess();
            }

        } catch (Exception e) {
            handleHeartbeatFailure(e);
        } finally {
            if (channel != null) {
                connectionPool.release(channel);
            }
        }
    }

    /**
     * Handle heartbeat failure.
     * @param cause the failure cause
     */
    private void handleHeartbeatFailure(Throwable cause) {
        int failures = consecutiveFailures.incrementAndGet();
        logger.warn("Heartbeat failed for client [{}] (consecutive failures: {}): {}",
                clientSpec.getName(), failures, cause.getMessage());

        if (listener != null) {
            listener.onHeartbeatFailure(cause);
        }

        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            logger.error("Connection unhealthy for client [{}] after {} consecutive heartbeat failures",
                    clientSpec.getName(), failures);

            if (listener != null) {
                listener.onConnectionUnhealthy();
            }

            // Reset counter
            consecutiveFailures.set(0);
        }
    }

    /**
     * Return whether heartbeat is running.
     * @return {@code true} if running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Return the number of consecutive failures.
     * @return the consecutive failures count
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

}
