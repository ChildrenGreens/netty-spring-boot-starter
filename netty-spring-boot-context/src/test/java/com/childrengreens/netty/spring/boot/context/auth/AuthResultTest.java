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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AuthResult}.
 */
class AuthResultTest {

    @Test
    void success_createsSuccessfulResult() {
        AuthResult result = AuthResult.success("user123");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPrincipal()).isNotNull();
        assertThat(result.getPrincipal().getUserId()).isEqualTo("user123");
        assertThat(result.getErrorCode()).isNull();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void failure_createsFailedResult() {
        AuthResult result = AuthResult.failure("INVALID_TOKEN", "Token has expired");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getPrincipal()).isNull();
        assertThat(result.getErrorCode()).isEqualTo("INVALID_TOKEN");
        assertThat(result.getErrorMessage()).isEqualTo("Token has expired");
    }

    @Test
    void withAttribute_addsAttributeToPrincipal() {
        AuthResult result = AuthResult.success("user123")
                .withAttribute("nickname", "John")
                .withAttribute("level", 5);

        assertThat(result.getPrincipal().<String>getAttribute("nickname")).isEqualTo("John");
        assertThat(result.getPrincipal().<Integer>getAttribute("level")).isEqualTo(5);
    }

    @Test
    void withUsername_setsUsername() {
        AuthResult result = AuthResult.success("user123")
                .withUsername("admin");

        assertThat(result.getPrincipal().getUsername()).isEqualTo("admin");
    }

    @Test
    void withRoles_addsRoles() {
        AuthResult result = AuthResult.success("user123")
                .withRoles("admin", "user", "moderator");

        assertThat(result.getPrincipal().getRoles())
                .containsExactlyInAnyOrder("admin", "user", "moderator");
    }

    @Test
    void withAttribute_onFailure_doesNotThrow() {
        AuthResult result = AuthResult.failure("ERROR", "message")
                .withAttribute("key", "value");

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void toResponseMap_forSuccess_containsPayload() {
        AuthResult result = AuthResult.success("user123")
                .withUsername("admin")
                .withRoles("admin")
                .withAttribute("nickname", "John");

        Map<String, Object> response = result.toResponseMap();

        assertThat(response.get("success")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) response.get("payload");
        assertThat(payload.get("userId")).isEqualTo("user123");
        assertThat(payload.get("username")).isEqualTo("admin");
        assertThat(payload.get("nickname")).isEqualTo("John");
    }

    @Test
    void toResponseMap_forFailure_containsError() {
        AuthResult result = AuthResult.failure("INVALID_TOKEN", "Token expired");

        Map<String, Object> response = result.toResponseMap();

        assertThat(response.get("success")).isEqualTo(false);
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) response.get("error");
        assertThat(error.get("code")).isEqualTo("INVALID_TOKEN");
        assertThat(error.get("message")).isEqualTo("Token expired");
    }
}
