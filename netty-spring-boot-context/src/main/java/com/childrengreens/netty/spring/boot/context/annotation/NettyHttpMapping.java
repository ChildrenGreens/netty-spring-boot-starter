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
 * Annotation for mapping HTTP requests onto handler methods.
 *
 * <p>This annotation can be used both at the type level and at the method level.
 * In most cases, at the method level applications will prefer to use one of
 * the HTTP method specific variants {@link NettyHttpGet @NettyHttpGet},
 * {@link NettyHttpPost @NettyHttpPost}, etc.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see NettyHttpGet
 * @see NettyHttpPost
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NettyHttpMapping {

    /**
     * The path mapping URIs (e.g. {@code "/profile"}).
     * @return the path patterns
     */
    String[] value() default {};

    /**
     * The path mapping URIs (e.g. {@code "/profile"}).
     * <p>Alias for {@link #value()}.
     * @return the path patterns
     */
    String[] path() default {};

    /**
     * The HTTP request methods to map to.
     * @return the HTTP methods
     */
    String[] method() default {};

    /**
     * The consumable media types of the mapped request.
     * @return the consumable media types
     */
    String[] consumes() default {};

    /**
     * The producible media types of the mapped request.
     * @return the producible media types
     */
    String[] produces() default {};

}
