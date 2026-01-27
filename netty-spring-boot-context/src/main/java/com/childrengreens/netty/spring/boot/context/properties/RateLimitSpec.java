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
 * Rate limiting configuration.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public class RateLimitSpec {

    /**
     * Whether rate limiting is enabled.
     */
    private boolean enabled = false;

    /**
     * Maximum requests per second per IP.
     */
    private int requestsPerSecond = 100;

    /**
     * Burst size for token bucket algorithm.
     * Allows short bursts above the rate limit.
     */
    private int burstSize = 0;

    /**
     * Return whether rate limiting is enabled.
     * @return {@code true} if rate limiting is enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Set whether to enable rate limiting.
     * @param enabled {@code true} to enable rate limiting
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the maximum requests per second.
     * @return the requests per second limit
     */
    public int getRequestsPerSecond() {
        return this.requestsPerSecond;
    }

    /**
     * Set the maximum requests per second.
     * @param requestsPerSecond the requests per second limit
     */
    public void setRequestsPerSecond(int requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
    }

    /**
     * Return the burst size.
     * @return the burst size
     */
    public int getBurstSize() {
        return this.burstSize;
    }

    /**
     * Set the burst size.
     * @param burstSize the burst size
     */
    public void setBurstSize(int burstSize) {
        this.burstSize = burstSize;
    }

}
