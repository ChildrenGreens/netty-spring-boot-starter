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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RouteDefinition}.
 */
class RouteDefinitionTest {

    @Test
    void constructor_setsAllFields() throws NoSuchMethodException {
        Object bean = new TestController();
        Method method = TestController.class.getMethod("handleRequest", String.class);

        RouteDefinition route = new RouteDefinition(
                "/api/test", "GET", bean, method, String.class, "server1");

        assertThat(route.getRouteKey()).isEqualTo("/api/test");
        assertThat(route.getHttpMethod()).isEqualTo("GET");
        assertThat(route.getBean()).isSameAs(bean);
        assertThat(route.getMethod()).isSameAs(method);
        assertThat(route.getPayloadType()).isEqualTo(String.class);
    }

    @Test
    void matchesServer_withNullServerName_returnsTrue() throws NoSuchMethodException {
        RouteDefinition route = createRoute(null);
        assertThat(route.matchesServer("anyServer")).isTrue();
        assertThat(route.matchesServer(null)).isTrue();
    }

    @Test
    void matchesServer_withEmptyServerName_returnsTrue() throws NoSuchMethodException {
        RouteDefinition route = createRoute("");
        assertThat(route.matchesServer("anyServer")).isTrue();
    }

    @Test
    void matchesServer_withMatchingServerName_returnsTrue() throws NoSuchMethodException {
        RouteDefinition route = createRoute("myServer");
        assertThat(route.matchesServer("myServer")).isTrue();
    }

    @Test
    void matchesServer_withDifferentServerName_returnsFalse() throws NoSuchMethodException {
        RouteDefinition route = createRoute("myServer");
        assertThat(route.matchesServer("otherServer")).isFalse();
    }

    @Test
    void matchesServer_withNullInput_matchesNullRoute() throws NoSuchMethodException {
        RouteDefinition route = createRoute(null);
        assertThat(route.matchesServer(null)).isTrue();
    }

    @Test
    void getRouteKey_returnsCorrectValue() throws NoSuchMethodException {
        RouteDefinition route = createRoute("/path", "POST", null);
        assertThat(route.getRouteKey()).isEqualTo("/path");
    }

    @Test
    void getHttpMethod_returnsCorrectValue() throws NoSuchMethodException {
        RouteDefinition route = createRoute("/path", "DELETE", null);
        assertThat(route.getHttpMethod()).isEqualTo("DELETE");
    }

    @Test
    void getPayloadType_returnsCorrectValue() throws NoSuchMethodException {
        Object bean = new TestController();
        Method method = TestController.class.getMethod("handleRequest", String.class);
        RouteDefinition route = new RouteDefinition(
                "/test", null, bean, method, Integer.class, null);
        assertThat(route.getPayloadType()).isEqualTo(Integer.class);
    }

    private RouteDefinition createRoute(String serverName) throws NoSuchMethodException {
        return createRoute("/test", "GET", serverName);
    }

    private RouteDefinition createRoute(String path, String httpMethod, String serverName)
            throws NoSuchMethodException {
        Object bean = new TestController();
        Method method = TestController.class.getMethod("handleRequest", String.class);
        return new RouteDefinition(path, httpMethod, bean, method, String.class, serverName);
    }

    /**
     * Test controller class.
     */
    public static class TestController {
        public String handleRequest(String input) {
            return "response";
        }
    }
}
