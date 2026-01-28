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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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

        assertThat(proxy1.equals(proxy1)).isTrue();
        assertThat(proxy1.equals(proxy2)).isFalse();
    }

    @Test
    void proxy_methodWithoutNettyRequest_throwsException() {
        TestClientWithInvalidMethod proxy = proxyFactory.createProxy(TestClientWithInvalidMethod.class);

        assertThatThrownBy(() -> proxy.invalidMethod())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("@NettyRequest");
    }

    @Test
    void proxy_methodWithoutRuntime_throwsException() {
        TestClient proxy = proxyFactory.createProxy(TestClient.class);

        assertThatThrownBy(() -> proxy.ping())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No client configured");
    }

    @NettyClient(name = "test-client")
    interface TestClient {
        @NettyRequest(type = "ping")
        String ping();
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
