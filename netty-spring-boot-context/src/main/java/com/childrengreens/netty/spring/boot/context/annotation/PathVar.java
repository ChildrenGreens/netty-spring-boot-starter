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
 * Annotation that binds a method parameter to a path variable.
 *
 * <p>For example, a path pattern like {@code "/user/{id}"} can be mapped
 * with a method parameter annotated with {@code @PathVar("id")}.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see NettyHttpGet
 * @see NettyHttpPost
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathVar {

    /**
     * The name of the path variable to bind to.
     * @return the path variable name
     */
    String value() default "";

    /**
     * The name of the path variable to bind to.
     * <p>Alias for {@link #value()}.
     * @return the path variable name
     */
    String name() default "";

    /**
     * Whether the path variable is required.
     * @return whether required
     */
    boolean required() default true;

}
