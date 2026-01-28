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

import com.childrengreens.netty.spring.boot.context.codec.NettyCodec;
import com.childrengreens.netty.spring.boot.context.properties.ClientSpec;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Invoker for sending requests and matching responses.
 *
 * <p>This class manages the request-response correlation using correlation IDs,
 * handles timeouts, and provides both synchronous and asynchronous invocation.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see ResponseFuture
 * @see ClientSpec
 */
public class RequestInvoker {

    private static final Logger logger = LoggerFactory.getLogger(RequestInvoker.class);

    /**
     * Header name for correlation ID.
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /**
     * Header name for message type.
     */
    public static final String MESSAGE_TYPE_HEADER = "type";

    private final ClientSpec clientSpec;
    private final NettyCodec codec;
    private final ConcurrentMap<String, ResponseFuture<Object>> pendingRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeoutScheduler;

    /**
     * Create a new RequestInvoker.
     * @param clientSpec the client specification
     * @param codec the codec for serialization
     */
    public RequestInvoker(ClientSpec clientSpec, NettyCodec codec) {
        this.clientSpec = clientSpec;
        this.codec = codec;
        this.timeoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "request-timeout-" + clientSpec.getName());
            t.setDaemon(true);
            return t;
        });

        // Start timeout checker
        startTimeoutChecker();
    }

    /**
     * Invoke a request asynchronously.
     * @param channel the channel to use
     * @param messageType the message type
     * @param payload the request payload
     * @param timeoutMs the timeout in milliseconds (0 for default)
     * @return a CompletableFuture for the response
     */
    public CompletableFuture<Object> invoke(Channel channel, String messageType, Object payload, long timeoutMs) {
        String correlationId = generateCorrelationId();
        long actualTimeout = timeoutMs > 0 ? timeoutMs : clientSpec.getTimeout().getRequestMs();

        ResponseFuture<Object> responseFuture = new ResponseFuture<>(correlationId, actualTimeout);
        pendingRequests.put(correlationId, responseFuture);

        try {
            // Build request message
            Map<String, Object> request = buildRequest(messageType, correlationId, payload);

            // Send request
            channel.writeAndFlush(request).addListener(future -> {
                if (!future.isSuccess()) {
                    responseFuture.completeExceptionally(future.cause());
                    pendingRequests.remove(correlationId);
                } else {
                    logger.debug("Request sent: type={}, correlationId={}", messageType, correlationId);
                }
            });

        } catch (Exception e) {
            responseFuture.completeExceptionally(e);
            pendingRequests.remove(correlationId);
        }

        return responseFuture.toCompletableFuture();
    }

    /**
     * Invoke a one-way request (no response expected).
     * @param channel the channel to use
     * @param messageType the message type
     * @param payload the request payload
     */
    public void invokeOneWay(Channel channel, String messageType, Object payload) {
        try {
            Map<String, Object> request = buildRequest(messageType, null, payload);
            channel.writeAndFlush(request);
            logger.debug("One-way request sent: type={}", messageType);
        } catch (Exception e) {
            logger.error("Failed to send one-way request: type={}", messageType, e);
        }
    }

    /**
     * Build a request message.
     * @param messageType the message type
     * @param correlationId the correlation ID (may be null for one-way)
     * @param payload the payload
     * @return the request message
     */
    private Map<String, Object> buildRequest(String messageType, String correlationId, Object payload) {
        Map<String, Object> request = new HashMap<>();
        request.put(MESSAGE_TYPE_HEADER, messageType);

        if (correlationId != null) {
            request.put(CORRELATION_ID_HEADER, correlationId);
        }

        if (payload != null) {
            if (payload instanceof Map) {
                request.putAll((Map<String, Object>) payload);
            } else if (payload instanceof String) {
                request.put("data", payload);
            } else {
                // Serialize complex objects
                request.put("data", payload);
            }
        }

        return request;
    }

    /**
     * Complete a pending request with a response.
     * @param correlationId the correlation ID
     * @param response the response
     * @return {@code true} if a matching request was found
     */
    public boolean completeRequest(String correlationId, Object response) {
        ResponseFuture<Object> future = pendingRequests.remove(correlationId);
        if (future != null) {
            boolean completed = future.complete(response);
            logger.debug("Request completed: correlationId={}", correlationId);
            return completed;
        }
        logger.debug("No pending request found for correlationId={}", correlationId);
        return false;
    }

    /**
     * Fail a pending request.
     * @param correlationId the correlation ID
     * @param cause the failure cause
     * @return {@code true} if a matching request was found
     */
    public boolean failRequest(String correlationId, Throwable cause) {
        ResponseFuture<Object> future = pendingRequests.remove(correlationId);
        if (future != null) {
            return future.completeExceptionally(cause);
        }
        return false;
    }

    /**
     * Generate a unique correlation ID.
     * @return the correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Start the timeout checker task.
     */
    private void startTimeoutChecker() {
        timeoutScheduler.scheduleAtFixedRate(() -> {
            pendingRequests.forEach((correlationId, future) -> {
                if (future.cancelIfExpired()) {
                    pendingRequests.remove(correlationId);
                    logger.debug("Request timed out: correlationId={}", correlationId);
                }
            });
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Return the number of pending requests.
     * @return the pending request count
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    /**
     * Close the invoker and cancel all pending requests.
     */
    public void close() {
        timeoutScheduler.shutdown();

        // Cancel all pending requests
        pendingRequests.forEach((correlationId, future) -> {
            future.completeExceptionally(new CancellationException("Request invoker closed"));
        });
        pendingRequests.clear();

        logger.info("Request invoker closed for client [{}]", clientSpec.getName());
    }

}
