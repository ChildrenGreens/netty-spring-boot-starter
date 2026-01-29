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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Registers Netty client proxies as Spring beans.
 *
 * <p>This registrar scans for interfaces annotated with {@link NettyClient}
 * and registers them as bean definitions that will be resolved by the
 * {@link ClientProxyFactoryBean}.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see NettyClient
 * @see ClientProxyFactory
 */
public class NettyClientRegistrar implements BeanDefinitionRegistryPostProcessor,
        ResourceLoaderAware, EnvironmentAware, BeanClassLoaderAware {

    private static final Logger logger = LoggerFactory.getLogger(NettyClientRegistrar.class);

    private ResourceLoader resourceLoader;
    private Environment environment;
    private ClassLoader classLoader;
    private String[] basePackages;

    /**
     * Set the base packages to scan for client interfaces.
     * @param basePackages the base packages
     */
    public void setBasePackages(String... basePackages) {
        this.basePackages = basePackages;
    }

    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (basePackages == null || basePackages.length == 0) {
            logger.debug("No base packages configured for NettyClient scanning");
            return;
        }

        ClassPathScanningCandidateComponentProvider scanner = createScanner();

        for (String basePackage : basePackages) {
            if (!StringUtils.hasText(basePackage)) {
                continue;
            }

            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);

            for (BeanDefinition candidate : candidates) {
                registerClientBean(registry, candidate);
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // No additional processing needed
    }

    /**
     * Create a scanner for finding NettyClient interfaces.
     */
    private ClassPathScanningCandidateComponentProvider createScanner() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, environment);

        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(NettyClient.class));

        return scanner;
    }

    /**
     * Register a client bean definition.
     */
    private void registerClientBean(BeanDefinitionRegistry registry, BeanDefinition candidate) {
        String className = candidate.getBeanClassName();
        if (className == null) {
            return;
        }

        try {
            Class<?> interfaceType = ClassUtils.forName(className, classLoader);
            NettyClient annotation = interfaceType.getAnnotation(NettyClient.class);

            if (annotation == null) {
                return;
            }

            String clientName = annotation.name();
            if (clientName.isEmpty()) {
                clientName = annotation.value();
            }

            // Use interface simple name as bean name
            String beanName = StringUtils.uncapitalize(interfaceType.getSimpleName());

            logger.info("Registering Netty client bean: {} -> {}", beanName, interfaceType.getName());

            BeanDefinitionBuilder builder = BeanDefinitionBuilder
                    .genericBeanDefinition(ClientProxyFactoryBean.class);
            builder.addPropertyValue("interfaceType", interfaceType);
            builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

            registry.registerBeanDefinition(beanName, builder.getBeanDefinition());

        } catch (ClassNotFoundException e) {
            logger.warn("Could not load class: {}", className, e);
        }
    }

}
