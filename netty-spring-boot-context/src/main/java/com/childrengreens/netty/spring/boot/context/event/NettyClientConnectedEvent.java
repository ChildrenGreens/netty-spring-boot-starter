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
 * Event published when a Netty client has connected to a server.
 *
 * <p>This event is published after a connection has been established
 * successfully.
 *
 * <p>Example usage:
 * <pre>{@code
 * @EventListener
 * public void onClientConnected(NettyClientConnectedEvent event) {
 *     log.info("Client {} connected to {}:{}",
 *         event.getName(), event.getHost(), event.getPort());
 * }
 * }</pre>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see NettyClientDisconnectedEvent
 */
public class NettyClientConnectedEvent extends NettyEvent {

    private final String host;
    private final int port;

    /**
     * Create a new NettyClientConnectedEvent.
     * @param source the orchestrator that published this event
     * @param name the client name
     * @param host the host connected to
     * @param port the port connected to
     */
    public NettyClientConnectedEvent(NettyClientOrchestrator source, String name,
                                      String host, int port) {
        super(source, name);
        this.host = host;
        this.port = port;
    }

    /**
     * Return the host connected to.
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Return the port connected to.
     * @return the port
     */
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "NettyClientConnectedEvent{" +
                "name='" + getName() + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }

}
