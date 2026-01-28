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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void resolve_withBooleanType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withBooleanPathVar", boolean.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("flag", "true");

        Object result = resolver.resolve(param, message, context, pathVars);

        assertThat(result).isEqualTo(true);
    }

    @Test
    void resolve_withDoubleType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withDoublePathVar", Double.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("value", "3.14");

        Object result = resolver.resolve(param, message, context, pathVars);

        assertThat(result).isEqualTo(3.14);
    }

    @Test
    void resolve_withPrimitiveDoubleType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withPrimitiveDoublePathVar", double.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("value", "2.71");

        Object result = resolver.resolve(param, message, context, pathVars);

        assertThat(result).isEqualTo(2.71);
    }

    @Test
    void resolve_withIntegerWrapperType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withIntegerWrapperPathVar", Integer.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("num", "100");

        Object result = resolver.resolve(param, message, context, pathVars);

        assertThat(result).isEqualTo(100);
    }

    @Test
    void resolve_withPrimitiveLongType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withPrimitiveLongPathVar", long.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("num", "999");

        Object result = resolver.resolve(param, message, context, pathVars);

        assertThat(result).isEqualTo(999L);
    }

    @Test
    void resolve_withBooleanWrapperType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withBooleanWrapperPathVar", Boolean.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("flag", "false");

        Object result = resolver.resolve(param, message, context, pathVars);

        assertThat(result).isEqualTo(false);
    }

    @Test
    void resolve_withMissingRequiredPathVariable_throwsException() throws Exception {
        Parameter param = TestController.class.getMethod("withRequiredPathVar", String.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();

        assertThatThrownBy(() -> resolver.resolve(param, message, context, pathVars))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required path variable");
    }

    @Test
    void resolve_withMissingOptionalPathVariable_returnsNull() throws Exception {
        Parameter param = TestController.class.getMethod("withOptionalPathVar", String.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();

        Object result = resolver.resolve(param, message, context, pathVars);

        assertThat(result).isNull();
    }

    @Test
    void resolve_withNameAttribute_usesName() throws Exception {
        Parameter param = TestController.class.getMethod("withNamedPathVar", String.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("userId", "user-123");

        Object result = resolver.resolve(param, message, context, pathVars);

        assertThat(result).isEqualTo("user-123");
    }

    @Test
    void resolve_withUnknownType_returnsStringValue() throws Exception {
        Parameter param = TestController.class.getMethod("withObjectPathVar", Object.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("obj", "some-value");

        Object result = resolver.resolve(param, message, context, pathVars);

        assertThat(result).isEqualTo("some-value");
    }

    /**
     * Test controller for parameter extraction.
     */
    public static class TestController {
        public void withPathVar(@PathVar("id") String id) {}
        public void withoutPathVar(String id) {}
        public void withIntPathVar(@PathVar("num") int num) {}
        public void withLongPathVar(@PathVar("num") Long num) {}
        public void withBooleanPathVar(@PathVar("flag") boolean flag) {}
        public void withDoublePathVar(@PathVar("value") Double value) {}
        public void withPrimitiveDoublePathVar(@PathVar("value") double value) {}
        public void withIntegerWrapperPathVar(@PathVar("num") Integer num) {}
        public void withPrimitiveLongPathVar(@PathVar("num") long num) {}
        public void withBooleanWrapperPathVar(@PathVar("flag") Boolean flag) {}
        public void withRequiredPathVar(@PathVar(value = "required", required = true) String required) {}
        public void withOptionalPathVar(@PathVar(value = "optional", required = false) String optional) {}
        public void withNamedPathVar(@PathVar(name = "userId") String id) {}
        public void withObjectPathVar(@PathVar("obj") Object obj) {}
    }
}
