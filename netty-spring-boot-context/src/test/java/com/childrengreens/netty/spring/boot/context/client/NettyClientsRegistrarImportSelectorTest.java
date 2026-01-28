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

import com.childrengreens.netty.spring.boot.context.annotation.EnableNettyClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link NettyClientsRegistrarImportSelector}.
 */
class NettyClientsRegistrarImportSelectorTest {

    private NettyClientsRegistrarImportSelector selector;
    private BeanDefinitionRegistry registry;
    private AnnotationMetadata metadata;

    @BeforeEach
    void setUp() {
        selector = new NettyClientsRegistrarImportSelector();
        registry = mock(BeanDefinitionRegistry.class);
        metadata = mock(AnnotationMetadata.class);
    }

    @Test
    void registerBeanDefinitions_withNoAnnotation_doesNothing() {
        when(metadata.getAnnotationAttributes(EnableNettyClients.class.getName())).thenReturn(null);

        selector.registerBeanDefinitions(metadata, registry);

        verify(registry, never()).registerBeanDefinition(anyString(), any());
    }

    @Test
    void registerBeanDefinitions_withValuePackages_registersWithPackages() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("value", new String[]{"com.example.clients"});
        attrs.put("basePackages", new String[]{});
        attrs.put("basePackageClasses", new Class<?>[]{});

        when(metadata.getAnnotationAttributes(EnableNettyClients.class.getName())).thenReturn(attrs);

        selector.registerBeanDefinitions(metadata, registry);

        verify(registry).registerBeanDefinition(eq("nettyClientRegistrar"), any());
    }

    @Test
    void registerBeanDefinitions_withBasePackages_registersWithPackages() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("value", new String[]{});
        attrs.put("basePackages", new String[]{"com.example.clients"});
        attrs.put("basePackageClasses", new Class<?>[]{});

        when(metadata.getAnnotationAttributes(EnableNettyClients.class.getName())).thenReturn(attrs);

        selector.registerBeanDefinitions(metadata, registry);

        verify(registry).registerBeanDefinition(eq("nettyClientRegistrar"), any());
    }

    @Test
    void registerBeanDefinitions_withBasePackageClasses_registersWithPackages() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("value", new String[]{});
        attrs.put("basePackages", new String[]{});
        attrs.put("basePackageClasses", new Class<?>[]{String.class});

        when(metadata.getAnnotationAttributes(EnableNettyClients.class.getName())).thenReturn(attrs);

        selector.registerBeanDefinitions(metadata, registry);

        verify(registry).registerBeanDefinition(eq("nettyClientRegistrar"), any());
    }

    @Test
    void registerBeanDefinitions_withNoPackages_usesImportingClassPackage() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("value", new String[]{});
        attrs.put("basePackages", new String[]{});
        attrs.put("basePackageClasses", new Class<?>[]{});

        when(metadata.getAnnotationAttributes(EnableNettyClients.class.getName())).thenReturn(attrs);
        when(metadata.getClassName()).thenReturn("com.example.MyApplication");

        selector.registerBeanDefinitions(metadata, registry);

        verify(registry).registerBeanDefinition(eq("nettyClientRegistrar"), any());
    }

}
