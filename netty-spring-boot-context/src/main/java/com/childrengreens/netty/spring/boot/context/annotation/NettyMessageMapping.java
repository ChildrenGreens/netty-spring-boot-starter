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
 * Annotation for mapping TCP/UDP messages onto handler methods by message type.
 *
 * <p>Handler methods which are annotated with this annotation are allowed
 * to have very flexible signatures. They may have parameters of the following types:
 * <ul>
 * <li>The decoded message object (POJO)</li>
 * <li>{@link com.childrengreens.netty.spring.boot.context.context.NettyContext NettyContext}
 * - provides access to channel attributes and remote address</li>
 * <li>{@link io.netty.channel.Channel Channel} - the Netty channel</li>
 * </ul>
 *
 * <p>The method may return:
 * <ul>
 * <li>A response object that will be encoded and sent back</li>
 * <li>{@code void} if no response is needed</li>
 * <li>{@link java.util.concurrent.CompletableFuture CompletableFuture} for async processing</li>
 * </ul>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see NettyMessageController
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NettyMessageMapping {

    /**
     * The message type to handle.
     * <p>This value is matched against the type field in the incoming message.
     * @return the message type
     */
    String value() default "";

    /**
     * Alias for {@link #value()}.
     * @return the message type
     */
    String type() default "";

}
