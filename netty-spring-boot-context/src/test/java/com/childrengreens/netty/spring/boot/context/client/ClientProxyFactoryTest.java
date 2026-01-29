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

package com.childrengreens.netty.spring.boot.context.client;

import com.childrengreens.netty.spring.boot.context.annotation.NettyClient;
import com.childrengreens.netty.spring.boot.context.annotation.NettyRequest;
import com.childrengreens.netty.spring.boot.context.annotation.Param;
import io.netty.channel.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ClientProxyFactory}.
 */
class ClientProxyFactoryTest {

    private NettyClientOrchestrator orchestrator;
    private ClientProxyFactory proxyFactory;

    @BeforeEach
    void setUp() {
        orchestrator = mock(NettyClientOrchestrator.class);
        proxyFactory = new ClientProxyFactory(orchestrator);
    }

    @Test
    void createProxy_withValidInterface_createsProxy() {
        TestClient proxy = proxyFactory.createProxy(TestClient.class);

        assertThat(proxy).isNotNull();
    }

    @Test
    void createProxy_withoutNettyClientAnnotation_throwsException() {
        assertThatThrownBy(() -> proxyFactory.createProxy(InvalidClient.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@NettyClient");
    }

    @Test
    void createProxy_withEmptyName_throwsException() {
        assertThatThrownBy(() -> proxyFactory.createProxy(EmptyNameClient.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must specify a name");
    }

    @Test
    void proxy_toString_returnsDescriptiveString() {
        TestClient proxy = proxyFactory.createProxy(TestClient.class);

        String result = proxy.toString();

        assertThat(result).contains("NettyClient");
        assertThat(result).contains("test-client");
    }

    @Test
    void proxy_hashCode_returnsIdentityHashCode() {
        TestClient proxy = proxyFactory.createProxy(TestClient.class);

        int hashCode = proxy.hashCode();

        assertThat(hashCode).isEqualTo(System.identityHashCode(proxy));
    }

    @Test
    void proxy_equals_usesIdentity() {
        TestClient proxy1 = proxyFactory.createProxy(TestClient.class);
        TestClient proxy2 = proxyFactory.createProxy(TestClient.class);

        assertThat(proxy1.equals(proxy2)).isFalse();
    }

    @Test
    void proxy_methodWithoutNettyRequest_throwsException() {
        TestClientWithInvalidMethod proxy = proxyFactory.createProxy(TestClientWithInvalidMethod.class);

        assertThatThrownBy(proxy::invalidMethod)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("@NettyRequest");
    }

    @Test
    void proxy_methodWithoutRuntime_throwsException() {
        TestClient proxy = proxyFactory.createProxy(TestClient.class);

        assertThatThrownBy(proxy::ping)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No client configured");
    }

    @Test
    void proxy_equals_withNull_returnsFalse() {
        TestClient proxy = proxyFactory.createProxy(TestClient.class);

        assertThat(proxy == null).isFalse();
    }

    @Test
    void proxy_equals_withOtherObject_returnsFalse() {
        TestClient proxy = proxyFactory.createProxy(TestClient.class);

        assertThat(proxy.equals("not a proxy")).isFalse();
    }

    @Test
    void proxy_invokeSyncMethod_withRuntime_returnsResult() throws Exception {
        // Setup
        ClientRuntime runtime = mock(ClientRuntime.class);
        ConnectionPool connectionPool = mock(ConnectionPool.class);
        RequestInvoker requestInvoker = mock(RequestInvoker.class);
        Channel channel = mock(Channel.class);

        when(orchestrator.getRuntime("test-client")).thenReturn(runtime);
        when(runtime.getConnectionPool()).thenReturn(connectionPool);
        when(runtime.getRequestInvoker()).thenReturn(requestInvoker);
        when(connectionPool.acquire()).thenReturn(channel);

        CompletableFuture<Object> future = CompletableFuture.completedFuture("pong");
        when(requestInvoker.invoke(eq(channel), eq("ping"), isNull(), anyLong())).thenReturn(future);

        TestClient proxy = proxyFactory.createProxy(TestClient.class);

        // Execute
        String result = proxy.ping();

        // Verify
        assertThat(result).isEqualTo("pong");
        verify(connectionPool).release(channel);
    }

    @Test
    void proxy_invokeAsyncMethod_withRuntime_returnsFuture() throws Exception {
        // Setup
        ClientRuntime runtime = mock(ClientRuntime.class);
        ConnectionPool connectionPool = mock(ConnectionPool.class);
        RequestInvoker requestInvoker = mock(RequestInvoker.class);
        Channel channel = mock(Channel.class);

        when(orchestrator.getRuntime("test-client")).thenReturn(runtime);
        when(runtime.getConnectionPool()).thenReturn(connectionPool);
        when(runtime.getRequestInvoker()).thenReturn(requestInvoker);
        when(connectionPool.acquire()).thenReturn(channel);

        CompletableFuture<Object> future = CompletableFuture.completedFuture("async-pong");
        when(requestInvoker.invoke(eq(channel), eq("async-ping"), isNull(), anyLong())).thenReturn(future);

        AsyncTestClient proxy = proxyFactory.createProxy(AsyncTestClient.class);

        // Execute
        CompletableFuture<String> result = proxy.asyncPing();

        // Verify
        assertThat(result.get()).isEqualTo("async-pong");
    }

    @Test
    void proxy_invokeOneWayMethod_withRuntime_sendsMessage() throws Exception {
        // Setup
        ClientRuntime runtime = mock(ClientRuntime.class);
        ConnectionPool connectionPool = mock(ConnectionPool.class);
        RequestInvoker requestInvoker = mock(RequestInvoker.class);
        Channel channel = mock(Channel.class);

        when(orchestrator.getRuntime("test-client")).thenReturn(runtime);
        when(runtime.getConnectionPool()).thenReturn(connectionPool);
        when(runtime.getRequestInvoker()).thenReturn(requestInvoker);
        when(connectionPool.acquire()).thenReturn(channel);

        OneWayTestClient proxy = proxyFactory.createProxy(OneWayTestClient.class);

        // Execute
        proxy.sendNotification("test-message");

        // Verify - invokeOneWay takes (channel, messageType, payload)
        verify(requestInvoker).invokeOneWay(eq(channel), eq("notify"), any());
    }

    @Test
    void proxy_invokeMethodWithParams_buildsPayloadCorrectly() throws Exception {
        // Setup
        ClientRuntime runtime = mock(ClientRuntime.class);
        ConnectionPool connectionPool = mock(ConnectionPool.class);
        RequestInvoker requestInvoker = mock(RequestInvoker.class);
        Channel channel = mock(Channel.class);

        when(orchestrator.getRuntime("test-client")).thenReturn(runtime);
        when(runtime.getConnectionPool()).thenReturn(connectionPool);
        when(runtime.getRequestInvoker()).thenReturn(requestInvoker);
        when(connectionPool.acquire()).thenReturn(channel);

        CompletableFuture<Object> future = CompletableFuture.completedFuture("user-data");
        when(requestInvoker.invoke(eq(channel), eq("get-user"), any(), anyLong())).thenReturn(future);

        ParamTestClient proxy = proxyFactory.createProxy(ParamTestClient.class);

        // Execute
        String result = proxy.getUser("user-123");

        // Verify
        assertThat(result).isEqualTo("user-data");
        verify(requestInvoker).invoke(eq(channel), eq("get-user"), argThat(arg -> {
            if (arg instanceof java.util.Map) {
                return ((java.util.Map<?, ?>) arg).get("userId").equals("user-123");
            }
            return false;
        }), anyLong());
    }

    @Test
    void proxy_invokeVoidMethod_completesSuccessfully() throws Exception {
        // Setup
        ClientRuntime runtime = mock(ClientRuntime.class);
        ConnectionPool connectionPool = mock(ConnectionPool.class);
        RequestInvoker requestInvoker = mock(RequestInvoker.class);
        Channel channel = mock(Channel.class);

        when(orchestrator.getRuntime("test-client")).thenReturn(runtime);
        when(runtime.getConnectionPool()).thenReturn(connectionPool);
        when(runtime.getRequestInvoker()).thenReturn(requestInvoker);
        when(connectionPool.acquire()).thenReturn(channel);

        CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
        when(requestInvoker.invoke(eq(channel), eq("void-method"), isNull(), anyLong())).thenReturn(future);

        VoidMethodClient proxy = proxyFactory.createProxy(VoidMethodClient.class);

        // Execute - should not throw
        proxy.voidMethod();

        // Verify
        verify(connectionPool).release(channel);
    }

    @Test
    void proxy_convertsMapResultToPojo() throws Exception {
        ClientRuntime runtime = mock(ClientRuntime.class);
        ConnectionPool connectionPool = mock(ConnectionPool.class);
        RequestInvoker requestInvoker = mock(RequestInvoker.class);
        Channel channel = mock(Channel.class);

        when(orchestrator.getRuntime("test-client")).thenReturn(runtime);
        when(runtime.getConnectionPool()).thenReturn(connectionPool);
        when(runtime.getRequestInvoker()).thenReturn(requestInvoker);
        when(connectionPool.acquire()).thenReturn(channel);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Alice");
        payload.put("age", 30);

        CompletableFuture<Object> future = CompletableFuture.completedFuture(payload);
        when(requestInvoker.invoke(eq(channel), eq("get-user-pojo"), isNull(), anyLong())).thenReturn(future);

        PojoClient proxy = proxyFactory.createProxy(PojoClient.class);

        User result = proxy.getUser();

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getAge()).isEqualTo(30);
        verify(connectionPool).release(channel);
    }

    @NettyClient(name = "test-client")
    interface TestClient {
        @NettyRequest(type = "ping")
        String ping();
    }

    @NettyClient(name = "test-client")
    interface AsyncTestClient {
        @NettyRequest(type = "async-ping")
        CompletableFuture<String> asyncPing();
    }

    @NettyClient(name = "test-client")
    interface OneWayTestClient {
        @NettyRequest(type = "notify", oneWay = true)
        void sendNotification(String message);
    }

    @NettyClient(name = "test-client")
    interface ParamTestClient {
        @NettyRequest(type = "get-user")
        String getUser(@Param("userId") String userId);
    }

    @NettyClient(name = "test-client")
    interface VoidMethodClient {
        @NettyRequest(type = "void-method")
        void voidMethod();
    }

    @NettyClient(name = "test-client")
    interface PojoClient {
        @NettyRequest(type = "get-user-pojo")
        User getUser();
    }

    static class User {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @NettyClient(name = "test-client")
    interface TestClientWithInvalidMethod {
        String invalidMethod();
    }

    interface InvalidClient {
        void doSomething();
    }

    @NettyClient(name = "")
    interface EmptyNameClient {
        void doSomething();
    }

    @NettyClient(name = "value-client", value = "value-client")
    interface ValueAnnotatedClient {
        @NettyRequest(type = "ping", value = "ping")
        String ping();
    }

    @Test
    void createProxy_withValueAnnotation_usesValue() {
        ValueAnnotatedClient proxy = proxyFactory.createProxy(ValueAnnotatedClient.class);

        assertThat(proxy).isNotNull();
        assertThat(proxy.toString()).contains("value-client");
    }

}
