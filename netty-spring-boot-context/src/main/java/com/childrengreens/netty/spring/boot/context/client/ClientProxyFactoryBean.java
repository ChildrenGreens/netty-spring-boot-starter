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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

/**
 * Factory bean for creating Netty client proxies.
 *
 * <p>This factory bean creates a proxy for the specified interface type
 * using the {@link ClientProxyFactory}.
 *
 * @param <T> the client interface type
 * @author ChildrenGreens
 * @since 0.0.1
 * @see ClientProxyFactory
 */
public class ClientProxyFactoryBean<T> implements FactoryBean<T>, ApplicationContextAware {

    private Class<T> interfaceType;
    private ApplicationContext applicationContext;
    private T proxy;

    /**
     * Set the interface type.
     * @param interfaceType the interface type
     */
    public void setInterfaceType(Class<T> interfaceType) {
        this.interfaceType = interfaceType;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public T getObject() throws Exception {
        if (proxy == null) {
            ClientProxyFactory proxyFactory = applicationContext.getBean(ClientProxyFactory.class);
            proxy = proxyFactory.createProxy(interfaceType);
        }
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceType;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
