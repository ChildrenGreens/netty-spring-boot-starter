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

package com.childrengreens.netty.spring.boot.context.codec;

/**
 * Exception thrown when codec operations fail.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 */
public class CodecException extends RuntimeException {

    /**
     * Create a new CodecException with the specified message.
     * @param message the detail message
     */
    public CodecException(String message) {
        super(message);
    }

    /**
     * Create a new CodecException with the specified message and cause.
     * @param message the detail message
     * @param cause the cause
     */
    public CodecException(String message, Throwable cause) {
        super(message, cause);
    }

}
