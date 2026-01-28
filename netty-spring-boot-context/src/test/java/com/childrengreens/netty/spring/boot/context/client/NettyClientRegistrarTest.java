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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link NettyClientRegistrar}.
 */
class NettyClientRegistrarTest {

    private NettyClientRegistrar registrar;
    private BeanDefinitionRegistry registry;
    private ResourceLoader resourceLoader;
    private Environment environment;

    @BeforeEach
    void setUp() {
        registrar = new NettyClientRegistrar();
        registry = mock(BeanDefinitionRegistry.class);
        resourceLoader = mock(ResourceLoader.class);
        environment = mock(Environment.class);

        registrar.setResourceLoader(resourceLoader);
        registrar.setEnvironment(environment);
        registrar.setBeanClassLoader(getClass().getClassLoader());
    }

    @Test
    void postProcessBeanDefinitionRegistry_withNoBasePackages_doesNothing() {
        registrar.postProcessBeanDefinitionRegistry(registry);

        verify(registry, never()).registerBeanDefinition(anyString(), any());
    }

    @Test
    void postProcessBeanDefinitionRegistry_withEmptyBasePackages_doesNothing() {
        registrar.setBasePackages();

        registrar.postProcessBeanDefinitionRegistry(registry);

        verify(registry, never()).registerBeanDefinition(anyString(), any());
    }

    @Test
    void postProcessBeanDefinitionRegistry_withEmptyStringPackage_skips() {
        registrar.setBasePackages("", " ");

        registrar.postProcessBeanDefinitionRegistry(registry);

        verify(registry, never()).registerBeanDefinition(anyString(), any());
    }

    @Test
    void postProcessBeanFactory_doesNothing() {
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);

        registrar.postProcessBeanFactory(beanFactory);

        // No interactions expected
        verifyNoInteractions(beanFactory);
    }

    @Test
    void setBasePackages_setsPackages() {
        registrar.setBasePackages("com.example.clients");

        // No exception thrown
    }

}
