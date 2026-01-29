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
 * Annotation for mapping HTTP DELETE requests onto specific handler methods.
 *
 * <p>Specifically, {@code @NettyHttpDelete} is a composed annotation that
 * acts as a shortcut for {@code @NettyHttpMapping(method = "DELETE")}.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see NettyController
 * @see NettyHttpGet
 * @see NettyHttpPost
 * @see NettyHttpMapping
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@NettyHttpMapping(method = "DELETE")
public @interface NettyHttpDelete {

    /**
     * The path mapping URIs (e.g. {@code "/users/{id}"}).
     * @return the path patterns
     */
    String[] value() default {};

    /**
     * The path mapping URIs (e.g. {@code "/users/{id}"}).
     * <p>Alias for {@link #value()}.
     * @return the path patterns
     */
    String[] path() default {};

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
