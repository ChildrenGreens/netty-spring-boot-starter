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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Future for async response handling with timeout support.
 *
 * <p>This class wraps a request-response correlation, allowing callers
 * to wait for responses asynchronously with configurable timeouts.
 *
 * @param <T> the response type
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class ResponseFuture<T> {

    private final String correlationId;
    private final long timeoutMs;
    private final long createTime;
    private final CompletableFuture<T> future;
    private final AtomicBoolean completed = new AtomicBoolean(false);

    /**
     * Create a new ResponseFuture.
     * @param correlationId the correlation ID for matching request/response
     * @param timeoutMs the timeout in milliseconds
     */
    public ResponseFuture(String correlationId, long timeoutMs) {
        this.correlationId = correlationId;
        this.timeoutMs = timeoutMs;
        this.createTime = System.currentTimeMillis();
        this.future = new CompletableFuture<>();
    }

    /**
     * Return the correlation ID.
     * @return the correlation ID
     */
    public String getCorrelationId() {
        return this.correlationId;
    }

    /**
     * Return the timeout in milliseconds.
     * @return the timeout
     */
    public long getTimeoutMs() {
        return this.timeoutMs;
    }

    /**
     * Return the creation time.
     * @return the creation time in milliseconds
     */
    public long getCreateTime() {
        return this.createTime;
    }

    /**
     * Complete the future with a response.
     * @param response the response
     * @return {@code true} if successfully completed
     */
    public boolean complete(T response) {
        if (completed.compareAndSet(false, true)) {
            return future.complete(response);
        }
        return false;
    }

    /**
     * Complete the future exceptionally.
     * @param cause the exception cause
     * @return {@code true} if successfully completed
     */
    public boolean completeExceptionally(Throwable cause) {
        if (completed.compareAndSet(false, true)) {
            return future.completeExceptionally(cause);
        }
        return false;
    }

    /**
     * Check if this future is expired.
     * @return {@code true} if expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - createTime > timeoutMs;
    }

    /**
     * Check if this future is completed.
     * @return {@code true} if completed
     */
    public boolean isDone() {
        return future.isDone();
    }

    /**
     * Get the result, blocking until available.
     * @return the result
     * @throws Exception if an error occurs
     */
    public T get() throws Exception {
        return future.get();
    }

    /**
     * Get the result with timeout.
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @return the result
     * @throws Exception if an error occurs or timeout
     */
    public T get(long timeout, TimeUnit unit) throws Exception {
        return future.get(timeout, unit);
    }

    /**
     * Return the underlying CompletableFuture.
     * @return the CompletableFuture
     */
    public CompletableFuture<T> toCompletableFuture() {
        return future;
    }

    /**
     * Cancel this future if expired.
     * @return {@code true} if cancelled
     */
    public boolean cancelIfExpired() {
        if (isExpired() && !future.isDone()) {
            return completeExceptionally(new TimeoutException(
                    "Request timeout after " + timeoutMs + " ms, correlationId=" + correlationId));
        }
        return false;
    }

}
