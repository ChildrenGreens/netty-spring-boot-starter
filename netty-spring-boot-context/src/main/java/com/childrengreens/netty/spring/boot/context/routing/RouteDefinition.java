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

import java.lang.reflect.Method;

/**
 * Definition of a route mapping a route key to a handler method.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 * @see Router
 */
public class RouteDefinition {

    /**
     * The route key for matching (path, message type, etc.).
     */
    private final String routeKey;

    /**
     * The HTTP method (for HTTP routes).
     */
    private final String httpMethod;

    /**
     * The bean instance containing the handler method.
     */
    private final Object bean;

    /**
     * The handler method to invoke.
     */
    private final Method method;

    /**
     * The expected payload type for parameter binding.
     */
    private final Class<?> payloadType;

    /**
     * The server name filter (optional).
     */
    private final String serverName;

    /**
     * Create a new RouteDefinition.
     * @param routeKey the route key
     * @param httpMethod the HTTP method (can be null for non-HTTP routes)
     * @param bean the handler bean
     * @param method the handler method
     * @param payloadType the payload type
     * @param serverName the server name filter
     */
    public RouteDefinition(String routeKey, String httpMethod, Object bean,
                           Method method, Class<?> payloadType, String serverName) {
        this.routeKey = routeKey;
        this.httpMethod = httpMethod;
        this.bean = bean;
        this.method = method;
        this.payloadType = payloadType;
        this.serverName = serverName;
    }

    /**
     * Return the route key.
     * @return the route key
     */
    public String getRouteKey() {
        return this.routeKey;
    }

    /**
     * Return the HTTP method.
     * @return the HTTP method, or {@code null} for non-HTTP routes
     */
    public String getHttpMethod() {
        return this.httpMethod;
    }

    /**
     * Return the handler bean.
     * @return the bean
     */
    public Object getBean() {
        return this.bean;
    }

    /**
     * Return the handler method.
     * @return the method
     */
    public Method getMethod() {
        return this.method;
    }

    /**
     * Return the payload type.
     * @return the payload type
     */
    public Class<?> getPayloadType() {
        return this.payloadType;
    }

    /**
     * Return the server name filter.
     * @return the server name, or {@code null} if not filtered
     */
    public String getServerName() {
        return this.serverName;
    }

    /**
     * Return whether this route matches the given server name.
     * @param serverName the server name to check
     * @return {@code true} if the route matches
     */
    public boolean matchesServer(String serverName) {
        return this.serverName == null || this.serverName.isEmpty() ||
                this.serverName.equals(serverName);
    }

    @Override
    public String toString() {
        return "RouteDefinition{" +
                "routeKey='" + routeKey + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", method=" + method.getName() +
                ", serverName='" + serverName + '\'' +
                '}';
    }

}
