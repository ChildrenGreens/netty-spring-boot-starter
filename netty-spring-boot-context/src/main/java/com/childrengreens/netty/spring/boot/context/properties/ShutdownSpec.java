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
 * Graceful shutdown configuration for Netty servers.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 */
public class ShutdownSpec {

    /**
     * Whether to enable graceful shutdown.
     */
    private boolean graceful = true;

    /**
     * Quiet period in milliseconds before shutdown.
     */
    private long quietPeriodMs = 200;

    /**
     * Maximum timeout in milliseconds for shutdown.
     */
    private long timeoutMs = 3000;

    /**
     * Return whether graceful shutdown is enabled.
     * @return {@code true} if graceful shutdown is enabled
     */
    public boolean isGraceful() {
        return this.graceful;
    }

    /**
     * Set whether to enable graceful shutdown.
     * @param graceful {@code true} to enable graceful shutdown
     */
    public void setGraceful(boolean graceful) {
        this.graceful = graceful;
    }

    /**
     * Return the quiet period in milliseconds.
     * @return the quiet period
     */
    public long getQuietPeriodMs() {
        return this.quietPeriodMs;
    }

    /**
     * Set the quiet period in milliseconds.
     * @param quietPeriodMs the quiet period
     */
    public void setQuietPeriodMs(long quietPeriodMs) {
        this.quietPeriodMs = quietPeriodMs;
    }

    /**
     * Return the shutdown timeout in milliseconds.
     * @return the timeout
     */
    public long getTimeoutMs() {
        return this.timeoutMs;
    }

    /**
     * Set the shutdown timeout in milliseconds.
     * @param timeoutMs the timeout
     */
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

}
