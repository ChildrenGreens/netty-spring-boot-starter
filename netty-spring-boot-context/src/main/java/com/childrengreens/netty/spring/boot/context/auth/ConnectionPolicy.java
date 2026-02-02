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

/**
 * Configuration for multi-connection control policy.
 *
 * <p>Defines how the server handles multiple connections from the same user,
 * applicable for WebSocket and TCP protocols.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class ConnectionPolicy {

    /**
     * Whether to allow multiple connections from the same user.
     * Default is false (single connection per user).
     */
    private boolean allowMultiple = false;

    /**
     * Strategy for handling multiple connections.
     * Only effective when allowMultiple is false.
     */
    private ConnectionStrategy strategy = ConnectionStrategy.KICK_OLD;

    /**
     * Maximum number of connections per user.
     * Only effective when allowMultiple is true.
     */
    private int maxConnectionsPerUser = 1;

    public boolean isAllowMultiple() {
        return allowMultiple;
    }

    public void setAllowMultiple(boolean allowMultiple) {
        this.allowMultiple = allowMultiple;
    }

    public ConnectionStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(ConnectionStrategy strategy) {
        this.strategy = strategy;
    }

    public int getMaxConnectionsPerUser() {
        return maxConnectionsPerUser;
    }

    public void setMaxConnectionsPerUser(int maxConnectionsPerUser) {
        this.maxConnectionsPerUser = maxConnectionsPerUser;
    }

}
