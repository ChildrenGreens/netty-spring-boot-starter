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

package com.childrengreens.netty.spring.boot.context.routing;

import com.childrengreens.netty.spring.boot.context.message.InboundMessage;

/**
 * Strategy interface for extracting route keys from messages.
 *
 * <p>Implementations determine how to extract the routing key from
 * different message formats and protocols.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
@FunctionalInterface
public interface NettyRouteResolver {

    /**
     * Extract the route key from an inbound message.
     * @param message the inbound message
     * @return the route key for handler matching
     */
    String resolveRouteKey(InboundMessage message);

}
