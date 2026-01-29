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

package com.childrengreens.netty.spring.boot.context.properties;

/**
 * Configuration specification for connection pool settings.
 *
 * <p>Example configuration:
 * <pre>{@code
 * pool:
 *   maxConnections: 10
 *   minIdle: 2
 *   maxIdleMs: 60000
 *   acquireTimeoutMs: 5000
 * }</pre>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class PoolSpec {

    /**
     * Maximum number of connections in the pool.
     */
    private int maxConnections = 10;

    /**
     * Minimum number of idle connections to maintain.
     */
    private int minIdle = 2;

    /**
     * Maximum idle time in milliseconds before a connection is closed.
     */
    private long maxIdleMs = 60000;

    /**
     * Timeout in milliseconds for acquiring a connection from the pool.
     */
    private long acquireTimeoutMs = 5000;

    /**
     * Whether to validate connections before use.
     */
    private boolean testOnBorrow = true;

    /**
     * Return the maximum number of connections.
     * @return the maximum connections
     */
    public int getMaxConnections() {
        return this.maxConnections;
    }

    /**
     * Set the maximum number of connections.
     * @param maxConnections the maximum connections
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * Return the minimum idle connections.
     * @return the minimum idle connections
     */
    public int getMinIdle() {
        return this.minIdle;
    }

    /**
     * Set the minimum idle connections.
     * @param minIdle the minimum idle connections
     */
    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    /**
     * Return the maximum idle time in milliseconds.
     * @return the maximum idle time
     */
    public long getMaxIdleMs() {
        return this.maxIdleMs;
    }

    /**
     * Set the maximum idle time in milliseconds.
     * @param maxIdleMs the maximum idle time
     */
    public void setMaxIdleMs(long maxIdleMs) {
        this.maxIdleMs = maxIdleMs;
    }

    /**
     * Return the acquire timeout in milliseconds.
     * @return the acquire timeout
     */
    public long getAcquireTimeoutMs() {
        return this.acquireTimeoutMs;
    }

    /**
     * Set the acquire timeout in milliseconds.
     * @param acquireTimeoutMs the acquire timeout
     */
    public void setAcquireTimeoutMs(long acquireTimeoutMs) {
        this.acquireTimeoutMs = acquireTimeoutMs;
    }

    /**
     * Return whether to test connections on borrow.
     * @return {@code true} if testing on borrow
     */
    public boolean isTestOnBorrow() {
        return this.testOnBorrow;
    }

    /**
     * Set whether to test connections on borrow.
     * @param testOnBorrow {@code true} to test on borrow
     */
    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

}
