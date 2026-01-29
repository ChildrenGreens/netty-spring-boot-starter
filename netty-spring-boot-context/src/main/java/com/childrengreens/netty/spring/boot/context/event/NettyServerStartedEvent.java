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

import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;

/**
 * Event published when a Netty server has started successfully.
 *
 * <p>This event is published after the server is bound to the specified
 * host and port and is ready to accept connections.
 *
 * <p>Example usage:
 * <pre>{@code
 * @EventListener
 * public void onServerStarted(NettyServerStartedEvent event) {
 *     log.info("Server {} started on {}:{}",
 *         event.getName(), event.getHost(), event.getPort());
 * }
 * }</pre>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see NettyServerStoppedEvent
 */
public class NettyServerStartedEvent extends NettyEvent {

    private final String host;
    private final int port;
    private final String profile;

    /**
     * Create a new NettyServerStartedEvent.
     * @param source the orchestrator that published this event
     * @param name the server name
     * @param host the host the server is bound to
     * @param port the port the server is listening on
     * @param profile the profile used by the server
     */
    public NettyServerStartedEvent(NettyServerOrchestrator source, String name,
                                    String host, int port, String profile) {
        super(source, name);
        this.host = host;
        this.port = port;
        this.profile = profile;
    }

    /**
     * Return the host the server is bound to.
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Return the port the server is listening on.
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Return the profile used by the server.
     * @return the profile name
     */
    public String getProfile() {
        return profile;
    }

    @Override
    public String toString() {
        return "NettyServerStartedEvent{" +
                "name='" + getName() + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", profile='" + profile + '\'' +
                '}';
    }

}
