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

package com.childrengreens.netty.spring.boot.context.dispatch;

import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.codec.JsonNettyCodec;
import com.childrengreens.netty.spring.boot.context.codec.NettyCodec;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.message.InboundMessage;
import com.childrengreens.netty.spring.boot.context.message.OutboundMessage;
import com.childrengreens.netty.spring.boot.context.routing.RouteDefinition;
import com.childrengreens.netty.spring.boot.context.routing.Router;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Dispatcher for routing and invoking handler methods.
 *
 * <p>The dispatcher performs the following operations:
 * <ol>
 * <li>Route lookup using the {@link Router}</li>
 * <li>Parameter resolution for the handler method</li>
 * <li>Handler method invocation</li>
 * <li>Response encoding using the appropriate codec</li>
 * </ol>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see Router
 */
public class Dispatcher {

    private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private final Router router;
    private final CodecRegistry codecRegistry;
    private final ArgumentResolverComposite argumentResolvers;
    private final ObjectMapper objectMapper;

    /**
     * Create a new Dispatcher.
     * @param router the router for route matching
     * @param codecRegistry the codec registry for encoding/decoding
     */
    public Dispatcher(Router router, CodecRegistry codecRegistry) {
        this.router = router;
        this.codecRegistry = codecRegistry;
        this.argumentResolvers = new ArgumentResolverComposite();
        this.objectMapper = resolveObjectMapper(codecRegistry);
    }

    /**
     * Resolve ObjectMapper from CodecRegistry or create a new one.
     */
    private static ObjectMapper resolveObjectMapper(CodecRegistry codecRegistry) {
        NettyCodec codec = codecRegistry.getCodec(JsonNettyCodec.NAME);
        if (codec instanceof JsonNettyCodec) {
            return ((JsonNettyCodec) codec).getObjectMapper();
        }
        return new ObjectMapper();
    }

    /**
     * Dispatch an inbound message to the appropriate handler.
     * @param message the inbound message
     * @param context the Netty context
     * @return the outbound message, or {@code null} if no response
     */
    public CompletableFuture<OutboundMessage> dispatch(InboundMessage message, NettyContext context) {
        String serverName = context.getServerName();
        Router.RouteResult result = router.findRoute(message, serverName);

        if (result == null) {
            logger.warn("No handler found for route: {} (server={})",
                    message.getRouteKey(), serverName);
            return CompletableFuture.completedFuture(
                    OutboundMessage.error(404, "No handler found for: " + message.getRouteKey()));
        }

        RouteDefinition route = result.getRoute();
        Map<String, String> pathVariables = result.getPathVariables();

        try {
            Object[] args = resolveArguments(route.getMethod(), message, context, pathVariables);
            Object returnValue = invoke(route.getBean(), route.getMethod(), args);
            return handleReturnValue(returnValue);
        } catch (Exception e) {
            logger.error("Error dispatching to handler: {}", route.getMethod(), e);
            return CompletableFuture.completedFuture(
                    OutboundMessage.error(500, "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Resolve arguments for the handler method.
     */
    private Object[] resolveArguments(Method method, InboundMessage message,
                                       NettyContext context, Map<String, String> pathVariables) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            args[i] = resolveArgument(parameters[i], message, context, pathVariables);
        }

        return args;
    }

    /**
     * Resolve a single argument.
     */
    private Object resolveArgument(Parameter parameter, InboundMessage message,
                                    NettyContext context, Map<String, String> pathVariables) {
        Class<?> type = parameter.getType();

        // Handle common types
        if (NettyContext.class.isAssignableFrom(type)) {
            return context;
        }
        if (Channel.class.isAssignableFrom(type)) {
            return context.getChannel();
        }
        if (InboundMessage.class.isAssignableFrom(type)) {
            return message;
        }

        // Try argument resolvers
        Object resolved = argumentResolvers.resolveArgument(parameter, message, context, pathVariables);
        if (resolved != null) {
            return resolved;
        }

        // Default to payload conversion
        Object payload = message.getPayload();
        if (payload != null && type.isAssignableFrom(payload.getClass())) {
            return payload;
        }

        // Try codec conversion - extract "data" field from unified message format
        if (message.getRawPayload() != null) {
            Object decoded = decodeDataField(message.getRawPayload(), type);
            if (decoded != null) {
                return decoded;
            }
        } else if (message.getRawPayloadBuffer() != null) {
            ByteBuf buf = message.getRawPayloadBuffer();
            byte[] bytes = ByteBufUtil.getBytes(
                    buf, buf.readerIndex(), buf.readableBytes(), false);
            Object decoded = decodeDataField(bytes, type);
            if (decoded != null) {
                return decoded;
            }
        }

        return null;
    }

    /**
     * Decode the "data" field from a JSON message to the target type.
     * <p>Message format: {@code {"type": "...", "X-Correlation-Id": "...", "data": {...}}}
     *
     * @param bytes the raw JSON bytes
     * @param type the target type
     * @return the decoded object, or null if decoding fails
     */
    private Object decodeDataField(byte[] bytes, Class<?> type) {
        try {
            JsonNode rootNode = objectMapper.readTree(bytes);
            if (rootNode == null || !rootNode.isObject()) {
                return null;
            }

            JsonNode dataNode = rootNode.get("data");
            if (dataNode != null) {
                // Decode the "data" field to target type
                return objectMapper.treeToValue(dataNode, type);
            }

            // Fallback: try decoding the entire message (for backward compatibility)
            return objectMapper.treeToValue(rootNode, type);
        } catch (Exception e) {
            logger.debug("Failed to decode data field to {}: {}", type, e.getMessage());
            return null;
        }
    }

    /**
     * Invoke the handler method.
     */
    private Object invoke(Object bean, Method method, Object[] args)
            throws InvocationTargetException, IllegalAccessException {
        method.setAccessible(true);
        return method.invoke(bean, args);
    }

    /**
     * Handle the return value from the handler method.
     */
    private CompletableFuture<OutboundMessage> handleReturnValue(Object returnValue) {
        if (returnValue == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (returnValue instanceof CompletableFuture) {
            return ((CompletableFuture<?>) returnValue)
                    .thenApply(this::wrapReturnValue)
                    .exceptionally(this::handleAsyncException);
        }

        if (returnValue instanceof CompletionStage) {
            return ((CompletionStage<?>) returnValue).toCompletableFuture()
                    .thenApply(this::wrapReturnValue)
                    .exceptionally(this::handleAsyncException);
        }

        if (returnValue instanceof OutboundMessage) {
            return CompletableFuture.completedFuture((OutboundMessage) returnValue);
        }

        return CompletableFuture.completedFuture(wrapReturnValue(returnValue));
    }

    /**
     * Wrap a return value in an OutboundMessage.
     */
    private OutboundMessage wrapReturnValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OutboundMessage) {
            return (OutboundMessage) value;
        }
        return OutboundMessage.ok(value);
    }

    private OutboundMessage handleAsyncException(Throwable ex) {
        logger.error("Async handler error", ex);
        String message = ex != null ? ex.getMessage() : "unknown";
        return OutboundMessage.error(500, "Internal server error: " + message);
    }

    /**
     * Get the argument resolvers.
     * @return the argument resolver composite
     */
    public ArgumentResolverComposite getArgumentResolvers() {
        return argumentResolvers;
    }

}
