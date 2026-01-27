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
import com.childrengreens.netty.spring.boot.context.properties.RoutingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Router for matching inbound messages to route definitions.
 *
 * <p>The router supports multiple routing modes:
 * <ul>
 * <li>{@link RoutingMode#PATH} - Match by HTTP path and method</li>
 * <li>{@link RoutingMode#MESSAGE_TYPE} - Match by message type field</li>
 * <li>{@link RoutingMode#WS_PATH} - Match by WebSocket path</li>
 * </ul>
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 * @see RouteDefinition
 */
public class Router {

    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    /**
     * Pattern for extracting path variables like {id}.
     */
    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{([^}]+)}");

    /**
     * Routes indexed by route key for exact matching.
     */
    private final Map<String, List<RouteDefinition>> exactRoutes = new ConcurrentHashMap<>();

    /**
     * Routes with path variables for pattern matching.
     */
    private final List<PatternRoute> patternRoutes = Collections.synchronizedList(new ArrayList<>());

    /**
     * Register a route definition.
     * @param route the route to register
     */
    public void register(RouteDefinition route) {
        String key = buildRouteKey(route.getRouteKey(), route.getHttpMethod());

        if (containsPathVariable(route.getRouteKey())) {
            // Register as pattern route
            Pattern pattern = compilePathPattern(route.getRouteKey());
            patternRoutes.add(new PatternRoute(pattern, route));
            logger.debug("Registered pattern route: {} -> {}", route.getRouteKey(), route.getMethod());
        } else {
            // Register as exact route
            exactRoutes.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(route);
            logger.debug("Registered exact route: {} -> {}", key, route.getMethod());
        }
    }

    /**
     * Find a route matching the inbound message.
     * @param message the inbound message
     * @param serverName the server name for filtering
     * @return the matching route result, or {@code null} if no match
     */
    public RouteResult findRoute(InboundMessage message, String serverName) {
        String routeKey = message.getRouteKey();
        String httpMethod = message.getHeader("httpMethod");
        String key = buildRouteKey(routeKey, httpMethod);

        // Try exact match first
        List<RouteDefinition> routes = exactRoutes.get(key);
        if (routes != null) {
            for (RouteDefinition route : routes) {
                if (route.matchesServer(serverName)) {
                    return new RouteResult(route, Collections.emptyMap());
                }
            }
        }

        // Try pattern matching
        for (PatternRoute patternRoute : patternRoutes) {
            Matcher matcher = patternRoute.pattern.matcher(routeKey);
            if (matcher.matches()) {
                RouteDefinition route = patternRoute.route;
                if (route.matchesServer(serverName) &&
                        (httpMethod == null || httpMethod.equals(route.getHttpMethod()))) {
                    Map<String, String> pathVariables = extractPathVariables(
                            route.getRouteKey(), matcher);
                    return new RouteResult(route, pathVariables);
                }
            }
        }

        logger.debug("No route found for: {} (server={})", key, serverName);
        return null;
    }

    /**
     * Build a composite route key from path and HTTP method.
     */
    private String buildRouteKey(String path, String httpMethod) {
        if (httpMethod != null && !httpMethod.isEmpty()) {
            return httpMethod + ":" + path;
        }
        return path;
    }

    /**
     * Check if the path contains path variables.
     */
    private boolean containsPathVariable(String path) {
        return path != null && path.contains("{");
    }

    /**
     * Compile a path pattern to a regex.
     */
    private Pattern compilePathPattern(String path) {
        String regex = PATH_VARIABLE_PATTERN.matcher(path)
                .replaceAll("([^/]+)");
        return Pattern.compile("^" + regex + "$");
    }

    /**
     * Extract path variable values from a matched path.
     */
    private Map<String, String> extractPathVariables(String pathTemplate, Matcher matcher) {
        Map<String, String> variables = new ConcurrentHashMap<>();
        Matcher varMatcher = PATH_VARIABLE_PATTERN.matcher(pathTemplate);
        int group = 1;
        while (varMatcher.find()) {
            String varName = varMatcher.group(1);
            if (group <= matcher.groupCount()) {
                variables.put(varName, matcher.group(group));
            }
            group++;
        }
        return variables;
    }

    /**
     * Internal class for pattern-based routes.
     */
    private static class PatternRoute {
        final Pattern pattern;
        final RouteDefinition route;

        PatternRoute(Pattern pattern, RouteDefinition route) {
            this.pattern = pattern;
            this.route = route;
        }
    }

    /**
     * Result of route matching including path variables.
     */
    public static class RouteResult {

        private final RouteDefinition route;
        private final Map<String, String> pathVariables;

        /**
         * Create a new RouteResult.
         * @param route the matched route
         * @param pathVariables the extracted path variables
         */
        public RouteResult(RouteDefinition route, Map<String, String> pathVariables) {
            this.route = route;
            this.pathVariables = pathVariables;
        }

        /**
         * Return the matched route.
         * @return the route definition
         */
        public RouteDefinition getRoute() {
            return this.route;
        }

        /**
         * Return the extracted path variables.
         * @return the path variables map
         */
        public Map<String, String> getPathVariables() {
            return this.pathVariables;
        }
    }

}
