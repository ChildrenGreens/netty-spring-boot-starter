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
 * Binds a method parameter to a request parameter.
 *
 * <p>Parameters annotated with this annotation will be included
 * in the request payload with the specified name.
 *
 * <p>Example:
 * <pre>{@code
 * @NettyRequest(type = "user.get")
 * User getUser(@Param("userId") String userId);
 * }</pre>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see NettyRequest
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    /**
     * The name of the parameter.
     * @return the parameter name
     */
    String value();

    /**
     * Whether the parameter is required.
     * @return {@code true} if required, defaults to {@code true}
     */
    boolean required() default true;

    /**
     * Default value if the parameter is not provided.
     * @return the default value
     */
    String defaultValue() default "";

}
