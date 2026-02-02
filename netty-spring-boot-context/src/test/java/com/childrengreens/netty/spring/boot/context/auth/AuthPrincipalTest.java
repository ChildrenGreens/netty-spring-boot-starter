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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AuthPrincipal}.
 */
class AuthPrincipalTest {

    private AuthPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new AuthPrincipal("user123");
    }

    @Test
    void constructor_setsUserIdAndTimestamp() {
        assertThat(principal.getUserId()).isEqualTo("user123");
        assertThat(principal.getAuthenticatedAt()).isNotNull();
        assertThat(principal.getAuthenticatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void setUsername_updatesUsername() {
        principal.setUsername("admin");

        assertThat(principal.getUsername()).isEqualTo("admin");
    }

    @Test
    void addRole_addsRoleToSet() {
        principal.addRole("admin");
        principal.addRole("user");

        assertThat(principal.getRoles()).containsExactlyInAnyOrder("admin", "user");
    }

    @Test
    void hasRole_returnsTrueForExistingRole() {
        principal.addRole("admin");

        assertThat(principal.hasRole("admin")).isTrue();
        assertThat(principal.hasRole("user")).isFalse();
    }

    @Test
    void setAttribute_storesAttribute() {
        principal.setAttribute("level", 5);
        principal.setAttribute("nickname", "John");

        assertThat(principal.<Integer>getAttribute("level")).isEqualTo(5);
        assertThat(principal.<String>getAttribute("nickname")).isEqualTo("John");
    }

    @Test
    void getAttribute_returnsNullForMissingKey() {
        assertThat(principal.<String>getAttribute("nonexistent")).isNull();
    }

    @Test
    void getRoles_returnsUnmodifiableSet() {
        principal.addRole("admin");

        assertThatThrownBy(() -> principal.getRoles().add("hacker"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getAttributes_returnsUnmodifiableMap() {
        principal.setAttribute("key", "value");

        assertThatThrownBy(() -> principal.getAttributes().put("hacker", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void toString_containsUserId() {
        String str = principal.toString();

        assertThat(str).contains("user123");
        assertThat(str).contains("AuthPrincipal");
    }
}
