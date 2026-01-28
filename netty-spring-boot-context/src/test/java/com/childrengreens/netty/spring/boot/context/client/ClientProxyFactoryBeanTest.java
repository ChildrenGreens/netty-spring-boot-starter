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
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ClientProxyFactoryBean}.
 */
class ClientProxyFactoryBeanTest {

    private ClientProxyFactoryBean<TestClient> factoryBean;
    private ApplicationContext applicationContext;
    private ClientProxyFactory proxyFactory;

    @BeforeEach
    void setUp() {
        factoryBean = new ClientProxyFactoryBean<>();
        applicationContext = mock(ApplicationContext.class);
        proxyFactory = mock(ClientProxyFactory.class);

        factoryBean.setApplicationContext(applicationContext);
        factoryBean.setInterfaceType(TestClient.class);
    }

    @Test
    void getObjectType_returnsInterfaceType() {
        assertThat(factoryBean.getObjectType()).isEqualTo(TestClient.class);
    }

    @Test
    void isSingleton_returnsTrue() {
        assertThat(factoryBean.isSingleton()).isTrue();
    }

    @Test
    void getObject_createsProxy() throws Exception {
        TestClient mockProxy = mock(TestClient.class);
        when(applicationContext.getBean(ClientProxyFactory.class)).thenReturn(proxyFactory);
        when(proxyFactory.createProxy(TestClient.class)).thenReturn(mockProxy);

        TestClient result = factoryBean.getObject();

        assertThat(result).isSameAs(mockProxy);
        verify(proxyFactory).createProxy(TestClient.class);
    }

    @Test
    void getObject_cachesSingleton() throws Exception {
        TestClient mockProxy = mock(TestClient.class);
        when(applicationContext.getBean(ClientProxyFactory.class)).thenReturn(proxyFactory);
        when(proxyFactory.createProxy(TestClient.class)).thenReturn(mockProxy);

        TestClient result1 = factoryBean.getObject();
        TestClient result2 = factoryBean.getObject();

        assertThat(result1).isSameAs(result2);
        verify(proxyFactory, times(1)).createProxy(TestClient.class);
    }

    @NettyClient(name = "test-client")
    interface TestClient {
        @NettyRequest(type = "ping")
        String ping();
    }

}
