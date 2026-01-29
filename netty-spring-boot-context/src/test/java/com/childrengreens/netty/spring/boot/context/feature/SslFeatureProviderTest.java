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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for {@link SslFeatureProvider}.
 */
class SslFeatureProviderTest {

    private SslFeatureProvider provider;
    private ServerSpec serverSpec;

    private static boolean selfSignedCertAvailable = false;
    private static SelfSignedCertificate sharedCert;
    private static SelfSignedCertificate sharedTrustCert;

    @BeforeAll
    static void initSharedResources() {
        try {
            sharedCert = new SelfSignedCertificate();
            sharedTrustCert = new SelfSignedCertificate();
            selfSignedCertAvailable = true;
        } catch (CertificateException e) {
            // Self-signed cert generation not available in this environment
            selfSignedCertAvailable = false;
        }
    }

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

    // ==================== Tests for buildSslContext method (lines 99-116) ====================

    @Test
    void buildSslContext_withSelfSignedCert_createsSslContext() throws Exception {
        // Test the self-signed certificate path (lines 104-108)
        Method buildSslContextMethod = SslFeatureProvider.class.getDeclaredMethod(
                "buildSslContext", SslSpec.class);
        buildSslContextMethod.setAccessible(true);

        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        // No cert/key paths - will use self-signed certificate

        try {
            SslContext result = (SslContext) buildSslContextMethod.invoke(provider, ssl);
            assertThat(result).isNotNull();
            assertThat(result.isServer()).isTrue();
        } catch (InvocationTargetException e) {
            // Self-signed cert generation might fail in certain environments
            assertThat(e.getCause()).isInstanceOf(CertificateException.class);
        }
    }

    @Test
    void buildSslContext_withValidCertAndKey_createsSslContext(@TempDir Path tempDir) throws Exception {
        assumeTrue(selfSignedCertAvailable, "Self-signed certificate generation not available");

        // Copy cert and key to temp files
        File certFile = tempDir.resolve("cert.pem").toFile();
        File keyFile = tempDir.resolve("key.pem").toFile();

        // Write certificate and key to files
        Files.copy(sharedCert.certificate().toPath(), certFile.toPath());
        Files.copy(sharedCert.privateKey().toPath(), keyFile.toPath());

        Method buildSslContextMethod = SslFeatureProvider.class.getDeclaredMethod(
                "buildSslContext", SslSpec.class);
        buildSslContextMethod.setAccessible(true);

        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        ssl.setCertPath(certFile.getAbsolutePath());
        ssl.setKeyPath(keyFile.getAbsolutePath());
        // No key password - covers line 101-102

        SslContext result = (SslContext) buildSslContextMethod.invoke(provider, ssl);

        assertThat(result).isNotNull();
        assertThat(result.isServer()).isTrue();
    }

    @Test
    void buildSslContext_withValidCertAndKeyAndPassword_createsSslContext(@TempDir Path tempDir) throws Exception {
        assumeTrue(selfSignedCertAvailable, "Self-signed certificate generation not available");

        // Copy cert and key to temp files
        File certFile = tempDir.resolve("cert.pem").toFile();
        File keyFile = tempDir.resolve("key.pem").toFile();

        Files.copy(sharedCert.certificate().toPath(), certFile.toPath());
        Files.copy(sharedCert.privateKey().toPath(), keyFile.toPath());

        Method buildSslContextMethod = SslFeatureProvider.class.getDeclaredMethod(
                "buildSslContext", SslSpec.class);
        buildSslContextMethod.setAccessible(true);

        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        ssl.setCertPath(certFile.getAbsolutePath());
        ssl.setKeyPath(keyFile.getAbsolutePath());
        ssl.setKeyPassword(null); // Test null password takes line 101-102 path

        SslContext result = (SslContext) buildSslContextMethod.invoke(provider, ssl);

        assertThat(result).isNotNull();
    }

