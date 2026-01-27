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

import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import com.childrengreens.netty.spring.boot.context.properties.SslSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.security.cert.CertificateException;

/**
 * Feature provider for SSL/TLS support.
 *
 * <p>Adds SSL handler to the pipeline for secure communication.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 */
public class SslFeatureProvider implements FeatureProvider {

    private static final Logger logger = LoggerFactory.getLogger(SslFeatureProvider.class);

    /**
     * Feature name constant.
     */
    public static final String NAME = "ssl";

    /**
     * Order for this feature - first in the pipeline.
     */
    public static final int ORDER = 10;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        SslSpec ssl = serverSpec.getFeatures().getSsl();
        if (ssl != null && ssl.isEnabled()) {
            try {
                SslContext sslContext = buildSslContext(ssl);
                pipeline.addLast("sslHandler", sslContext.newHandler(pipeline.channel().alloc()));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to configure SSL for server: " +
                        serverSpec.getName(), e);
            }
        }
    }

    @Override
    public boolean isEnabled(ServerSpec serverSpec) {
        SslSpec ssl = serverSpec.getFeatures().getSsl();
        return ssl != null && ssl.isEnabled();
    }

    /**
     * Build the SSL context from the configuration.
     * @param ssl the SSL specification
     * @return the SSL context
     * @throws SSLException if SSL configuration fails
     * @throws CertificateException if certificate generation fails
     */
    private SslContext buildSslContext(SslSpec ssl) throws SSLException, CertificateException {
        SslContextBuilder builder;

        if (ssl.getCertPath() != null && ssl.getKeyPath() != null) {
            // Use provided certificate and key
            File certFile = new File(ssl.getCertPath());
            File keyFile = new File(ssl.getKeyPath());

            if (ssl.getKeyPassword() != null) {
                builder = SslContextBuilder.forServer(certFile, keyFile, ssl.getKeyPassword());
            } else {
                builder = SslContextBuilder.forServer(certFile, keyFile);
            }
        } else {
            // Generate self-signed certificate for development
            logger.warn("No SSL certificate configured, using self-signed certificate");
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            builder = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey());
        }

        // Configure client authentication if enabled
        if (ssl.isClientAuth() && ssl.getTrustCertPath() != null) {
            builder.trustManager(new File(ssl.getTrustCertPath()));
        }

        return builder.build();
    }

}
