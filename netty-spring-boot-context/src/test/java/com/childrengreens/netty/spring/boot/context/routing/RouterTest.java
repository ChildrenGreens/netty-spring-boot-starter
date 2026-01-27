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
import com.childrengreens.netty.spring.boot.context.properties.TransportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Router}.
 */
class RouterTest {

    private Router router;

    @BeforeEach
    void setUp() {
        router = new Router();
    }

    @Test
    void register_exactRoute_canBeFoundByKey() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("handleGet");
        TestController controller = new TestController();
        RouteDefinition route = new RouteDefinition("/api/test", "GET", controller, method, Void.class, null);

        router.register(route);

        Map<String, Object> headers = new HashMap<>();
        headers.put("httpMethod", "GET");
        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/api/test")
                .headers(headers)
                .build();

        Router.RouteResult result = router.findRoute(message, null);

        assertThat(result).isNotNull();
        assertThat(result.getRoute()).isEqualTo(route);
        assertThat(result.getPathVariables()).isEmpty();
    }

    @Test
    void register_patternRoute_canBeFoundWithPathVariables() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("handleGet");
        TestController controller = new TestController();
        RouteDefinition route = new RouteDefinition("/api/users/{id}", "GET", controller, method, Void.class, null);

        router.register(route);

        Map<String, Object> headers = new HashMap<>();
        headers.put("httpMethod", "GET");
        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/api/users/123")
                .headers(headers)
                .build();

        Router.RouteResult result = router.findRoute(message, null);

        assertThat(result).isNotNull();
        assertThat(result.getRoute()).isEqualTo(route);
        assertThat(result.getPathVariables()).containsEntry("id", "123");
    }

    @Test
    void findRoute_withNoMatch_returnsNull() {
        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/nonexistent")
                .build();

        Router.RouteResult result = router.findRoute(message, null);

        assertThat(result).isNull();
    }

    @Test
    void findRoute_withServerFilter_matchesCorrectServer() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("handleGet");
        TestController controller = new TestController();
        RouteDefinition route = new RouteDefinition("/api/test", null, controller, method, Void.class, "myServer");

        router.register(route);

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.TCP)
                .routeKey("/api/test")
                .build();

        Router.RouteResult result1 = router.findRoute(message, "myServer");
        Router.RouteResult result2 = router.findRoute(message, "otherServer");

        assertThat(result1).isNotNull();
        assertThat(result2).isNull();
    }

    @Test
    void findRoute_withMultiplePathVariables_extractsAll() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("handleGet");
        TestController controller = new TestController();
        RouteDefinition route = new RouteDefinition("/api/{resource}/{id}", "GET", controller, method, Void.class, null);

        router.register(route);

        Map<String, Object> headers = new HashMap<>();
        headers.put("httpMethod", "GET");
        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/api/users/456")
                .headers(headers)
                .build();

        Router.RouteResult result = router.findRoute(message, null);

        assertThat(result).isNotNull();
        assertThat(result.getPathVariables()).containsEntry("resource", "users");
        assertThat(result.getPathVariables()).containsEntry("id", "456");
    }

    @Test
    void findRoute_messageTypeRouting_withoutHttpMethod() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("handleGet");
        TestController controller = new TestController();
        RouteDefinition route = new RouteDefinition("ping", null, controller, method, Void.class, null);

        router.register(route);

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.TCP)
                .routeKey("ping")
                .build();

        Router.RouteResult result = router.findRoute(message, null);

        assertThat(result).isNotNull();
        assertThat(result.getRoute()).isEqualTo(route);
    }

    @Test
    void routeResult_getters() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("handleGet");
        TestController controller = new TestController();
        RouteDefinition route = new RouteDefinition("/test", "GET", controller, method, Void.class, null);
        Map<String, String> pathVariables = new HashMap<>();
        pathVariables.put("id", "123");

        Router.RouteResult result = new Router.RouteResult(route, pathVariables);

        assertThat(result.getRoute()).isEqualTo(route);
        assertThat(result.getPathVariables()).isEqualTo(pathVariables);
    }

    // Test controller class
    public static class TestController {
        public String handleGet() {
            return "OK";
        }
    }
}