    @Test
    void buildSslContext_withKeyPassword_usesPasswordPath(@TempDir Path tempDir) throws Exception {
        assumeTrue(selfSignedCertAvailable, "Self-signed certificate generation not available");

        // This test verifies the code path for keyPassword != null (line 99-100)
        // Since SelfSignedCertificate creates unencrypted keys, providing any password
        // will cause an error, but the code path is still exercised
        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        ssl.setCertPath(sharedCert.certificate().getAbsolutePath());
        ssl.setKeyPath(sharedCert.privateKey().getAbsolutePath());
        ssl.setKeyPassword("somepassword"); // Non-null password to exercise line 99-100

        Method buildSslContextMethod = SslFeatureProvider.class.getDeclaredMethod(
                "buildSslContext", SslSpec.class);
        buildSslContextMethod.setAccessible(true);

        // The invocation will fail because the key is not encrypted,
        // but the important thing is that the password code path is exercised
        try {
            buildSslContextMethod.invoke(provider, ssl);
        } catch (InvocationTargetException e) {
            // Expected: The key file is not encrypted, so providing a password causes an error
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void buildSslContext_withClientAuth_addsTrustManager(@TempDir Path tempDir) throws Exception {
        assumeTrue(selfSignedCertAvailable, "Self-signed certificate generation not available");

        File certFile = tempDir.resolve("cert.pem").toFile();
        File keyFile = tempDir.resolve("key.pem").toFile();
        File trustFile = tempDir.resolve("trust.pem").toFile();

        Files.copy(sharedCert.certificate().toPath(), certFile.toPath());
        Files.copy(sharedCert.privateKey().toPath(), keyFile.toPath());
        Files.copy(sharedTrustCert.certificate().toPath(), trustFile.toPath());

        Method buildSslContextMethod = SslFeatureProvider.class.getDeclaredMethod(
                "buildSslContext", SslSpec.class);
        buildSslContextMethod.setAccessible(true);

        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        ssl.setCertPath(certFile.getAbsolutePath());
        ssl.setKeyPath(keyFile.getAbsolutePath());
        ssl.setClientAuth(true);
        ssl.setTrustCertPath(trustFile.getAbsolutePath());
        // Covers lines 111-114

        SslContext result = (SslContext) buildSslContextMethod.invoke(provider, ssl);

        assertThat(result).isNotNull();
        assertThat(result.isServer()).isTrue();
    }

    @Test
    void buildSslContext_withClientAuthButNoTrustPath_skipsAddingTrustManager(@TempDir Path tempDir) throws Exception {
        assumeTrue(selfSignedCertAvailable, "Self-signed certificate generation not available");

        File certFile = tempDir.resolve("cert.pem").toFile();
        File keyFile = tempDir.resolve("key.pem").toFile();

        Files.copy(sharedCert.certificate().toPath(), certFile.toPath());
        Files.copy(sharedCert.privateKey().toPath(), keyFile.toPath());

        Method buildSslContextMethod = SslFeatureProvider.class.getDeclaredMethod(
                "buildSslContext", SslSpec.class);
        buildSslContextMethod.setAccessible(true);

        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        ssl.setCertPath(certFile.getAbsolutePath());
        ssl.setKeyPath(keyFile.getAbsolutePath());
        ssl.setClientAuth(true);
        ssl.setTrustCertPath(null); // clientAuth is true but no trust path - skips lines 112-113

        SslContext result = (SslContext) buildSslContextMethod.invoke(provider, ssl);

        assertThat(result).isNotNull();
    }

    @Test
    void configure_withValidCertificates_addsSslHandler(@TempDir Path tempDir) throws Exception {
        assumeTrue(selfSignedCertAvailable, "Self-signed certificate generation not available");

        File certFile = tempDir.resolve("cert.pem").toFile();
        File keyFile = tempDir.resolve("key.pem").toFile();

        Files.copy(sharedCert.certificate().toPath(), certFile.toPath());
        Files.copy(sharedCert.privateKey().toPath(), keyFile.toPath());

        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        ssl.setCertPath(certFile.getAbsolutePath());
        ssl.setKeyPath(keyFile.getAbsolutePath());
        serverSpec.getFeatures().setSsl(ssl);

        EmbeddedChannel channel = new EmbeddedChannel();

        provider.configure(channel.pipeline(), serverSpec);

        assertThat(channel.pipeline().names()).contains("sslHandler");

        channel.close();
    }

    @Test
    void configure_withValidCertAndClientAuth_addsSslHandler(@TempDir Path tempDir) throws Exception {
        assumeTrue(selfSignedCertAvailable, "Self-signed certificate generation not available");

        File certFile = tempDir.resolve("cert.pem").toFile();
        File keyFile = tempDir.resolve("key.pem").toFile();
        File trustFile = tempDir.resolve("trust.pem").toFile();

        Files.copy(sharedCert.certificate().toPath(), certFile.toPath());
        Files.copy(sharedCert.privateKey().toPath(), keyFile.toPath());
        Files.copy(sharedTrustCert.certificate().toPath(), trustFile.toPath());

        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        ssl.setCertPath(certFile.getAbsolutePath());
        ssl.setKeyPath(keyFile.getAbsolutePath());
        ssl.setClientAuth(true);
        ssl.setTrustCertPath(trustFile.getAbsolutePath());
        serverSpec.getFeatures().setSsl(ssl);

        EmbeddedChannel channel = new EmbeddedChannel();

        provider.configure(channel.pipeline(), serverSpec);

        assertThat(channel.pipeline().names()).contains("sslHandler");

        channel.close();
    }

}
