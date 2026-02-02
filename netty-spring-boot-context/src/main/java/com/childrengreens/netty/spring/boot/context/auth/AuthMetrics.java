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

package com.childrengreens.netty.spring.boot.context.auth;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics for authentication operations.
 *
 * <p>Tracks success, failure, timeout, and kicked connection counts.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class AuthMetrics {

    private final String serverName;
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong timeoutCount = new AtomicLong(0);
    private final AtomicLong kickedCount = new AtomicLong(0);

    /**
     * Create a new AuthMetrics instance.
     *
     * @param serverName the server name
     */
    public AuthMetrics(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Get the server name.
     *
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Increment the success count.
     */
    public void incrementSuccess() {
        successCount.incrementAndGet();
    }

    /**
     * Increment the failure count.
     */
    public void incrementFailure() {
        failureCount.incrementAndGet();
    }

    /**
     * Increment the timeout count.
     */
    public void incrementTimeout() {
        timeoutCount.incrementAndGet();
    }

    /**
     * Increment the kicked count.
     */
    public void incrementKicked() {
        kickedCount.incrementAndGet();
    }

    /**
     * Get the total successful authentications.
     *
     * @return the success count
     */
    public long getSuccessCount() {
        return successCount.get();
    }

    /**
     * Get the total failed authentications.
     *
     * @return the failure count
     */
    public long getFailureCount() {
        return failureCount.get();
    }

    /**
     * Get the total authentication timeouts.
     *
     * @return the timeout count
     */
    public long getTimeoutCount() {
        return timeoutCount.get();
    }

    /**
     * Get the total kicked connections.
     *
     * @return the kicked count
     */
    public long getKickedCount() {
        return kickedCount.get();
    }

    @Override
    public String toString() {
        return "AuthMetrics{" +
                "serverName='" + serverName + '\'' +
                ", successCount=" + successCount +
                ", failureCount=" + failureCount +
                ", timeoutCount=" + timeoutCount +
                ", kickedCount=" + kickedCount +
                '}';
    }

}
