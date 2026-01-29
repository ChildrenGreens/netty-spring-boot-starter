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

import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * Argument resolver for path variables annotated with {@link PathVar}.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class PathVariableArgumentResolver implements ArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(PathVar.class);
    }

    @Override
    public Object resolve(Parameter parameter, InboundMessage message,
                          NettyContext context, Map<String, String> pathVariables) {
        PathVar annotation = parameter.getAnnotation(PathVar.class);
        String name = annotation.value();
        if (name.isEmpty()) {
            name = annotation.name();
        }
        if (name.isEmpty()) {
            name = parameter.getName();
        }

        String value = pathVariables.get(name);
        if (value == null && annotation.required()) {
            throw new IllegalArgumentException("Missing required path variable: " + name);
        }

        return convertValue(value, parameter.getType());
    }

    /**
     * Convert the string value to the target type.
     */
    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType == String.class) {
            return value;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value);
        }
        return value;
    }

}
