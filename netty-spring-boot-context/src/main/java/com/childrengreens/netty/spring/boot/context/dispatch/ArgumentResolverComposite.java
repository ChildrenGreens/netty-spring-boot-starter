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

package com.childrengreens.netty.spring.boot.context.dispatch;

import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.message.InboundMessage;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Composite argument resolver that delegates to a list of resolvers.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see ArgumentResolver
 */
public class ArgumentResolverComposite {

    private final List<ArgumentResolver> resolvers = new ArrayList<>();

    /**
     * Add an argument resolver.
     * @param resolver the resolver to add
     */
    public void addResolver(ArgumentResolver resolver) {
        this.resolvers.add(resolver);
    }

    /**
     * Add multiple argument resolvers.
     * @param resolvers the resolvers to add
     */
    public void addResolvers(List<ArgumentResolver> resolvers) {
        this.resolvers.addAll(resolvers);
    }

    /**
     * Resolve an argument using the registered resolvers.
     * @param parameter the method parameter
     * @param message the inbound message
     * @param context the Netty context
     * @param pathVariables the path variables
     * @return the resolved value, or {@code null} if no resolver supports it
     */
    public Object resolveArgument(Parameter parameter, InboundMessage message,
                                   NettyContext context, Map<String, String> pathVariables) {
        for (ArgumentResolver resolver : this.resolvers) {
            if (resolver.supports(parameter)) {
                return resolver.resolve(parameter, message, context, pathVariables);
            }
        }
        return null;
    }

    /**
     * Return the list of resolvers.
     * @return the resolvers
     */
    public List<ArgumentResolver> getResolvers() {
        return this.resolvers;
    }

}
