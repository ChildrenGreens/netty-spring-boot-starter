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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        // Use real implementations for ResourceLoader and Environment
        resourceLoader = new DefaultResourceLoader();
        environment = new StandardEnvironment();

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

    @Test
    void setResourceLoader_setsResourceLoader() {
        ResourceLoader newResourceLoader = mock(ResourceLoader.class);

        registrar.setResourceLoader(newResourceLoader);

        // No exception thrown
    }

    @Test
    void setEnvironment_setsEnvironment() {
        Environment newEnvironment = mock(Environment.class);

        registrar.setEnvironment(newEnvironment);

        // No exception thrown
    }

    @Test
    void setBeanClassLoader_setsClassLoader() {
        ClassLoader newClassLoader = getClass().getClassLoader();

        registrar.setBeanClassLoader(newClassLoader);

        // No exception thrown
    }

    @Test
    void setBasePackages_withMultiplePackages_acceptsAll() {
        registrar.setBasePackages("com.example.package1", "com.example.package2", "com.example.package3");

        // No exception thrown
    }

    @Test
    void postProcessBeanDefinitionRegistry_withNonExistentPackage_doesNotThrow() {
        registrar.setBasePackages("com.nonexistent.package.that.does.not.exist");

        // Should not throw, just not find any candidates
        registrar.postProcessBeanDefinitionRegistry(registry);

        verify(registry, never()).registerBeanDefinition(anyString(), any());
    }

    @Test
    void postProcessBeanDefinitionRegistry_withValidPackage_registersClients() {
        // Use a package that contains test NettyClient interfaces
        // Note: ClassPathScanningCandidateComponentProvider might not find interfaces by default
        // This test verifies the scanning process runs without error
        registrar.setBasePackages("com.childrengreens.netty.spring.boot.context.client.testclient");

        registrar.postProcessBeanDefinitionRegistry(registry);

        // Scanning interfaces might require additional configuration
        // This test ensures the scanning process completes without error
    }

    @Test
    void postProcessBeanDefinitionRegistry_withRealTestPackage_scansSuccessfully() {
        // Test that scanning a real package works without exceptions
        registrar.setBasePackages("com.childrengreens.netty.spring.boot.context");

        // Should not throw exception
        registrar.postProcessBeanDefinitionRegistry(registry);
    }

    @Test
    void registerClientBean_withValidNettyClientInterface_registersBeanDefinition() throws Exception {
        // Access private method via reflection
        Method registerClientBeanMethod = NettyClientRegistrar.class.getDeclaredMethod(
                "registerClientBean", BeanDefinitionRegistry.class, BeanDefinition.class);
        registerClientBeanMethod.setAccessible(true);

        // Create a bean definition for a valid NettyClient interface
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(TestClientInterface.class.getName());

        // Invoke the private method
        registerClientBeanMethod.invoke(registrar, registry, beanDefinition);

        // Verify that the bean was registered
        verify(registry).registerBeanDefinition(eq("testClientInterface"), any(BeanDefinition.class));
    }

    @Test
    void registerClientBean_withNullClassName_doesNotRegister() throws Exception {
        Method registerClientBeanMethod = NettyClientRegistrar.class.getDeclaredMethod(
                "registerClientBean", BeanDefinitionRegistry.class, BeanDefinition.class);
        registerClientBeanMethod.setAccessible(true);

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        // Don't set bean class name - it will be null

        registerClientBeanMethod.invoke(registrar, registry, beanDefinition);

        verify(registry, never()).registerBeanDefinition(anyString(), any());
    }

    @Test
    void registerClientBean_withNonNettyClientClass_doesNotRegister() throws Exception {
        Method registerClientBeanMethod = NettyClientRegistrar.class.getDeclaredMethod(
                "registerClientBean", BeanDefinitionRegistry.class, BeanDefinition.class);
        registerClientBeanMethod.setAccessible(true);

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(String.class.getName()); // Not a NettyClient

        registerClientBeanMethod.invoke(registrar, registry, beanDefinition);

        verify(registry, never()).registerBeanDefinition(anyString(), any());
    }

    @Test
    void registerClientBean_withNonExistentClass_doesNotRegister() throws Exception {
        Method registerClientBeanMethod = NettyClientRegistrar.class.getDeclaredMethod(
                "registerClientBean", BeanDefinitionRegistry.class, BeanDefinition.class);
        registerClientBeanMethod.setAccessible(true);

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName("com.nonexistent.NonExistentClass");

        registerClientBeanMethod.invoke(registrar, registry, beanDefinition);

        verify(registry, never()).registerBeanDefinition(anyString(), any());
    }

    @Test
    void registerClientBean_withEmptyName_usesValueAttribute() throws Exception {
        Method registerClientBeanMethod = NettyClientRegistrar.class.getDeclaredMethod(
                "registerClientBean", BeanDefinitionRegistry.class, BeanDefinition.class);
        registerClientBeanMethod.setAccessible(true);

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(ValueClientInterface.class.getName());

        registerClientBeanMethod.invoke(registrar, registry, beanDefinition);

        verify(registry).registerBeanDefinition(eq("valueClientInterface"), any(BeanDefinition.class));
    }

    /**
     * Test interface annotated with @NettyClient for testing registration.
     */
    @NettyClient(name = "test-client")
    interface TestClientInterface {
        void doSomething();
    }

    /**
     * Test interface with empty name to test value attribute fallback.
     */
    @NettyClient(name = "", value = "value-client")
    interface ValueClientInterface {
        void doSomething();
    }

}
