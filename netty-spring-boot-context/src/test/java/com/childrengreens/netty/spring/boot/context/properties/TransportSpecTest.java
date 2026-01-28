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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TransportSpec}.
 */
class TransportSpecTest {

    @Test
    void defaultValues() {
        TransportSpec spec = new TransportSpec();

        assertThat(spec.getPrefer()).isEqualTo(TransportImpl.AUTO);
    }

    @Test
    void setPrefer() {
        TransportSpec spec = new TransportSpec();

        spec.setPrefer(TransportImpl.NIO);

        assertThat(spec.getPrefer()).isEqualTo(TransportImpl.NIO);
    }

    @Test
    void setPrefer_epoll() {
        TransportSpec spec = new TransportSpec();

        spec.setPrefer(TransportImpl.EPOLL);

        assertThat(spec.getPrefer()).isEqualTo(TransportImpl.EPOLL);
    }

    @Test
    void setPrefer_kqueue() {
        TransportSpec spec = new TransportSpec();

        spec.setPrefer(TransportImpl.KQUEUE);

        assertThat(spec.getPrefer()).isEqualTo(TransportImpl.KQUEUE);
    }

}
