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
 * Annotation for handling WebSocket connection close events.
 *
 * <p>Methods annotated with this annotation are invoked when a WebSocket
 * connection is closed.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 * @see NettyController
 * @see NettyWsOnOpen
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NettyWsOnClose {

    /**
     * The WebSocket path to map to.
     * @return the WebSocket path
     */
    String value() default "";

    /**
     * The WebSocket path to map to.
     * <p>Alias for {@link #value()}.
     * @return the WebSocket path
     */
    String path() default "";

}
