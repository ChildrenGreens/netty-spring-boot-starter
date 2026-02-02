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

package com.childrengreens.netty.spring.boot.context.auth;

/**
 * Token type enumeration for HTTP authentication.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public enum TokenType {

    /**
     * JSON Web Token (JWT).
     *
     * <p>A compact, URL-safe means of representing claims to be
     * transferred between two parties.
     */
    JWT,

    /**
     * API Key authentication.
     *
     * <p>A simple string token used to identify the calling application.
     */
    API_KEY

}
