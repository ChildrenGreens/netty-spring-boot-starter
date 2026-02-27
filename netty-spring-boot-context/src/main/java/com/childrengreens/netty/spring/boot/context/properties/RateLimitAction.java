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
 * Action to take when rate limit is exceeded.
 *
 * <p>This setting applies to WebSocket, TCP, and UDP protocols.
 * HTTP always returns 429 Too Many Requests.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public enum RateLimitAction {

    /**
     * Drop the message but keep the connection open.
     * This is the most lenient option for long-lived connections.
     */
    DROP,

    /**
     * Close the connection after dropping the message.
     * For WebSocket, sends a CloseFrame(1008) before closing.
     */
    CLOSE,

    /**
     * Send an error response and keep the connection open.
     * For WebSocket, sends a text frame with error message.
     * For TCP/UDP, sends a JSON error message.
     */
    REJECT

}
