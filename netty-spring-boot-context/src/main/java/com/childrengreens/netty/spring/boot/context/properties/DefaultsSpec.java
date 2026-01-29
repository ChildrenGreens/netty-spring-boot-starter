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

import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Default settings applied to all servers unless overridden.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class DefaultsSpec {

    /**
     * Thread pool configuration.
     */
    @NestedConfigurationProperty
    private ThreadsSpec threads = new ThreadsSpec();

    /**
     * Transport configuration.
     */
    @NestedConfigurationProperty
    private TransportSpec transport = new TransportSpec();

    /**
     * Shutdown configuration.
     */
    @NestedConfigurationProperty
    private ShutdownSpec shutdown = new ShutdownSpec();

    /**
     * Return the thread pool configuration.
     * @return the threads specification
     */
    public ThreadsSpec getThreads() {
        return this.threads;
    }

    /**
     * Set the thread pool configuration.
     * @param threads the threads specification
     */
    public void setThreads(ThreadsSpec threads) {
        this.threads = threads;
    }

    /**
     * Return the transport configuration.
     * @return the transport specification
     */
    public TransportSpec getTransport() {
        return this.transport;
    }

    /**
     * Set the transport configuration.
     * @param transport the transport specification
     */
    public void setTransport(TransportSpec transport) {
        this.transport = transport;
    }

    /**
     * Return the shutdown configuration.
     * @return the shutdown specification
     */
    public ShutdownSpec getShutdown() {
        return this.shutdown;
    }

    /**
     * Set the shutdown configuration.
     * @param shutdown the shutdown specification
     */
    public void setShutdown(ShutdownSpec shutdown) {
        this.shutdown = shutdown;
    }

}
