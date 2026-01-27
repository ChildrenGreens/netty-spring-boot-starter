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
 * Enumeration of routing modes for message dispatching.
 *
 * <p>The routing mode determines how incoming messages are matched
 * to handler methods.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 */
public enum RoutingMode {

    /**
     * Route by HTTP path and method.
     * <p>Suitable for HTTP and REST-style APIs.
     */
    PATH,

    /**
     * Route by message type field.
     * <p>Suitable for TCP/UDP protocols with typed messages.
     */
    MESSAGE_TYPE,

    /**
     * Route by WebSocket path.
     * <p>Suitable for WebSocket connections.
     */
    WS_PATH,

    /**
     * Route by topic or subject.
     * <p>Suitable for pub/sub style messaging.
     */
    TOPIC,

    /**
     * Custom routing logic provided by user.
     * <p>Requires implementation of {@code NettyRouteResolver}.
     */
    CUSTOM

}
