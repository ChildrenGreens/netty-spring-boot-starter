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

package com.childrengreens.netty.spring.boot.context.server;

/**
 * Enumeration of server states.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public enum ServerState {

    /**
     * Server is being initialized.
     */
    INITIALIZING,

    /**
     * Server is starting.
     */
    STARTING,

    /**
     * Server is running and accepting connections.
     */
    RUNNING,

    /**
     * Server is stopping.
     */
    STOPPING,

    /**
     * Server is stopped.
     */
    STOPPED,

    /**
     * Server failed to start or encountered an error.
     */
    FAILED

}
