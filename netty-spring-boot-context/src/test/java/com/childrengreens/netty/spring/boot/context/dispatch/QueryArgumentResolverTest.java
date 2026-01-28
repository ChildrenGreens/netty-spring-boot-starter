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

import com.childrengreens.netty.spring.boot.context.annotation.Query;
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
import static org.mockito.Mockito.when;

/**
 * Tests for {@link QueryArgumentResolver}.
 */
class QueryArgumentResolverTest {

    private QueryArgumentResolver resolver;
    private InboundMessage message;
    private NettyContext context;

    @BeforeEach
    void setUp() {
        resolver = new QueryArgumentResolver();
        message = mock(InboundMessage.class);
        context = mock(NettyContext.class);
    }

    @Test
    void supports_withQueryAnnotation_returnsTrue() throws Exception {
        Parameter param = TestController.class.getMethod("withQuery", String.class)
                .getParameters()[0];
        assertThat(resolver.supports(param)).isTrue();
    }

    @Test
    void supports_withoutQueryAnnotation_returnsFalse() throws Exception {
        Parameter param = TestController.class.getMethod("withoutQuery", String.class)
                .getParameters()[0];
        assertThat(resolver.supports(param)).isFalse();
    }

    @Test
    void resolve_withMatchingQueryParam_returnsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withQuery", String.class)
                .getParameters()[0];

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("name", "testValue");
        when(message.getHeader("queryParams")).thenReturn(queryParams);

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isEqualTo("testValue");
    }

    @Test
    void resolve_withMissingQueryParam_returnsNull() throws Exception {
        Parameter param = TestController.class.getMethod("withOptionalQuery", String.class)
                .getParameters()[0];
        when(message.getHeader("queryParams")).thenReturn(new HashMap<>());

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isNull();
    }

    @Test
    void resolve_withIntegerType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withIntQuery", Integer.class)
                .getParameters()[0];

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("page", "5");
        when(message.getHeader("queryParams")).thenReturn(queryParams);

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isEqualTo(5);
    }

    @Test
    void resolve_withLongType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withLongQuery", Long.class)
                .getParameters()[0];

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("id", "12345678901");
        when(message.getHeader("queryParams")).thenReturn(queryParams);

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isEqualTo(12345678901L);
    }

    @Test
    void resolve_withPrimitiveLongType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withPrimitiveLongQuery", long.class)
                .getParameters()[0];

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("id", "999");
        when(message.getHeader("queryParams")).thenReturn(queryParams);

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isEqualTo(999L);
    }

    @Test
    void resolve_withBooleanType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withBooleanQuery", Boolean.class)
                .getParameters()[0];

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("active", "true");
        when(message.getHeader("queryParams")).thenReturn(queryParams);

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isEqualTo(true);
    }

    @Test
    void resolve_withPrimitiveBooleanType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withPrimitiveBooleanQuery", boolean.class)
                .getParameters()[0];

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("active", "false");
        when(message.getHeader("queryParams")).thenReturn(queryParams);

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isEqualTo(false);
    }

    @Test
    void resolve_withDoubleType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withDoubleQuery", Double.class)
                .getParameters()[0];

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("price", "19.99");
        when(message.getHeader("queryParams")).thenReturn(queryParams);

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isEqualTo(19.99);
    }

    @Test
    void resolve_withPrimitiveDoubleType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withPrimitiveDoubleQuery", double.class)
                .getParameters()[0];

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("price", "29.99");
        when(message.getHeader("queryParams")).thenReturn(queryParams);

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isEqualTo(29.99);
    }

    @Test
    void resolve_withPrimitiveIntType_convertsValue() throws Exception {
        Parameter param = TestController.class.getMethod("withPrimitiveIntQuery", int.class)
                .getParameters()[0];

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("count", "42");
        when(message.getHeader("queryParams")).thenReturn(queryParams);

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isEqualTo(42);
    }

    @Test
    void resolve_withDefaultValue_usesDefault() throws Exception {
        Parameter param = TestController.class.getMethod("withDefaultValue", String.class)
                .getParameters()[0];
        when(message.getHeader("queryParams")).thenReturn(new HashMap<>());

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isEqualTo("default");
    }

    @Test
    void resolve_withNullQueryParams_returnsNull() throws Exception {
        Parameter param = TestController.class.getMethod("withOptionalQuery", String.class)
                .getParameters()[0];
        when(message.getHeader("queryParams")).thenReturn(null);

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isNull();
    }

    @Test
    void resolve_withMissingRequiredQuery_throwsException() throws Exception {
        Parameter param = TestController.class.getMethod("withRequiredQuery", String.class)
                .getParameters()[0];
        when(message.getHeader("queryParams")).thenReturn(new HashMap<>());

        assertThatThrownBy(() -> resolver.resolve(param, message, context, new HashMap<>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required query parameter");
    }

    @Test
    void resolve_withNameAttribute_usesName() throws Exception {
        Parameter param = TestController.class.getMethod("withNamedQuery", String.class)
                .getParameters()[0];

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("userName", "john");
        when(message.getHeader("queryParams")).thenReturn(queryParams);

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isEqualTo("john");
    }

    @Test
    void resolve_withUnknownType_returnsStringValue() throws Exception {
        Parameter param = TestController.class.getMethod("withObjectQuery", Object.class)
                .getParameters()[0];

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("obj", "some-object");
        when(message.getHeader("queryParams")).thenReturn(queryParams);

        Object result = resolver.resolve(param, message, context, new HashMap<>());

        assertThat(result).isEqualTo("some-object");
    }

    /**
     * Test controller for parameter extraction.
     */
    public static class TestController {
        public void withQuery(@Query(value = "name", required = false) String name) {}
        public void withOptionalQuery(@Query(value = "opt", required = false) String opt) {}
        public void withoutQuery(String name) {}
        public void withIntQuery(@Query(value = "page", required = false) Integer page) {}
        public void withLongQuery(@Query(value = "id", required = false) Long id) {}
        public void withPrimitiveLongQuery(@Query(value = "id", required = false) long id) {}
        public void withBooleanQuery(@Query(value = "active", required = false) Boolean active) {}
        public void withPrimitiveBooleanQuery(@Query(value = "active", required = false) boolean active) {}
        public void withDoubleQuery(@Query(value = "price", required = false) Double price) {}
        public void withPrimitiveDoubleQuery(@Query(value = "price", required = false) double price) {}
        public void withPrimitiveIntQuery(@Query(value = "count", required = false) int count) {}
        public void withDefaultValue(@Query(value = "def", required = false, defaultValue = "default") String def) {}
        public void withRequiredQuery(@Query(value = "required", required = true) String required) {}
        public void withNamedQuery(@Query(name = "userName", required = false) String user) {}
        public void withObjectQuery(@Query(value = "obj", required = false) Object obj) {}
    }
}
