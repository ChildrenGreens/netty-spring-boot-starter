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
 * Transport implementation preference configuration.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see TransportImpl
 */
public class TransportSpec {

    /**
     * Preferred transport implementation.
     * <p>Will automatically fallback to NIO if the preferred implementation
     * is not available on the current platform.
     */
    private TransportImpl prefer = TransportImpl.AUTO;

    /**
     * Return the preferred transport implementation.
     * @return the preferred implementation
     */
    public TransportImpl getPrefer() {
        return this.prefer;
    }

    /**
     * Set the preferred transport implementation.
     * @param prefer the preferred implementation
     */
    public void setPrefer(TransportImpl prefer) {
        this.prefer = prefer;
    }

}
