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
 * Connection strategy enumeration for handling multiple connections
 * from the same user.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public enum ConnectionStrategy {

    /**
     * Allow multiple connections from the same user.
     *
     * <p>The number of connections can be limited by
     * {@link ConnectionPolicy#getMaxConnectionsPerUser()}.
     */
    ALLOW,

    /**
     * Reject new connection if user already has an active connection.
     *
     * <p>The new connection attempt will fail with an error.
     */
    REJECT_NEW,

    /**
     * Kick the old connection when a new connection is established.
     *
     * <p>The existing connection will be closed with a notification,
     * and the new connection will be accepted.
     */
    KICK_OLD

}
