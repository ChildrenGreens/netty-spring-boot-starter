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

package com.childrengreens.netty.spring.boot.context.event;

import com.childrengreens.netty.spring.boot.context.client.NettyClientOrchestrator;

/**
 * Event published when a Netty client has disconnected from a server.
 *
 * <p>This event is published after a connection has been closed,
 * either gracefully or due to an error.
 *
 * <p>Example usage:
 * <pre>{@code
 * @EventListener
 * public void onClientDisconnected(NettyClientDisconnectedEvent event) {
 *     log.info("Client {} disconnected from {}:{}, reason: {}",
 *         event.getName(), event.getHost(), event.getPort(), event.getReason());
 * }
 * }</pre>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see NettyClientConnectedEvent
 */
public class NettyClientDisconnectedEvent extends NettyEvent {

    private final String host;
    private final int port;
    private final String reason;

    /**
     * Create a new NettyClientDisconnectedEvent.
     * @param source the orchestrator that published this event
     * @param name the client name
     * @param host the host that was connected to
     * @param port the port that was connected to
     * @param reason the reason for disconnection (may be null for graceful shutdown)
     */
    public NettyClientDisconnectedEvent(NettyClientOrchestrator source, String name,
                                         String host, int port, String reason) {
        super(source, name);
        this.host = host;
        this.port = port;
        this.reason = reason;
    }

    /**
     * Return the host that was connected to.
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Return the port that was connected to.
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Return the reason for disconnection.
     * @return the reason, or null for graceful shutdown
     */
    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "NettyClientDisconnectedEvent{" +
                "name='" + getName() + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", reason='" + reason + '\'' +
                '}';
    }

}
