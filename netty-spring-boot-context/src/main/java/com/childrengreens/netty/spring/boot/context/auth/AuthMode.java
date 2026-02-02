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
 * Authentication mode enumeration.
 *
 * <p>Defines how authentication is performed based on the protocol type.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public enum AuthMode {

    /**
     * Token-based authentication (for HTTP).
     *
     * <p>Each request must carry a token in the header.
     * The server validates the token on every request.
     */
    TOKEN,

    /**
     * Credential-based authentication (for WebSocket/TCP).
     *
     * <p>The first message after connection must be an authentication
     * message containing username and password. Once authenticated,
     * subsequent messages do not need to carry credentials.
     */
    CREDENTIAL

}
