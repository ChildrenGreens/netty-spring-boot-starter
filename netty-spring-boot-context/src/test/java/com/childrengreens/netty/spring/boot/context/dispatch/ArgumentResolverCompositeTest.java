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
 * Tests for {@link ArgumentResolverComposite}.
 */
class ArgumentResolverCompositeTest {

    private ArgumentResolverComposite composite;
    private InboundMessage message;
    private NettyContext context;

    @BeforeEach
    void setUp() {
        composite = new ArgumentResolverComposite();
        message = mock(InboundMessage.class);
        context = mock(NettyContext.class);
    }

    @Test
    void addResolver_addsToList() {
        PathVariableArgumentResolver resolver = new PathVariableArgumentResolver();
        composite.addResolver(resolver);
        assertThat(composite).isNotNull();
    }

    @Test
    void resolveArgument_withMatchingResolver_returnsResolvedValue() throws Exception {
        composite.addResolver(new PathVariableArgumentResolver());

        Parameter param = TestController.class.getMethod("withPathVar", String.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("id", "resolved-value");

        Object result = composite.resolveArgument(param, message, context, pathVars);

        assertThat(result).isEqualTo("resolved-value");
    }

    @Test
    void resolveArgument_withNoMatchingResolver_returnsNull() throws Exception {
        Parameter param = TestController.class.getMethod("withPathVar", String.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("id", "value");

        Object result = composite.resolveArgument(param, message, context, pathVars);

        assertThat(result).isNull();
    }

    @Test
    void resolveArgument_withMultipleResolvers_usesFirstMatch() throws Exception {
        composite.addResolver(new PathVariableArgumentResolver());
        composite.addResolver(new QueryArgumentResolver());

        Parameter param = TestController.class.getMethod("withPathVar", String.class)
                .getParameters()[0];
        Map<String, String> pathVars = new HashMap<>();
        pathVars.put("id", "path-value");

        Object result = composite.resolveArgument(param, message, context, pathVars);

        assertThat(result).isEqualTo("path-value");
    }

    /**
     * Test controller for parameter extraction.
     */
    public static class TestController {
        public void withPathVar(@PathVar("id") String id) {}
    }
}
