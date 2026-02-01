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

package com.childrengreens.netty.spring.boot.context.backpressure;

/**
 * Policy for dropping messages when using {@link BackpressureStrategy#DROP}.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public enum DropPolicy {

    /**
     * Drop the newest (most recently arrived) messages.
     * <p>The queue keeps older messages, newer ones are discarded.
     * Use when older data is more valuable.
     */
    NEWEST,

    /**
     * Drop the oldest messages from the queue.
     * <p>The queue keeps newer messages, older ones are discarded.
     * Use when fresher data is more valuable (e.g., real-time updates).
     */
    OLDEST

}
