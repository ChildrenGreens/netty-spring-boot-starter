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
 * Event published when a Netty server has stopped.
 *
 * <p>This event is published after the server has been shut down
 * and is no longer accepting connections.
 *
 * <p>Example usage:
 * <pre>{@code
 * @EventListener
 * public void onServerStopped(NettyServerStoppedEvent event) {
 *     log.info("Server {} stopped", event.getName());
 * }
 * }</pre>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see NettyServerStartedEvent
 */
public class NettyServerStoppedEvent extends NettyEvent {

    /**
     * Create a new NettyServerStoppedEvent.
     * @param source the orchestrator that published this event
     * @param name the server name
     */
    public NettyServerStoppedEvent(NettyServerOrchestrator source, String name) {
        super(source, name);
    }

    @Override
    public String toString() {
        return "NettyServerStoppedEvent{" +
                "name='" + getName() + '\'' +
                '}';
    }

}
