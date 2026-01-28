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

import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.codec.JsonNettyCodec;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.message.InboundMessage;
import com.childrengreens.netty.spring.boot.context.message.OutboundMessage;
import com.childrengreens.netty.spring.boot.context.properties.TransportType;
import com.childrengreens.netty.spring.boot.context.routing.RouteDefinition;
import com.childrengreens.netty.spring.boot.context.routing.Router;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link Dispatcher}.
 */
class DispatcherTest {

    private Router router;
    private CodecRegistry codecRegistry;
    private Dispatcher dispatcher;
    private NettyContext context;
    private Channel channel;

    @BeforeEach
    void setUp() {
        router = mock(Router.class);
        codecRegistry = new CodecRegistry();
        codecRegistry.register(new JsonNettyCodec());
        dispatcher = new Dispatcher(router, codecRegistry);

        channel = mock(Channel.class);
        @SuppressWarnings("unchecked")
        Attribute<Object> attr = mock(Attribute.class);
        when(channel.attr(any())).thenReturn(attr);
        when(attr.get()).thenReturn("testServer");
        context = new NettyContext(channel);
    }

    @Test
    void dispatch_noRouteFound_returns404() throws Exception {
        when(router.findRoute(any(), any())).thenReturn(null);

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/unknown")
                .build();

        CompletableFuture<OutboundMessage> future = dispatcher.dispatch(message, context);
        OutboundMessage result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(404);
    }

    @Test
    void dispatch_routeFound_invokesHandler() throws Exception {
        TestHandler handler = new TestHandler();
        RouteDefinition route = new RouteDefinition("/test", null, handler,
                TestHandler.class.getMethod("handleSimple"), Void.class, null);
        Router.RouteResult routeResult = new Router.RouteResult(route, Collections.emptyMap());

        when(router.findRoute(any(), any())).thenReturn(routeResult);

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/test")
                .build();

        CompletableFuture<OutboundMessage> future = dispatcher.dispatch(message, context);
        OutboundMessage result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(result.getPayload()).isEqualTo("OK");
    }

    @Test
    void dispatch_handlerReturnsOutboundMessage_returnsDirectly() throws Exception {
        TestHandler handler = new TestHandler();
        RouteDefinition route = new RouteDefinition("/test", null, handler,
                TestHandler.class.getMethod("handleReturnsOutbound"), Void.class, null);
        Router.RouteResult routeResult = new Router.RouteResult(route, Collections.emptyMap());

        when(router.findRoute(any(), any())).thenReturn(routeResult);

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/test")
                .build();

        CompletableFuture<OutboundMessage> future = dispatcher.dispatch(message, context);
        OutboundMessage result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(201);
    }

    @Test
    void dispatch_handlerReturnsCompletableFuture_awaitsResult() throws Exception {
        TestHandler handler = new TestHandler();
        RouteDefinition route = new RouteDefinition("/test", null, handler,
                TestHandler.class.getMethod("handleAsync"), Void.class, null);
        Router.RouteResult routeResult = new Router.RouteResult(route, Collections.emptyMap());

        when(router.findRoute(any(), any())).thenReturn(routeResult);

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/test")
                .build();

        CompletableFuture<OutboundMessage> future = dispatcher.dispatch(message, context);
        OutboundMessage result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(result.getPayload()).isEqualTo("async result");
    }

    @Test
    void dispatch_handlerReturnsNull_returnsNull() throws Exception {
        TestHandler handler = new TestHandler();
        RouteDefinition route = new RouteDefinition("/test", null, handler,
                TestHandler.class.getMethod("handleReturnsNull"), Void.class, null);
        Router.RouteResult routeResult = new Router.RouteResult(route, Collections.emptyMap());

        when(router.findRoute(any(), any())).thenReturn(routeResult);

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/test")
                .build();

        CompletableFuture<OutboundMessage> future = dispatcher.dispatch(message, context);
        OutboundMessage result = future.get();

        assertThat(result).isNull();
    }

    @Test
    void dispatch_handlerThrowsException_returns500() throws Exception {
        TestHandler handler = new TestHandler();
        RouteDefinition route = new RouteDefinition("/test", null, handler,
                TestHandler.class.getMethod("handleThrows"), Void.class, null);
        Router.RouteResult routeResult = new Router.RouteResult(route, Collections.emptyMap());

        when(router.findRoute(any(), any())).thenReturn(routeResult);

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/test")
                .build();

        CompletableFuture<OutboundMessage> future = dispatcher.dispatch(message, context);
        OutboundMessage result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(500);
    }

    @Test
    void dispatch_handlerWithContextParameter_injectsContext() throws Exception {
        TestHandler handler = new TestHandler();
        RouteDefinition route = new RouteDefinition("/test", null, handler,
                TestHandler.class.getMethod("handleWithContext", NettyContext.class), Void.class, null);
        Router.RouteResult routeResult = new Router.RouteResult(route, Collections.emptyMap());

        when(router.findRoute(any(), any())).thenReturn(routeResult);

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/test")
                .build();

        CompletableFuture<OutboundMessage> future = dispatcher.dispatch(message, context);
        OutboundMessage result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.getPayload()).isEqualTo("context");
    }

    @Test
    void dispatch_handlerWithInboundMessage_injectsMessage() throws Exception {
        TestHandler handler = new TestHandler();
        RouteDefinition route = new RouteDefinition("/test", null, handler,
                TestHandler.class.getMethod("handleWithMessage", InboundMessage.class), Void.class, null);
        Router.RouteResult routeResult = new Router.RouteResult(route, Collections.emptyMap());

        when(router.findRoute(any(), any())).thenReturn(routeResult);

        InboundMessage message = InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey("/test")
                .build();

        CompletableFuture<OutboundMessage> future = dispatcher.dispatch(message, context);
        OutboundMessage result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.getPayload()).isEqualTo("message");
    }

    @Test
    void getArgumentResolvers_returnsComposite() {
        ArgumentResolverComposite resolvers = dispatcher.getArgumentResolvers();
        assertThat(resolvers).isNotNull();
    }

    // Test handler class
    public static class TestHandler {
        public String handleSimple() {
            return "OK";
        }

        public OutboundMessage handleReturnsOutbound() {
            return OutboundMessage.status(201, "Created");
        }

        public CompletableFuture<String> handleAsync() {
            return CompletableFuture.completedFuture("async result");
        }

        public String handleReturnsNull() {
            return null;
        }

        public String handleThrows() {
            throw new RuntimeException("Test error");
        }

        public String handleWithContext(NettyContext ctx) {
            return ctx != null ? "context" : "null";
        }

        public String handleWithMessage(InboundMessage msg) {
            return msg != null ? "message" : "null";
        }
    }
}
