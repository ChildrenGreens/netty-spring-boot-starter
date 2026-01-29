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

import com.childrengreens.netty.spring.boot.context.annotation.*;
import com.childrengreens.netty.spring.boot.context.message.InboundMessage;
import com.childrengreens.netty.spring.boot.context.properties.TransportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AnnotationRegistry}.
 */
class AnnotationRegistryTest {

    private Router router;
    private AnnotationRegistry registry;

    @BeforeEach
    void setUp() {
        router = new Router();
        registry = new AnnotationRegistry();

        // Mock BeanFactory to provide Router
        BeanFactory beanFactory = mock(BeanFactory.class);
        when(beanFactory.getBean(Router.class)).thenReturn(router);

        registry.setBeanFactory(beanFactory);
        registry.afterPropertiesSet();
    }

    @Test
    void postProcessAfterInitialization_withNettyController_registersHttpRoutes() {
        TestHttpController controller = new TestHttpController();

        Object result = registry.postProcessAfterInitialization(controller, "testHttpController");

        assertThat(result).isSameAs(controller);

        Map<String, Object> headers = new HashMap<>();
        headers.put("httpMethod", "GET");
        InboundMessage getMessage = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/api/users")
                .headers(headers)
                .build();
        assertThat(router.findRoute(getMessage, null)).isNotNull();

        headers.put("httpMethod", "POST");
        InboundMessage postMessage = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/api/users")
                .headers(headers)
                .build();
        assertThat(router.findRoute(postMessage, null)).isNotNull();
    }

    @Test
    void postProcessAfterInitialization_withNettyMessageController_registersMessageRoutes() {
        TestMessageController controller = new TestMessageController();

        Object result = registry.postProcessAfterInitialization(controller, "testMessageController");

        assertThat(result).isSameAs(controller);

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.TCP)
                .routeKey("ping")
                .build();
        assertThat(router.findRoute(message, null)).isNotNull();
    }

    @Test
    void postProcessAfterInitialization_withPutAndDelete_registersRoutes() {
        TestCrudController controller = new TestCrudController();

        Object result = registry.postProcessAfterInitialization(controller, "testCrudController");

        assertThat(result).isSameAs(controller);

        Map<String, Object> headers = new HashMap<>();
        headers.put("httpMethod", "PUT");
        InboundMessage putMessage = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/items")
                .headers(headers)
                .build();
        assertThat(router.findRoute(putMessage, null)).isNotNull();

        headers.put("httpMethod", "DELETE");
        InboundMessage deleteMessage = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/items")
                .headers(headers)
                .build();
        assertThat(router.findRoute(deleteMessage, null)).isNotNull();
    }

    @Test
    void postProcessAfterInitialization_withWebSocketController_registersWsRoutes() {
        TestWsController controller = new TestWsController();

        Object result = registry.postProcessAfterInitialization(controller, "testWsController");

        assertThat(result).isSameAs(controller);

        InboundMessage openMessage = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("WS:OPEN:/ws/chat")
                .build();
        assertThat(router.findRoute(openMessage, null)).isNotNull();

        InboundMessage textMessage = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("WS:TEXT:/ws/chat")
                .build();
        assertThat(router.findRoute(textMessage, null)).isNotNull();

        InboundMessage closeMessage = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("WS:CLOSE:/ws/chat")
                .build();
        assertThat(router.findRoute(closeMessage, null)).isNotNull();
    }

    @Test
    void postProcessAfterInitialization_withNonAnnotatedBean_returnsUnchanged() {
        PlainBean bean = new PlainBean();

        Object result = registry.postProcessAfterInitialization(bean, "plainBean");

        assertThat(result).isSameAs(bean);
    }

    @NettyController(path = "/api")
    static class TestHttpController {
        @NettyHttpGet("/users")
        public Map<String, Object> getUsers() {
            return Map.of();
        }

        @NettyHttpPost("/users")
        public Map<String, Object> createUser(@Body Map<String, Object> user) {
            return user;
        }
    }

    @NettyMessageController
    static class TestMessageController {
        @NettyMessageMapping("ping")
        public Map<String, Object> handlePing() {
            return Map.of("type", "pong");
        }
    }

    @NettyController
    static class TestCrudController {
        @NettyHttpPut("/items")
        public Map<String, Object> updateItem(@Body Map<String, Object> item) {
            return item;
        }

        @NettyHttpDelete("/items")
        public void deleteItem() {
        }
    }

    @NettyController(path = "/ws")
    static class TestWsController {
        @NettyWsOnOpen("/chat")
        public void onOpen() {
        }

        @NettyWsOnText("/chat")
        public String onText(String message) {
            return message;
        }

        @NettyWsOnClose("/chat")
        public void onClose() {
        }
    }

    static class PlainBean {
    }

}
