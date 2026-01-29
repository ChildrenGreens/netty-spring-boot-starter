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

package com.childrengreens.netty.spring.boot.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a Netty client.
 *
 * <p>Interfaces annotated with this annotation will be automatically proxied
 * and can be injected into Spring beans. Methods on the interface will be
 * translated into network requests to the configured server.
 *
 * <p>Example:
 * <pre>{@code
 * @NettyClient(name = "order-service")
 * public interface OrderClient {
 *
 *     @NettyRequest(type = "ping")
 *     PongResponse ping();
 *
 *     @NettyRequest(type = "order", timeout = 5000)
 *     CompletableFuture<OrderResponse> createOrder(OrderRequest request);
 * }
 * }</pre>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see NettyRequest
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NettyClient {

    /**
     * The name of the client configuration to use.
     * <p>This should match a client name defined in application configuration.
     * @return the client name
     */
    String name();

    /**
     * Alias for {@link #name()}.
     * @return the client name
     */
    String value() default "";

    /**
     * The fallback class to use when the request fails.
     * @return the fallback class
     */
    Class<?> fallback() default void.class;

}
