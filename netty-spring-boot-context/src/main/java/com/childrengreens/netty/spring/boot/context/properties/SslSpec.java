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
 * SSL/TLS configuration for secure connections.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public class SslSpec {

    /**
     * Whether SSL is enabled.
     */
    private boolean enabled = false;

    /**
     * Path to the certificate file.
     */
    private String certPath;

    /**
     * Path to the private key file.
     */
    private String keyPath;

    /**
     * Private key password.
     */
    private String keyPassword;

    /**
     * Path to the trust certificate file for client authentication.
     */
    private String trustCertPath;

    /**
     * Whether to require client authentication (mTLS).
     */
    private boolean clientAuth = false;

    /**
     * Return whether SSL is enabled.
     * @return {@code true} if SSL is enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Set whether to enable SSL.
     * @param enabled {@code true} to enable SSL
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the certificate file path.
     * @return the certificate path
     */
    public String getCertPath() {
        return this.certPath;
    }

    /**
     * Set the certificate file path.
     * @param certPath the certificate path
     */
    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    /**
     * Return the private key file path.
     * @return the key path
     */
    public String getKeyPath() {
        return this.keyPath;
    }

    /**
     * Set the private key file path.
     * @param keyPath the key path
     */
    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }

    /**
     * Return the private key password.
     * @return the key password
     */
    public String getKeyPassword() {
        return this.keyPassword;
    }

    /**
     * Set the private key password.
     * @param keyPassword the key password
     */
    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    /**
     * Return the trust certificate file path.
     * @return the trust certificate path
     */
    public String getTrustCertPath() {
        return this.trustCertPath;
    }

    /**
     * Set the trust certificate file path.
     * @param trustCertPath the trust certificate path
     */
    public void setTrustCertPath(String trustCertPath) {
        this.trustCertPath = trustCertPath;
    }

    /**
     * Return whether client authentication is required.
     * @return {@code true} if client auth is required
     */
    public boolean isClientAuth() {
        return this.clientAuth;
    }

    /**
     * Set whether to require client authentication.
     * @param clientAuth {@code true} to require client auth
     */
    public void setClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
    }

}
