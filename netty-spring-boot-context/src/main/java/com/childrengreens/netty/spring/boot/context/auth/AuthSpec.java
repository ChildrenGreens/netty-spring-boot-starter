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

package com.childrengreens.netty.spring.boot.context.auth;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration specification for authentication.
 *
 * <p>This class defines the authentication settings for a server,
 * including the authentication mode, token configuration, and
 * connection policies.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class AuthSpec {

    /**
     * Whether authentication is enabled.
     */
    private boolean enabled = false;

    /**
     * Authentication mode: TOKEN (HTTP) or CREDENTIAL (WebSocket/TCP).
     */
    private AuthMode mode = AuthMode.TOKEN;

    /**
     * Route for authentication messages (CREDENTIAL mode).
     * Default is "/auth".
     */
    private String authRoute = "/auth";

    /**
     * Authentication timeout in milliseconds (CREDENTIAL mode).
     * If no auth message is received within this time, the connection is closed.
     */
    private long authTimeout = 10000;

    /**
     * Whether to close the connection on authentication failure.
     */
    private boolean closeOnFailure = true;

    /**
     * Token configuration (TOKEN mode).
     */
    @NestedConfigurationProperty
    private TokenSpec token;

    /**
     * Paths excluded from authentication.
     * Supports Ant-style patterns (e.g., /public/**).
     */
    private List<String> excludePaths = new ArrayList<>();

    /**
     * Connection policy for multi-connection control (CREDENTIAL mode).
     */
    @NestedConfigurationProperty
    private ConnectionPolicy connectionPolicy;

    /**
     * Whether to collect authentication metrics.
     */
    private boolean metrics = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AuthMode getMode() {
        return mode;
    }

    public void setMode(AuthMode mode) {
        this.mode = mode;
    }

    public String getAuthRoute() {
        return authRoute;
    }

    public void setAuthRoute(String authRoute) {
        this.authRoute = authRoute;
    }

    public long getAuthTimeout() {
        return authTimeout;
    }

    public void setAuthTimeout(long authTimeout) {
        this.authTimeout = authTimeout;
    }

    public boolean isCloseOnFailure() {
        return closeOnFailure;
    }

    public void setCloseOnFailure(boolean closeOnFailure) {
        this.closeOnFailure = closeOnFailure;
    }

    public TokenSpec getToken() {
        return token;
    }

    public void setToken(TokenSpec token) {
        this.token = token;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public ConnectionPolicy getConnectionPolicy() {
        return connectionPolicy;
    }

    public void setConnectionPolicy(ConnectionPolicy connectionPolicy) {
        this.connectionPolicy = connectionPolicy;
    }

    public boolean isMetrics() {
        return metrics;
    }

    public void setMetrics(boolean metrics) {
        this.metrics = metrics;
    }

}
