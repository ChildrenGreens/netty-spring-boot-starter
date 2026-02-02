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

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user connections for multi-connection control.
 *
 * <p>This class tracks active connections per user and enforces
 * connection policies such as single-connection or kick-old-connection.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    /**
     * Map of userId to set of active channels.
     */
    private final Map<String, Set<Channel>> userConnections = new ConcurrentHashMap<>();

    /**
     * Called when a user is successfully authenticated.
     *
     * <p>Applies the connection policy and returns whether the connection
     * should be allowed.
     *
     * @param channel the new channel
     * @param userId the user ID
     * @param policy the connection policy
     * @param authMetrics metrics to record (may be null)
     * @return true if the connection is allowed, false if it should be rejected
     */
    public boolean onAuthenticated(Channel channel, String userId,
                                   ConnectionPolicy policy, AuthMetrics authMetrics) {
        Set<Channel> connections = userConnections.computeIfAbsent(
                userId, k -> ConcurrentHashMap.newKeySet());

        // Remove any closed channels first
        connections.removeIf(ch -> !ch.isActive());

        if (policy == null || policy.isAllowMultiple()) {
            // Check max connections limit
            int max = policy != null ? policy.getMaxConnectionsPerUser() : Integer.MAX_VALUE;
            if (connections.size() >= max) {
                logger.warn("User {} exceeded max connections limit ({})", userId, max);
                return false;
            }
        } else {
            // Single connection mode
            if (!connections.isEmpty()) {
                switch (policy.getStrategy()) {
                    case REJECT_NEW:
                        logger.debug("Rejecting new connection for user {}, already connected", userId);
                        return false;

                    case KICK_OLD:
                        logger.debug("Kicking old connections for user {}", userId);
                        for (Channel old : connections) {
                            kickChannel(old, "Logged in from another location");
                            if (authMetrics != null) {
                                authMetrics.incrementKicked();
                            }
                        }
                        connections.clear();
                        break;

                    case ALLOW:
                        // Fall through to add
                        break;
                }
            }
        }

        // Add the new connection
        connections.add(channel);

        // Set up close listener to remove the channel when it closes
        channel.closeFuture().addListener(f -> {
            connections.remove(channel);
            if (connections.isEmpty()) {
                userConnections.remove(userId);
            }
            logger.debug("User {} connection closed, remaining: {}", userId, connections.size());
        });

        logger.debug("User {} connected, total connections: {}", userId, connections.size());
        return true;
    }

    /**
     * Get all active connections for a user.
     *
     * @param userId the user ID
     * @return an unmodifiable set of channels, or empty set if none
     */
    public Set<Channel> getConnections(String userId) {
        Set<Channel> connections = userConnections.get(userId);
        if (connections == null) {
            return Collections.emptySet();
        }
        // Remove closed channels and return
        connections.removeIf(ch -> !ch.isActive());
        return Collections.unmodifiableSet(connections);
    }

    /**
     * Kick a user from all connections.
     *
     * @param userId the user ID
     * @param reason the kick reason
     */
    public void kickUser(String userId, String reason) {
        Set<Channel> connections = userConnections.remove(userId);
        if (connections != null) {
            for (Channel channel : connections) {
                kickChannel(channel, reason);
            }
            logger.info("Kicked user {} from {} connections: {}", userId, connections.size(), reason);
        }
    }

    /**
     * Broadcast a message to all connections of a user.
     *
     * @param userId the user ID
     * @param message the message to send
     */
    public void broadcast(String userId, Object message) {
        Set<Channel> connections = getConnections(userId);
        for (Channel channel : connections) {
            if (channel.isActive()) {
                channel.writeAndFlush(message);
            }
        }
    }

    /**
     * Get the number of active connections for a user.
     *
     * @param userId the user ID
     * @return the connection count
     */
    public int getConnectionCount(String userId) {
        return getConnections(userId).size();
    }

    /**
     * Get the total number of connected users.
     *
     * @return the user count
     */
    public int getConnectedUserCount() {
        return userConnections.size();
    }

    /**
     * Kick a channel with a reason message.
     */
    private void kickChannel(Channel channel, String reason) {
        if (channel.isActive()) {
            // Send kick notification before closing
            Map<String, Object> kickMessage = Map.of(
                    "type", "/kicked",
                    "reason", reason
            );
            channel.writeAndFlush(kickMessage).addListener(f -> channel.close());
        }
    }

}
