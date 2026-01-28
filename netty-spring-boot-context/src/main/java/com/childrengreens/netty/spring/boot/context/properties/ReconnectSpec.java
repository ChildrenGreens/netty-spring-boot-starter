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
 * Configuration specification for auto-reconnection settings.
 *
 * <p>Example configuration:
 * <pre>{@code
 * reconnect:
 *   enabled: true
 *   initialDelayMs: 1000
 *   maxDelayMs: 30000
 *   multiplier: 2.0
 *   maxRetries: -1
 * }</pre>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public class ReconnectSpec {

    /**
     * Whether auto-reconnection is enabled.
     */
    private boolean enabled = true;

    /**
     * Initial delay in milliseconds before first reconnection attempt.
     */
    private long initialDelayMs = 1000;

    /**
     * Maximum delay in milliseconds between reconnection attempts.
     */
    private long maxDelayMs = 30000;

    /**
     * Multiplier for exponential backoff.
     */
    private double multiplier = 2.0;

    /**
     * Maximum number of reconnection retries. -1 means infinite retries.
     */
    private int maxRetries = -1;

    /**
     * Return whether auto-reconnection is enabled.
     * @return {@code true} if enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Set whether auto-reconnection is enabled.
     * @param enabled {@code true} to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the initial delay in milliseconds.
     * @return the initial delay
     */
    public long getInitialDelayMs() {
        return this.initialDelayMs;
    }

    /**
     * Set the initial delay in milliseconds.
     * @param initialDelayMs the initial delay
     */
    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    /**
     * Return the maximum delay in milliseconds.
     * @return the maximum delay
     */
    public long getMaxDelayMs() {
        return this.maxDelayMs;
    }

    /**
     * Set the maximum delay in milliseconds.
     * @param maxDelayMs the maximum delay
     */
    public void setMaxDelayMs(long maxDelayMs) {
        this.maxDelayMs = maxDelayMs;
    }

    /**
     * Return the multiplier for exponential backoff.
     * @return the multiplier
     */
    public double getMultiplier() {
        return this.multiplier;
    }

    /**
     * Set the multiplier for exponential backoff.
     * @param multiplier the multiplier
     */
    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    /**
     * Return the maximum number of retries.
     * @return the maximum retries, -1 for infinite
     */
    public int getMaxRetries() {
        return this.maxRetries;
    }

    /**
     * Set the maximum number of retries.
     * @param maxRetries the maximum retries, -1 for infinite
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

}
