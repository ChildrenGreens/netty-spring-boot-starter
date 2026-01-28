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
 * Tests for {@link SslSpec}.
 */
class SslSpecTest {

    @Test
    void defaultValues() {
        SslSpec spec = new SslSpec();

        assertThat(spec.isEnabled()).isFalse();
        assertThat(spec.getCertPath()).isNull();
        assertThat(spec.getKeyPath()).isNull();
        assertThat(spec.getKeyPassword()).isNull();
        assertThat(spec.getTrustCertPath()).isNull();
        assertThat(spec.isClientAuth()).isFalse();
    }

    @Test
    void setEnabled() {
        SslSpec spec = new SslSpec();

        spec.setEnabled(true);

        assertThat(spec.isEnabled()).isTrue();
    }

    @Test
    void setCertPath() {
        SslSpec spec = new SslSpec();

        spec.setCertPath("/path/to/cert.pem");

        assertThat(spec.getCertPath()).isEqualTo("/path/to/cert.pem");
    }

    @Test
    void setKeyPath() {
        SslSpec spec = new SslSpec();

        spec.setKeyPath("/path/to/key.pem");

        assertThat(spec.getKeyPath()).isEqualTo("/path/to/key.pem");
    }

    @Test
    void setKeyPassword() {
        SslSpec spec = new SslSpec();

        spec.setKeyPassword("secret");

        assertThat(spec.getKeyPassword()).isEqualTo("secret");
    }

    @Test
    void setTrustCertPath() {
        SslSpec spec = new SslSpec();

        spec.setTrustCertPath("/path/to/trust.pem");

        assertThat(spec.getTrustCertPath()).isEqualTo("/path/to/trust.pem");
    }

    @Test
    void setClientAuth() {
        SslSpec spec = new SslSpec();

        spec.setClientAuth(true);

        assertThat(spec.isClientAuth()).isTrue();
    }

}
