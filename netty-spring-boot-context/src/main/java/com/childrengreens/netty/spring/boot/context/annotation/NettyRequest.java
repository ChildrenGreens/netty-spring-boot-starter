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
 * Marks a method as a Netty request.
 *
 * <p>Methods annotated with this annotation within a {@link NettyClient}
 * interface will be translated into network requests.
 *
 * <p>Example:
 * <pre>{@code
 * @NettyRequest(type = "order", timeout = 5000)
 * CompletableFuture<OrderResponse> createOrder(OrderRequest request);
 * }</pre>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see NettyClient
 * @see Param
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NettyRequest {

    /**
     * The message type for this request.
     * <p>This will be used as the route key when sending the request.
     * @return the message type
     */
    String type();

    /**
     * Alias for {@link #type()}.
     * @return the message type
     */
    String value() default "";

    /**
     * Request timeout in milliseconds.
     * <p>If not specified, uses the client's default timeout.
     * @return the timeout in milliseconds, 0 for default
     */
    long timeout() default 0;

    /**
     * Whether this is a one-way request (no response expected).
     * @return {@code true} if one-way
     */
    boolean oneWay() default false;

}
