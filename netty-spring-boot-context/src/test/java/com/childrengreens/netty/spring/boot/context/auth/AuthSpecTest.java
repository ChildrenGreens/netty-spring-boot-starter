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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AuthSpec}.
 */
class AuthSpecTest {

    @Test
    void defaultValues() {
        AuthSpec spec = new AuthSpec();

        assertThat(spec.isEnabled()).isFalse();
        assertThat(spec.getMode()).isEqualTo(AuthMode.TOKEN);
        assertThat(spec.getAuthRoute()).isEqualTo("/auth");
        assertThat(spec.getAuthTimeout()).isEqualTo(10000);
        assertThat(spec.isCloseOnFailure()).isTrue();
        assertThat(spec.getToken()).isNull();
        assertThat(spec.getExcludePaths()).isEmpty();
        assertThat(spec.getConnectionPolicy()).isNull();
        assertThat(spec.isMetrics()).isTrue();
    }

    @Test
    void setEnabled_updatesValue() {
        AuthSpec spec = new AuthSpec();
        spec.setEnabled(true);

        assertThat(spec.isEnabled()).isTrue();
    }

    @Test
    void setMode_updatesValue() {
        AuthSpec spec = new AuthSpec();
        spec.setMode(AuthMode.CREDENTIAL);

        assertThat(spec.getMode()).isEqualTo(AuthMode.CREDENTIAL);
    }

    @Test
    void setAuthRoute_updatesValue() {
        AuthSpec spec = new AuthSpec();
        spec.setAuthRoute("/login");

        assertThat(spec.getAuthRoute()).isEqualTo("/login");
    }

    @Test
    void setAuthTimeout_updatesValue() {
        AuthSpec spec = new AuthSpec();
        spec.setAuthTimeout(5000);

        assertThat(spec.getAuthTimeout()).isEqualTo(5000);
    }

    @Test
    void setCloseOnFailure_updatesValue() {
        AuthSpec spec = new AuthSpec();
        spec.setCloseOnFailure(false);

        assertThat(spec.isCloseOnFailure()).isFalse();
    }

    @Test
    void setToken_updatesValue() {
        AuthSpec spec = new AuthSpec();
        TokenSpec tokenSpec = new TokenSpec();
        spec.setToken(tokenSpec);

        assertThat(spec.getToken()).isSameAs(tokenSpec);
    }

    @Test
    void setExcludePaths_updatesValue() {
        AuthSpec spec = new AuthSpec();
        spec.setExcludePaths(List.of("/health", "/public/**"));

        assertThat(spec.getExcludePaths()).containsExactly("/health", "/public/**");
    }

    @Test
    void setConnectionPolicy_updatesValue() {
        AuthSpec spec = new AuthSpec();
        ConnectionPolicy policy = new ConnectionPolicy();
        spec.setConnectionPolicy(policy);

        assertThat(spec.getConnectionPolicy()).isSameAs(policy);
    }

    @Test
    void setMetrics_updatesValue() {
        AuthSpec spec = new AuthSpec();
        spec.setMetrics(false);

        assertThat(spec.isMetrics()).isFalse();
    }
}
