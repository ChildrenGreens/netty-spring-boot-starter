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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void concurrentRegisterAndFindRoute_doesNotThrowConcurrentModificationException() throws Exception {
        // This test verifies the fix for ConcurrentModificationException
        // when registering routes while simultaneously finding routes
        Method method = TestController.class.getMethod("handleGet");
        TestController controller = new TestController();

        int numThreads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        AtomicInteger successfulFinds = new AtomicInteger(0);

        // Pre-register some routes
        for (int i = 0; i < 10; i++) {
            RouteDefinition route = new RouteDefinition("/api/users/{id" + i + "}", "GET",
                    controller, method, Void.class, null);
            router.register(route);
        }

        // Start threads that concurrently register and find routes
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int i = 0; i < operationsPerThread; i++) {
                        // Half threads register, half threads find
                        if (threadId % 2 == 0) {
                            // Register new pattern routes
                            RouteDefinition route = new RouteDefinition(
                                    "/api/thread" + threadId + "/resource/{id" + i + "}",
                                    "GET", controller, method, Void.class, null);
                            router.register(route);
                        } else {
                            // Find existing routes
                            Map<String, Object> headers = new HashMap<>();
                            headers.put("httpMethod", "GET");
                            InboundMessage message = InboundMessage.builder()
                                    .transport(TransportType.HTTP)
                                    .routeKey("/api/users/123")
                                    .headers(headers)
                                    .build();

                            Router.RouteResult result = router.findRoute(message, null);
                            if (result != null) {
                                successfulFinds.incrementAndGet();
                            }
                        }
                    }
                } catch (Exception e) {
                    exceptionOccurred.set(true);
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

        executor.shutdown();

        assertThat(completed).isTrue();
        // Before fix: ConcurrentModificationException would occur
        // After fix: No exception should occur
        assertThat(exceptionOccurred.get()).isFalse();
        assertThat(successfulFinds.get()).isGreaterThan(0);
    }

    @Test
    void concurrentRegisterAndFindExactRoute_doesNotThrowConcurrentModificationException() throws Exception {
        // Test concurrent access to exact routes (ConcurrentHashMap with CopyOnWriteArrayList values)
        Method method = TestController.class.getMethod("handleGet");
        TestController controller = new TestController();

        int numThreads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        List<Throwable> exceptions = new ArrayList<>();

        // Pre-register some exact routes
        for (int i = 0; i < 10; i++) {
            RouteDefinition route = new RouteDefinition("/api/exact" + i, "GET",
                    controller, method, Void.class, "server" + (i % 3));
            router.register(route);
        }

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int i = 0; i < operationsPerThread; i++) {
                        if (threadId % 2 == 0) {
                            // Register new exact routes to same key (different servers)
                            RouteDefinition route = new RouteDefinition(
                                    "/api/exact0", "GET", controller, method, Void.class,
                                    "server" + threadId + "_" + i);
                            router.register(route);
                        } else {
                            // Find routes
                            Map<String, Object> headers = new HashMap<>();
                            headers.put("httpMethod", "GET");
                            InboundMessage message = InboundMessage.builder()
                                    .transport(TransportType.HTTP)
                                    .routeKey("/api/exact0")
                                    .headers(headers)
                                    .build();

                            // Iterate through routes for different servers
                            router.findRoute(message, "server0");
                            router.findRoute(message, "server1");
                            router.findRoute(message, "server2");
                        }
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                    exceptionOccurred.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(exceptionOccurred.get())
                .withFailMessage("Exceptions occurred: " + exceptions)
                .isFalse();
    }

    @Test
    void register_duplicateExactRoute_throwsException() throws Exception {
        Method method = TestController.class.getMethod("handleGet");
        TestController controller = new TestController();

        RouteDefinition route1 = new RouteDefinition("/api/users", "GET",
                controller, method, Void.class, "serverA");
        RouteDefinition route2 = new RouteDefinition("/api/users", "GET",
                controller, method, Void.class, "serverA");

        router.register(route1);

        assertThatThrownBy(() -> router.register(route2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate route registration");
    }

    @Test
    void register_duplicatePatternRoute_throwsException() throws Exception {
        Method method = TestController.class.getMethod("handleGet");
        TestController controller = new TestController();

        RouteDefinition route1 = new RouteDefinition("/api/users/{id}", "GET",
                controller, method, Void.class, null);
        RouteDefinition route2 = new RouteDefinition("/api/users/{id}", "GET",
                controller, method, Void.class, "");

        router.register(route1);

        assertThatThrownBy(() -> router.register(route2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate route registration");
    }

    // Test controller class
    public static class TestController {
        public String handleGet() {
            return "OK";
        }
    }
}
