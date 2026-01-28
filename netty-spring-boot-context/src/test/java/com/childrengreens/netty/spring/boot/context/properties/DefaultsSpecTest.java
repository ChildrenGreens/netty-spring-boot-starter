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
 * Tests for {@link DefaultsSpec}.
 */
class DefaultsSpecTest {

    @Test
    void defaultValues() {
        DefaultsSpec spec = new DefaultsSpec();

        assertThat(spec.getThreads()).isNotNull();
        assertThat(spec.getTransport()).isNotNull();
        assertThat(spec.getShutdown()).isNotNull();
    }

    @Test
    void setThreads() {
        DefaultsSpec spec = new DefaultsSpec();
        ThreadsSpec threads = new ThreadsSpec();
        threads.setBoss(2);
        threads.setWorker(4);

        spec.setThreads(threads);

        assertThat(spec.getThreads().getBoss()).isEqualTo(2);
        assertThat(spec.getThreads().getWorker()).isEqualTo(4);
    }

    @Test
    void setTransport() {
        DefaultsSpec spec = new DefaultsSpec();
        TransportSpec transport = new TransportSpec();
        transport.setPrefer(TransportImpl.NIO);

        spec.setTransport(transport);

        assertThat(spec.getTransport().getPrefer()).isEqualTo(TransportImpl.NIO);
    }

    @Test
    void setShutdown() {
        DefaultsSpec spec = new DefaultsSpec();
        ShutdownSpec shutdown = new ShutdownSpec();
        shutdown.setGraceful(false);
        shutdown.setTimeoutMs(5000);

        spec.setShutdown(shutdown);

        assertThat(spec.getShutdown().isGraceful()).isFalse();
        assertThat(spec.getShutdown().getTimeoutMs()).isEqualTo(5000);
    }

}
