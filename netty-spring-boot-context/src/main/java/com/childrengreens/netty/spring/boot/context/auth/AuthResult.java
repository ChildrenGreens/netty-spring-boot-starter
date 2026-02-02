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

import java.util.HashMap;
import java.util.Map;

/**
 * Result of an authentication attempt.
 *
 * <p>Contains information about whether authentication succeeded or failed,
 * along with the authenticated principal or error details.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class AuthResult {

    private final boolean success;
    private final AuthPrincipal principal;
    private final String errorCode;
    private final String errorMessage;

    private AuthResult(boolean success, AuthPrincipal principal,
                       String errorCode, String errorMessage) {
        this.success = success;
        this.principal = principal;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Create a successful authentication result.
     *
     * @param userId the authenticated user ID
     * @return a successful result with the principal
     */
    public static AuthResult success(String userId) {
        return new AuthResult(true, new AuthPrincipal(userId), null, null);
    }

    /**
     * Create a failed authentication result.
     *
     * @param errorCode the error code
     * @param errorMessage the error message
     * @return a failed result with error details
     */
    public static AuthResult failure(String errorCode, String errorMessage) {
        return new AuthResult(false, null, errorCode, errorMessage);
    }

    /**
     * Add an attribute to the principal.
     *
     * @param key the attribute key
     * @param value the attribute value
     * @return this result for chaining
     */
    public AuthResult withAttribute(String key, Object value) {
        if (this.principal != null) {
            this.principal.setAttribute(key, value);
        }
        return this;
    }

    /**
     * Set the username on the principal.
     *
     * @param username the username
     * @return this result for chaining
     */
    public AuthResult withUsername(String username) {
        if (this.principal != null) {
            this.principal.setUsername(username);
        }
        return this;
    }

    /**
     * Set the roles on the principal.
     *
     * @param roles the roles
     * @return this result for chaining
     */
    public AuthResult withRoles(String... roles) {
        if (this.principal != null) {
            for (String role : roles) {
                this.principal.addRole(role);
            }
        }
        return this;
    }

    /**
     * Return whether authentication was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Return the authenticated principal.
     *
     * @return the principal, or null if authentication failed
     */
    public AuthPrincipal getPrincipal() {
        return principal;
    }

    /**
     * Return the error code.
     *
     * @return the error code, or null if authentication succeeded
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Return the error message.
     *
     * @return the error message, or null if authentication succeeded
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Convert this result to a response map for sending to the client.
     *
     * @return a map containing the response data
     */
    public Map<String, Object> toResponseMap() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (success && principal != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", principal.getUserId());
            if (principal.getUsername() != null) {
                payload.put("username", principal.getUsername());
            }
            if (!principal.getRoles().isEmpty()) {
                payload.put("roles", principal.getRoles());
            }
            if (!principal.getAttributes().isEmpty()) {
                payload.putAll(principal.getAttributes());
            }
            response.put("payload", payload);
        } else if (!success) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", errorCode);
            error.put("message", errorMessage);
            response.put("error", error);
        }

        return response;
    }

}
