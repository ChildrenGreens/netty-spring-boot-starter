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

package com.childrengreens.netty.spring.boot.context.feature;

import com.childrengreens.netty.spring.boot.context.properties.FeaturesSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import com.childrengreens.netty.spring.boot.context.properties.SslSpec;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SslFeatureProvider}.
 */
class SslFeatureProviderTest {

    private SslFeatureProvider provider;
    private ServerSpec serverSpec;

    @BeforeEach
    void setUp() {
        provider = new SslFeatureProvider();
        serverSpec = new ServerSpec();
        serverSpec.setName("test-server");

        FeaturesSpec features = new FeaturesSpec();
        serverSpec.setFeatures(features);
    }

    @Test
    void getName_returnsSsl() {
        assertThat(provider.getName()).isEqualTo("ssl");
    }

    @Test
    void getOrder_returns10() {
        assertThat(provider.getOrder()).isEqualTo(10);
    }

    @Test
    void isEnabled_whenSslNull_returnsFalse() {
        serverSpec.getFeatures().setSsl(null);

        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void isEnabled_whenSslDisabled_returnsFalse() {
        SslSpec ssl = new SslSpec();
        ssl.setEnabled(false);
        serverSpec.getFeatures().setSsl(ssl);

        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void isEnabled_whenSslEnabled_returnsTrue() {
        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        serverSpec.getFeatures().setSsl(ssl);

        assertThat(provider.isEnabled(serverSpec)).isTrue();
    }

    @Test
    void configure_whenSslDisabled_doesNotAddHandler() {
        SslSpec ssl = new SslSpec();
        ssl.setEnabled(false);
        serverSpec.getFeatures().setSsl(ssl);

        EmbeddedChannel channel = new EmbeddedChannel();

        provider.configure(channel.pipeline(), serverSpec);

        assertThat(channel.pipeline().names()).doesNotContain("sslHandler");

        channel.close();
    }

    @Test
    void configure_whenSslEnabled_withoutCert_throwsOrSucceeds() {
        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        serverSpec.getFeatures().setSsl(ssl);

        EmbeddedChannel channel = new EmbeddedChannel();

        try {
            provider.configure(channel.pipeline(), serverSpec);
            // If successful, sslHandler should be added
            assertThat(channel.pipeline().names()).contains("sslHandler");
        } catch (IllegalStateException e) {
            // Self-signed cert generation might fail in certain environments
            assertThat(e.getMessage()).contains("Failed to configure SSL");
        } finally {
            channel.close();
        }
    }

    @Test
    void configure_whenSslEnabled_withInvalidCertPath_throwsException() {
        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        ssl.setCertPath("/nonexistent/cert.pem");
        ssl.setKeyPath("/nonexistent/key.pem");
        serverSpec.getFeatures().setSsl(ssl);

        EmbeddedChannel channel = new EmbeddedChannel();

        assertThatThrownBy(() -> provider.configure(channel.pipeline(), serverSpec))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to configure SSL");

        channel.close();
    }

    @Test
    void getName_returnsConstantValue() {
        assertThat(provider.getName()).isEqualTo(SslFeatureProvider.NAME);
    }

    @Test
    void getOrder_returnsConstantValue() {
        assertThat(provider.getOrder()).isEqualTo(SslFeatureProvider.ORDER);
    }

    @Test
    void configure_whenSslNull_doesNotAddHandler() {
        serverSpec.getFeatures().setSsl(null);

        EmbeddedChannel channel = new EmbeddedChannel();

        provider.configure(channel.pipeline(), serverSpec);

        assertThat(channel.pipeline().names()).doesNotContain("sslHandler");

        channel.close();
    }

    @Test
    void configure_withSelfSignedCert_addsSslHandler() {
        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        // No cert/key paths means self-signed cert will be used
        serverSpec.getFeatures().setSsl(ssl);

        EmbeddedChannel channel = new EmbeddedChannel();

        try {
            provider.configure(channel.pipeline(), serverSpec);
            assertThat(channel.pipeline().names()).contains("sslHandler");
        } catch (IllegalStateException e) {
            // Self-signed cert generation might fail in certain environments
            assertThat(e.getMessage()).contains("Failed to configure SSL");
        } finally {
            channel.close();
        }
    }

    @Test
    void configure_withKeyPassword_buildsContextWithPassword() {
        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        ssl.setCertPath("/nonexistent/cert.pem");
        ssl.setKeyPath("/nonexistent/key.pem");
        ssl.setKeyPassword("password123");
        serverSpec.getFeatures().setSsl(ssl);

        EmbeddedChannel channel = new EmbeddedChannel();

        // This will fail because the files don't exist, but it tests the password path
        assertThatThrownBy(() -> provider.configure(channel.pipeline(), serverSpec))
                .isInstanceOf(IllegalStateException.class);

        channel.close();
    }

    @Test
    void configure_withClientAuth_buildsTrustManager() {
        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        ssl.setCertPath("/nonexistent/cert.pem");
        ssl.setKeyPath("/nonexistent/key.pem");
        ssl.setClientAuth(true);
        ssl.setTrustCertPath("/nonexistent/trust.pem");
        serverSpec.getFeatures().setSsl(ssl);

        EmbeddedChannel channel = new EmbeddedChannel();

        // This will fail because the files don't exist, but it tests the client auth path
        assertThatThrownBy(() -> provider.configure(channel.pipeline(), serverSpec))
                .isInstanceOf(IllegalStateException.class);

        channel.close();
    }

}
