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
 * Strategy interface for authentication.
 *
 * <p>Implementations of this interface perform the actual authentication
 * logic, such as validating tokens or verifying user credentials.
 *
 * <p>Users should implement this interface and register it as a Spring bean
 * to customize the authentication behavior.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public interface Authenticator {

    /**
     * Authenticate using a token (for HTTP requests).
     *
     * <p>This method is called for each HTTP request that requires authentication.
     * The token is typically extracted from the Authorization header.
     *
     * @param token the token to validate (may include "Bearer " prefix)
     * @return the authentication result
     */
    AuthResult authenticateToken(String token);

    /**
     * Authenticate using credentials (for WebSocket/TCP connections).
     *
     * <p>This method is called when processing the first message on a
     * new WebSocket or TCP connection.
     *
     * @param username the username
     * @param password the password
     * @return the authentication result
     */
    AuthResult authenticateCredential(String username, String password);

}
