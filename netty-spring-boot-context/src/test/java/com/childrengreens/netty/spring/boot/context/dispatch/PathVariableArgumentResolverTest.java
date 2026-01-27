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

package com.childrengreens.netty.spring.boot.context.dispatch;

import com.childrengreens.netty.spring.boot.context.annotation.PathVar;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.message.InboundMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PathVariableArgumentResolver}.
 */
class PathVariableArgumentResolverTest {

    private PathVariableArgumentResolver resolver;
    private InboundMessage message;
    private NettyContext context;

    @BeforeEach
    void setUp() {
        resolver = new PathVariableArgumentResolver();
        message = mock(InboundMessage.class);
        context = mock(NettyContext.class);
    }

    @Test
    void supports_withPathVarAnnotation_returnsTrue() throws Exception {
        Parameter param = TestController.class.getMethod("withPathVar", String.class)
                .getParameters()[0];
        assertThat(resolver.supports(param)).isTrue();
    }

    @Test
    void supports_withoutPathVarAnnotation_returnsFalse() throws Exception {
        Parameter param = TestController.class.getMethod("withoutPathVar", String.class)
                .getParameters()[0];
        assertThat(resolver.supports(param)).isFalse();
    }

    @Test
    void resolve_withMatchingPathVariable_returnsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withPathVar", String.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("id", "123");

        Object result = resolver.resolve(param, message, context, pathVars);

        assertThat(result).isEqualTo("123");
    }

    @Test
    void resolve_withIntegerType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withIntPathVar", int.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("num", "42");

        Object result = resolver.resolve(param, message, context, pathVars);

        assertThat(result).isEqualTo(42);
    }

    @Test
    void resolve_withLongType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withLongPathVar", Long.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("num", "12345678901");

        Object result = resolver.resolve(param, message, context, pathVars);

        assertThat(result).isEqualTo(12345678901L);
    }

    /**
     * Test controller for parameter extraction.
     */
    public static class TestController {
        public void withPathVar(@PathVar("id") String id) {}
        public void withoutPathVar(String id) {}
        public void withIntPathVar(@PathVar("num") int num) {}
        public void withLongPathVar(@PathVar("num") Long num) {}
    }
}
