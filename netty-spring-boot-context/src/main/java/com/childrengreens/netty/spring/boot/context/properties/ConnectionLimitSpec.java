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
 * Connection limit configuration.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class ConnectionLimitSpec {

    /**
     * Whether connection limiting is enabled.
     */
    private boolean enabled = false;

    /**
     * Maximum number of concurrent connections.
     */
    private int maxConnections = 10000;

    /**
     * Return whether connection limiting is enabled.
     * @return {@code true} if connection limiting is enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Set whether to enable connection limiting.
     * @param enabled {@code true} to enable connection limiting
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the maximum number of connections.
     * @return the max connections
     */
    public int getMaxConnections() {
        return this.maxConnections;
    }

    /**
     * Set the maximum number of connections.
     * @param maxConnections the max connections
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

}
