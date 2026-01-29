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

import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Base class for all Netty-related Spring events.
 *
 * <p>This provides a common foundation for server and client events,
 * enabling consistent event handling across the Netty Spring Boot framework.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public abstract class NettyEvent extends ApplicationEvent {

    private final String name;
    private final Instant timestamp;

    /**
     * Create a new NettyEvent.
     * @param source the object on which the event initially occurred
     * @param name the name of the server or client
     */
    protected NettyEvent(Object source, String name) {
        super(source);
        this.name = name;
        this.timestamp = Instant.now();
    }

    /**
     * Return the name of the server or client.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the timestamp when this event was created.
     * @return the timestamp
     */
    public Instant getEventTimestamp() {
        return timestamp;
    }

}
