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

    /**
     * Test controller for parameter extraction.
     */
    public static class TestController {
        public void withQuery(@Query(value = "name", required = false) String name) {}
        public void withOptionalQuery(@Query(value = "opt", required = false) String opt) {}
        public void withoutQuery(String name) {}
        public void withIntQuery(@Query(value = "page", required = false) Integer page) {}
    }
}
