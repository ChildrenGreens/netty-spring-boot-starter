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
 * Configuration specification for timeout settings.
 *
 * <p>Example configuration:
 * <pre>{@code
 * timeout:
 *   connectMs: 5000
 *   requestMs: 10000
 * }</pre>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public class TimeoutSpec {

    /**
     * Connection timeout in milliseconds.
     */
    private long connectMs = 5000;

    /**
     * Request timeout in milliseconds.
     */
    private long requestMs = 10000;

    /**
     * Read timeout in milliseconds.
     */
    private long readMs = 30000;

    /**
     * Write timeout in milliseconds.
     */
    private long writeMs = 10000;

    /**
     * Return the connection timeout in milliseconds.
     * @return the connection timeout
     */
    public long getConnectMs() {
        return this.connectMs;
    }

    /**
     * Set the connection timeout in milliseconds.
     * @param connectMs the connection timeout
     */
    public void setConnectMs(long connectMs) {
        this.connectMs = connectMs;
    }

    /**
     * Return the request timeout in milliseconds.
     * @return the request timeout
     */
    public long getRequestMs() {
        return this.requestMs;
    }

    /**
     * Set the request timeout in milliseconds.
     * @param requestMs the request timeout
     */
    public void setRequestMs(long requestMs) {
        this.requestMs = requestMs;
    }

    /**
     * Return the read timeout in milliseconds.
     * @return the read timeout
     */
    public long getReadMs() {
        return this.readMs;
    }

    /**
     * Set the read timeout in milliseconds.
     * @param readMs the read timeout
     */
    public void setReadMs(long readMs) {
        this.readMs = readMs;
    }

    /**
     * Return the write timeout in milliseconds.
     * @return the write timeout
     */
    public long getWriteMs() {
        return this.writeMs;
    }

    /**
     * Set the write timeout in milliseconds.
     * @param writeMs the write timeout
     */
    public void setWriteMs(long writeMs) {
        this.writeMs = writeMs;
    }

}
