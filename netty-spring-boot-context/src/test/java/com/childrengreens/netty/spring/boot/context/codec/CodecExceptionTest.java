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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CodecException}.
 */
class CodecExceptionTest {

    @Test
    void constructor_withMessage_setsMessage() {
        CodecException exception = new CodecException("test message");
        assertThat(exception.getMessage()).isEqualTo("test message");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_withMessageAndCause_setsMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        CodecException exception = new CodecException("test message", cause);
        assertThat(exception.getMessage()).isEqualTo("test message");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void isRuntimeException() {
        CodecException exception = new CodecException("test");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
