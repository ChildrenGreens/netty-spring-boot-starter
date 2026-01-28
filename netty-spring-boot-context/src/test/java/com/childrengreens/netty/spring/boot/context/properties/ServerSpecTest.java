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
 * Tests for {@link ServerSpec}.
 */
class ServerSpecTest {

    @Test
    void defaultValues() {
        ServerSpec spec = new ServerSpec();

        assertThat(spec.getName()).isNull();
        assertThat(spec.getTransport()).isEqualTo(TransportType.TCP);
        assertThat(spec.getHost()).isEqualTo("0.0.0.0");
        assertThat(spec.getPort()).isEqualTo(0);
        assertThat(spec.getProfile()).isNull();
        assertThat(spec.getRouting()).isNotNull();
        assertThat(spec.getFeatures()).isNotNull();
        assertThat(spec.getThreads()).isNull();
    }

    @Test
    void setName() {
        ServerSpec spec = new ServerSpec();

        spec.setName("my-server");

        assertThat(spec.getName()).isEqualTo("my-server");
    }

    @Test
    void setTransport() {
        ServerSpec spec = new ServerSpec();

        spec.setTransport(TransportType.HTTP);

        assertThat(spec.getTransport()).isEqualTo(TransportType.HTTP);
    }

    @Test
    void setTransport_udp() {
        ServerSpec spec = new ServerSpec();

        spec.setTransport(TransportType.UDP);

        assertThat(spec.getTransport()).isEqualTo(TransportType.UDP);
    }

    @Test
    void setHost() {
        ServerSpec spec = new ServerSpec();

        spec.setHost("127.0.0.1");

        assertThat(spec.getHost()).isEqualTo("127.0.0.1");
    }

    @Test
    void setPort() {
        ServerSpec spec = new ServerSpec();

        spec.setPort(8080);

        assertThat(spec.getPort()).isEqualTo(8080);
    }

    @Test
    void setProfile() {
        ServerSpec spec = new ServerSpec();

        spec.setProfile("tcp-lengthfield-json");

        assertThat(spec.getProfile()).isEqualTo("tcp-lengthfield-json");
    }

    @Test
    void setRouting() {
        ServerSpec spec = new ServerSpec();
        RoutingSpec routing = new RoutingSpec();
        routing.setMode(RoutingMode.PATH);

        spec.setRouting(routing);

        assertThat(spec.getRouting().getMode()).isEqualTo(RoutingMode.PATH);
    }

    @Test
    void setFeatures() {
        ServerSpec spec = new ServerSpec();
        FeaturesSpec features = new FeaturesSpec();
        IdleSpec idle = new IdleSpec();
        idle.setEnabled(true);
        features.setIdle(idle);

        spec.setFeatures(features);

        assertThat(spec.getFeatures().getIdle()).isNotNull();
        assertThat(spec.getFeatures().getIdle().isEnabled()).isTrue();
    }

    @Test
    void setThreads() {
        ServerSpec spec = new ServerSpec();
        ThreadsSpec threads = new ThreadsSpec();
        threads.setBoss(2);
        threads.setWorker(8);

        spec.setThreads(threads);

        assertThat(spec.getThreads()).isNotNull();
        assertThat(spec.getThreads().getBoss()).isEqualTo(2);
        assertThat(spec.getThreads().getWorker()).isEqualTo(8);
    }

}
