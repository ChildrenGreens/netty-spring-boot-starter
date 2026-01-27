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
 * Enumeration of supported transport protocols.
 *
 * <p>The transport type determines the underlying network protocol
 * and the corresponding Netty channel implementation.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public enum TransportType {

    /**
     * TCP transport for stream-based, connection-oriented communication.
     * <p>Uses {@code ServerSocketChannel} for server-side operations.
     */
    TCP,

    /**
     * UDP transport for datagram-based, connectionless communication.
     * <p>Uses {@code DatagramChannel} for both send and receive operations.
     */
    UDP,

    /**
     * HTTP transport for HTTP/1.1 and HTTP/2 protocol support.
     * <p>Includes support for WebSocket upgrade.
     */
    HTTP

}
