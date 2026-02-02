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

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents an authenticated user principal.
 *
 * <p>This class holds information about the authenticated user and is
 * stored in the channel attributes after successful authentication.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class AuthPrincipal {

    private final String userId;
    private String username;
    private final Set<String> roles = new HashSet<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private final Instant authenticatedAt;

    /**
     * Create a new AuthPrincipal.
     *
     * @param userId the user ID
     */
    public AuthPrincipal(String userId) {
        this.userId = userId;
        this.authenticatedAt = Instant.now();
    }

    /**
     * Return the user ID.
     *
     * @return the user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Return the username.
     *
     * @return the username, may be null
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set the username.
     *
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Return the user's roles.
     *
     * @return an unmodifiable set of roles
     */
    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    /**
     * Add a role to the principal.
     *
     * @param role the role to add
     */
    public void addRole(String role) {
        this.roles.add(role);
    }

    /**
     * Check if the principal has a specific role.
     *
     * @param role the role to check
     * @return true if the principal has the role
     */
    public boolean hasRole(String role) {
        return this.roles.contains(role);
    }

    /**
     * Return the custom attributes.
     *
     * @return an unmodifiable map of attributes
     */
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Get a custom attribute.
     *
     * @param key the attribute key
     * @param <T> the expected type
     * @return the attribute value, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) this.attributes.get(key);
    }

    /**
     * Set a custom attribute.
     *
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    /**
     * Return the time when authentication occurred.
     *
     * @return the authentication timestamp
     */
    public Instant getAuthenticatedAt() {
        return authenticatedAt;
    }

    @Override
    public String toString() {
        return "AuthPrincipal{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", roles=" + roles +
                ", authenticatedAt=" + authenticatedAt +
                '}';
    }

}
