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
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Import selector for enabling Netty clients.
 *
 * <p>This selector registers the {@link NettyClientRegistrar} with the
 * appropriate base packages to scan.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see EnableNettyClients
 */
public class NettyClientsRegistrarImportSelector implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        @NonNull BeanDefinitionRegistry registry) {
        AnnotationAttributes attrs = AnnotationAttributes.fromMap(
                importingClassMetadata.getAnnotationAttributes(EnableNettyClients.class.getName()));

        if (attrs == null) {
            return;
        }

        List<String> basePackages = new ArrayList<>();

        // Add packages from value()
        basePackages.addAll(Arrays.asList(attrs.getStringArray("value")));

        // Add packages from basePackages()
        basePackages.addAll(Arrays.asList(attrs.getStringArray("basePackages")));

        // Add packages from basePackageClasses()
        for (Class<?> clazz : attrs.getClassArray("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        // If no packages specified, use the package of the importing class
        if (basePackages.isEmpty()) {
            String packageName = ClassUtils.getPackageName(importingClassMetadata.getClassName());
            basePackages.add(packageName);
        }

        // Register the NettyClientRegistrar
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(NettyClientRegistrar.class);
        builder.addPropertyValue("basePackages", basePackages.toArray(new String[0]));

        registry.registerBeanDefinition("nettyClientRegistrar", builder.getBeanDefinition());
    }

}
