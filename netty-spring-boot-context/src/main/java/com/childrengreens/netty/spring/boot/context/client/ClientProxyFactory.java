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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Factory for creating dynamic proxies for {@link NettyClient} interfaces.
 *
 * <p>This factory generates JDK dynamic proxies that translate method calls
 * into network requests using the configured client runtime.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see NettyClient
 * @see NettyRequest
 */
public class ClientProxyFactory {

    private static final Logger logger = LoggerFactory.getLogger(ClientProxyFactory.class);
    private static final ObjectMapper OBJECT_MAPPER = createDefaultObjectMapper();

    private final NettyClientOrchestrator orchestrator;

    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }

    /**
     * Create a new ClientProxyFactory.
     * @param orchestrator the client orchestrator
     */
    public ClientProxyFactory(NettyClientOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Create a proxy for the given interface.
     * @param interfaceType the interface type
     * @param <T> the interface type
     * @return the proxy instance
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> interfaceType) {
        NettyClient clientAnnotation = interfaceType.getAnnotation(NettyClient.class);
        if (clientAnnotation == null) {
            throw new IllegalArgumentException("Interface must be annotated with @NettyClient: " + interfaceType.getName());
        }

        String clientName = clientAnnotation.name();
        if (clientName.isEmpty()) {
            clientName = clientAnnotation.value();
        }
        if (clientName.isEmpty()) {
            throw new IllegalArgumentException("@NettyClient must specify a name: " + interfaceType.getName());
        }

        final String finalClientName = clientName;
        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                new ClientInvocationHandler(finalClientName, interfaceType)
        );
    }

    /**
     * Invocation handler for client proxies.
     */
    private class ClientInvocationHandler implements InvocationHandler {

        private final String clientName;
        private final Class<?> interfaceType;

        ClientInvocationHandler(String clientName, Class<?> interfaceType) {
            this.clientName = clientName;
            this.interfaceType = interfaceType;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Handle Object methods
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }

            // Handle default methods
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }

            // Get NettyRequest annotation
            NettyRequest requestAnnotation = method.getAnnotation(NettyRequest.class);
            if (requestAnnotation == null) {
                throw new IllegalStateException("Method must be annotated with @NettyRequest: " + method.getName());
            }

            // Get client runtime
            ClientRuntime runtime = orchestrator.getRuntime(clientName);
            if (runtime == null) {
                throw new IllegalStateException("No client configured with name: " + clientName);
            }

            // Build request
            String messageType = requestAnnotation.type();
            if (messageType.isEmpty()) {
                messageType = requestAnnotation.value();
            }

            long timeout = requestAnnotation.timeout();
            boolean oneWay = requestAnnotation.oneWay();

            // Build payload from method arguments
            Object payload = buildPayload(method, args);

            // Acquire channel and invoke
            ConnectionPool connectionPool = runtime.getConnectionPool();
            RequestInvoker requestInvoker = runtime.getRequestInvoker();

            Channel channel = connectionPool.acquire();
            try {
                if (oneWay) {
                    requestInvoker.invokeOneWay(channel, messageType, payload);
                    return null;
                }

                CompletableFuture<Object> future = requestInvoker.invoke(channel, messageType, payload, timeout);

                // Handle return type
                Class<?> returnType = method.getReturnType();
                if (CompletableFuture.class.isAssignableFrom(returnType)) {
                    // Async - return the future directly
                    return future.whenComplete((result, ex) -> connectionPool.release(channel));
                } else if (returnType == void.class || returnType == Void.class) {
                    // Void return - wait but don't return value
                    future.whenComplete((result, ex) -> connectionPool.release(channel)).get();
                    return null;
                } else {
                    // Sync - block and wait for result
                    try {
                        Object result = future.get();
                        return convertResult(result, method);
                    } finally {
                        connectionPool.release(channel);
                    }
                }
            } catch (Exception e) {
                connectionPool.release(channel);
                throw e;
            }
        }

        /**
         * Handle Object class methods.
         */
        private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "toString" -> "NettyClient[" + clientName + "]@" + interfaceType.getSimpleName();
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        /**
         * Build request payload from method arguments.
         */
        private Object buildPayload(Method method, Object[] args) {
            if (args == null || args.length == 0) {
                return null;
            }

            Parameter[] parameters = method.getParameters();
            Map<String, Object> payload = new HashMap<>();

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                Object value = args[i];

                Param paramAnnotation = param.getAnnotation(Param.class);
                if (paramAnnotation != null) {
                    String paramName = paramAnnotation.value();
                    if (value != null || paramAnnotation.required()) {
                        payload.put(paramName, value);
                    }
                } else {
                    // If no @Param and it's the only argument, use it directly
                    if (args.length == 1) {
                        return value;
                    }
                    // Otherwise, use parameter name (requires -parameters compiler flag)
                    payload.put(param.getName(), value);
                }
            }

            return payload;
        }

        /**
         * Convert the result to the expected return type.
         */
        private Object convertResult(Object result, Method method) {
            if (result == null) {
                return null;
            }

            Class<?> returnType = method.getReturnType();
            Type genericReturnType = method.getGenericReturnType();
            Type targetType = returnType;

            // Handle CompletableFuture unwrapping
            if (CompletableFuture.class.isAssignableFrom(returnType) && genericReturnType instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) genericReturnType).getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    returnType = (Class<?>) typeArgs[0];
                    targetType = typeArgs[0];
                }
            }

            // If result is already the right type, return it
            if (returnType.isInstance(result)) {
                return result;
            }

            try {
                TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();
                return OBJECT_MAPPER.convertValue(result, typeFactory.constructType(targetType));
            } catch (IllegalArgumentException e) {
                logger.debug("Failed to convert result {} to type {}", result.getClass().getName(), targetType, e);
            }

            return result;
        }
    }

}
