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

import com.childrengreens.netty.spring.boot.context.auth.AuthSpec;
import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureSpec;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Feature configurations for a Netty server.
 *
 * <p>Features are composable handler suites that can be enabled/disabled
 * and configured for each server instance.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class FeaturesSpec {

    /**
     * Idle connection detection configuration.
     */
    @NestedConfigurationProperty
    private IdleSpec idle;

    /**
     * SSL/TLS configuration.
     */
    @NestedConfigurationProperty
    private SslSpec ssl;

    /**
     * Logging configuration.
     */
    @NestedConfigurationProperty
    private LoggingSpec logging;

    /**
     * Rate limiting configuration.
     */
    @NestedConfigurationProperty
    private RateLimitSpec rateLimit;

    /**
     * Connection limit configuration.
     */
    @NestedConfigurationProperty
    private ConnectionLimitSpec connectionLimit;

    /**
     * Backpressure configuration.
     * @since 0.0.2
     */
    @NestedConfigurationProperty
    private BackpressureSpec backpressure;

    /**
     * Authentication configuration.
     * @since 0.0.2
     */
    @NestedConfigurationProperty
    private AuthSpec auth;

    /**
     * Return the idle connection configuration.
     * @return the idle specification, or {@code null} if not configured
     */
    public IdleSpec getIdle() {
        return this.idle;
    }

    /**
     * Set the idle connection configuration.
     * @param idle the idle specification
     */
    public void setIdle(IdleSpec idle) {
        this.idle = idle;
    }

    /**
     * Return the SSL configuration.
     * @return the SSL specification, or {@code null} if not configured
     */
    public SslSpec getSsl() {
        return this.ssl;
    }

    /**
     * Set the SSL configuration.
     * @param ssl the SSL specification
     */
    public void setSsl(SslSpec ssl) {
        this.ssl = ssl;
    }

    /**
     * Return the logging configuration.
     * @return the logging specification, or {@code null} if not configured
     */
    public LoggingSpec getLogging() {
        return this.logging;
    }

    /**
     * Set the logging configuration.
     * @param logging the logging specification
     */
    public void setLogging(LoggingSpec logging) {
        this.logging = logging;
    }

    /**
     * Return the rate limit configuration.
     * @return the rate limit specification, or {@code null} if not configured
     */
    public RateLimitSpec getRateLimit() {
        return this.rateLimit;
    }

    /**
     * Set the rate limit configuration.
     * @param rateLimit the rate limit specification
     */
    public void setRateLimit(RateLimitSpec rateLimit) {
        this.rateLimit = rateLimit;
    }

    /**
     * Return the connection limit configuration.
     * @return the connection limit specification, or {@code null} if not configured
     */
    public ConnectionLimitSpec getConnectionLimit() {
        return this.connectionLimit;
    }

    /**
     * Set the connection limit configuration.
     * @param connectionLimit the connection limit specification
     */
    public void setConnectionLimit(ConnectionLimitSpec connectionLimit) {
        this.connectionLimit = connectionLimit;
    }

    /**
     * Return the backpressure configuration.
     * @return the backpressure specification, or {@code null} if not configured
     * @since 0.0.2
     */
    public BackpressureSpec getBackpressure() {
        return this.backpressure;
    }

    /**
     * Set the backpressure configuration.
     * @param backpressure the backpressure specification
     * @since 0.0.2
     */
    public void setBackpressure(BackpressureSpec backpressure) {
        this.backpressure = backpressure;
    }

    /**
     * Return the authentication configuration.
     * @return the auth specification, or {@code null} if not configured
     * @since 0.0.2
     */
    public AuthSpec getAuth() {
        return this.auth;
    }

    /**
     * Set the authentication configuration.
     * @param auth the auth specification
     * @since 0.0.2
     */
    public void setAuth(AuthSpec auth) {
        this.auth = auth;
    }

}
