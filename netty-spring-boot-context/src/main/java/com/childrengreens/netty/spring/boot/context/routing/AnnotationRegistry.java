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

package com.childrengreens.netty.spring.boot.context.routing;

import com.childrengreens.netty.spring.boot.context.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Bean post-processor that scans for Netty handler annotations.
 *
 * <p>This processor detects beans annotated with {@link NettyController} or
 * {@link NettyMessageController} and registers their handler methods with
 * the {@link Router}.
 *
 * <p>Supported HTTP method annotations:
 * <ul>
 * <li>{@link NettyHttpGet} - GET requests</li>
 * <li>{@link NettyHttpPost} - POST requests</li>
 * <li>{@link NettyHttpPut} - PUT requests</li>
 * <li>{@link NettyHttpDelete} - DELETE requests</li>
 * <li>{@link NettyHttpMapping} - Generic HTTP mapping</li>
 * </ul>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public class AnnotationRegistry implements BeanPostProcessor, BeanFactoryAware, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationRegistry.class);

    private BeanFactory beanFactory;
    private Router router;

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() {
        this.router = beanFactory.getBean(Router.class);
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);

        // Check for NettyController
        NettyController controller = AnnotationUtils.findAnnotation(targetClass, NettyController.class);
        if (controller != null) {
            processController(bean, targetClass, controller);
        }

        // Check for NettyMessageController
        NettyMessageController messageController = AnnotationUtils.findAnnotation(
                targetClass, NettyMessageController.class);
        if (messageController != null) {
            processMessageController(bean, targetClass, messageController);
        }

        return bean;
    }

    /**
     * Process HTTP/WebSocket controller.
     */
    private void processController(Object bean, Class<?> targetClass, NettyController controller) {
        String basePath = controller.path();

        for (Method method : targetClass.getDeclaredMethods()) {
            // Process HTTP mappings
            processHttpMappings(bean, method, basePath);

            // Process WebSocket mappings
            processWsMappings(bean, method, basePath);
        }
    }

    /**
     * Process message controller.
     */
    private void processMessageController(Object bean, Class<?> targetClass,
                                           NettyMessageController controller) {
        String serverName = controller.server();

        for (Method method : targetClass.getDeclaredMethods()) {
            NettyMessageMapping mapping = AnnotationUtils.findAnnotation(method, NettyMessageMapping.class);
            if (mapping != null) {
                String type = mapping.value().isEmpty() ? mapping.type() : mapping.value();
                Class<?> payloadType = findPayloadType(method);

                RouteDefinition route = new RouteDefinition(
                        type, null, bean, method, payloadType, serverName);
                router.register(route);
                logger.debug("Registered message mapping: {} -> {}", type, method);
            }
        }
    }

    /**
     * Process HTTP mapping annotations.
     */
    private void processHttpMappings(Object bean, Method method, String basePath) {
        // Check for @NettyHttpGet
        NettyHttpGet httpGet = AnnotationUtils.findAnnotation(method, NettyHttpGet.class);
        if (httpGet != null) {
            String[] paths = httpGet.value().length > 0 ? httpGet.value() : httpGet.path();
            registerHttpRoutes(bean, method, basePath, paths, "GET");
        }

        // Check for @NettyHttpPost
        NettyHttpPost httpPost = AnnotationUtils.findAnnotation(method, NettyHttpPost.class);
        if (httpPost != null) {
            String[] paths = httpPost.value().length > 0 ? httpPost.value() : httpPost.path();
            registerHttpRoutes(bean, method, basePath, paths, "POST");
        }

        // Check for @NettyHttpPut
        NettyHttpPut httpPut = AnnotationUtils.findAnnotation(method, NettyHttpPut.class);
        if (httpPut != null) {
            String[] paths = httpPut.value().length > 0 ? httpPut.value() : httpPut.path();
            registerHttpRoutes(bean, method, basePath, paths, "PUT");
        }

        // Check for @NettyHttpDelete
        NettyHttpDelete httpDelete = AnnotationUtils.findAnnotation(method, NettyHttpDelete.class);
        if (httpDelete != null) {
            String[] paths = httpDelete.value().length > 0 ? httpDelete.value() : httpDelete.path();
            registerHttpRoutes(bean, method, basePath, paths, "DELETE");
        }

        // Check for generic @NettyHttpMapping
        NettyHttpMapping httpMapping = AnnotatedElementUtils.findMergedAnnotation(method, NettyHttpMapping.class);
        if (httpMapping != null && httpGet == null && httpPost == null
                && httpPut == null && httpDelete == null) {
            String[] paths = httpMapping.value().length > 0 ? httpMapping.value() : httpMapping.path();
            String[] methods = httpMapping.method();
            for (String httpMethod : methods) {
                registerHttpRoutes(bean, method, basePath, paths, httpMethod);
            }
        }
    }

    /**
     * Process WebSocket mapping annotations.
     */
    private void processWsMappings(Object bean, Method method, String basePath) {
        NettyWsOnOpen wsOnOpen = AnnotationUtils.findAnnotation(method, NettyWsOnOpen.class);
        if (wsOnOpen != null) {
            String path = combinePath(basePath, wsOnOpen.value().isEmpty() ? wsOnOpen.path() : wsOnOpen.value());
            registerWsRoute(bean, method, path, "OPEN");
        }

        NettyWsOnText wsOnText = AnnotationUtils.findAnnotation(method, NettyWsOnText.class);
        if (wsOnText != null) {
            String path = combinePath(basePath, wsOnText.value().isEmpty() ? wsOnText.path() : wsOnText.value());
            registerWsRoute(bean, method, path, "TEXT");
        }

        NettyWsOnBinary wsOnBinary = AnnotationUtils.findAnnotation(method, NettyWsOnBinary.class);
        if (wsOnBinary != null) {
            String path = combinePath(basePath, wsOnBinary.value().isEmpty() ? wsOnBinary.path() : wsOnBinary.value());
            registerWsRoute(bean, method, path, "BINARY");
        }

        NettyWsOnClose wsOnClose = AnnotationUtils.findAnnotation(method, NettyWsOnClose.class);
        if (wsOnClose != null) {
            String path = combinePath(basePath, wsOnClose.value().isEmpty() ? wsOnClose.path() : wsOnClose.value());
            registerWsRoute(bean, method, path, "CLOSE");
        }
    }

    /**
     * Register HTTP routes.
     */
    private void registerHttpRoutes(Object bean, Method method, String basePath,
                                     String[] paths, String httpMethod) {
        Class<?> payloadType = findPayloadType(method);
        for (String path : paths) {
            String fullPath = combinePath(basePath, path);
            RouteDefinition route = new RouteDefinition(
                    fullPath, httpMethod, bean, method, payloadType, null);
            router.register(route);
            logger.debug("Registered HTTP mapping: {} {} -> {}", httpMethod, fullPath, method);
        }
    }

    /**
     * Register WebSocket route.
     */
    private void registerWsRoute(Object bean, Method method, String path, String eventType) {
        Class<?> payloadType = findPayloadType(method);
        String routeKey = "WS:" + eventType + ":" + path;
        RouteDefinition route = new RouteDefinition(
                routeKey, null, bean, method, payloadType, null);
        router.register(route);
        logger.debug("Registered WebSocket mapping: {} {} -> {}", eventType, path, method);
    }

    /**
     * Find the payload type from method parameters.
     */
    private Class<?> findPayloadType(Method method) {
        for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(Body.class)) {
                return param.getType();
            }
        }
        // Default to first non-context parameter
        for (Parameter param : method.getParameters()) {
            Class<?> type = param.getType();
            if (!isContextType(type)) {
                return type;
            }
        }
        return Object.class;
    }

    /**
     * Check if the type is a context type.
     */
    private boolean isContextType(Class<?> type) {
        return type.getName().contains("Context") ||
               type.getName().contains("Channel") ||
               type.getName().contains("Message");
    }

    /**
     * Combine base path with relative path.
     */
    private String combinePath(String basePath, String path) {
        if (basePath == null || basePath.isEmpty()) {
            return path.startsWith("/") ? path : "/" + path;
        }
        String base = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        String relative = path.startsWith("/") ? path : "/" + path;
        return base + relative;
    }

}
