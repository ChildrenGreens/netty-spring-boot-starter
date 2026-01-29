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
 * Configuration specification for heartbeat/keep-alive settings.
 *
 * <p>Example configuration:
 * <pre>{@code
 * heartbeat:
 *   enabled: true
 *   intervalMs: 30000
 *   timeoutMs: 5000
 *   message: '{"type":"heartbeat"}'
 * }</pre>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class HeartbeatSpec {

    /**
     * Whether heartbeat is enabled.
     */
    private boolean enabled = true;

    /**
     * Interval in milliseconds between heartbeat messages.
     */
    private long intervalMs = 30000;

    /**
     * Timeout in milliseconds to wait for heartbeat response.
     */
    private long timeoutMs = 5000;

    /**
     * The heartbeat message content.
     */
    private String message = "{\"type\":\"heartbeat\"}";

    /**
     * The expected heartbeat response type.
     */
    private String responseType = "heartbeat";

    /**
     * Return whether heartbeat is enabled.
     * @return {@code true} if enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Set whether heartbeat is enabled.
     * @param enabled {@code true} to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the interval in milliseconds.
     * @return the interval
     */
    public long getIntervalMs() {
        return this.intervalMs;
    }

    /**
     * Set the interval in milliseconds.
     * @param intervalMs the interval
     */
    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    /**
     * Return the timeout in milliseconds.
     * @return the timeout
     */
    public long getTimeoutMs() {
        return this.timeoutMs;
    }

    /**
     * Set the timeout in milliseconds.
     * @param timeoutMs the timeout
     */
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /**
     * Return the heartbeat message.
     * @return the heartbeat message
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Set the heartbeat message.
     * @param message the heartbeat message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Return the expected response type.
     * @return the response type
     */
    public String getResponseType() {
        return this.responseType;
    }

    /**
     * Set the expected response type.
     * @param responseType the response type
     */
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

}
