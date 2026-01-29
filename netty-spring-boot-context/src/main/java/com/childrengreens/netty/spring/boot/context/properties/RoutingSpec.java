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

package com.childrengreens.netty.spring.boot.context.properties;

/**
 * Routing configuration for message dispatching.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see RoutingMode
 */
public class RoutingSpec {

    /**
     * The routing mode for message matching.
     */
    private RoutingMode mode = RoutingMode.MESSAGE_TYPE;

    /**
     * The field name in message body used for routing when mode is MESSAGE_TYPE.
     */
    private String typeField = "type";

    /**
     * Return the routing mode.
     * @return the routing mode
     */
    public RoutingMode getMode() {
        return this.mode;
    }

    /**
     * Set the routing mode.
     * @param mode the routing mode
     */
    public void setMode(RoutingMode mode) {
        this.mode = mode;
    }

    /**
     * Return the type field name for MESSAGE_TYPE routing.
     * @return the type field name
     */
    public String getTypeField() {
        return this.typeField;
    }

    /**
     * Set the type field name for MESSAGE_TYPE routing.
     * @param typeField the type field name
     */
    public void setTypeField(String typeField) {
        this.typeField = typeField;
    }

}
