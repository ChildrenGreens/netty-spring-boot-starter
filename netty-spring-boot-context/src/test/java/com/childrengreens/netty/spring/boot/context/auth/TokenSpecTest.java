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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TokenSpec}.
 */
class TokenSpecTest {

    @Test
    void defaultValues() {
        TokenSpec spec = new TokenSpec();

        assertThat(spec.getType()).isEqualTo(TokenType.JWT);
        assertThat(spec.getHeaderName()).isEqualTo("Authorization");
        assertThat(spec.getSecret()).isNull();
        assertThat(spec.getExpireSeconds()).isEqualTo(3600);
        assertThat(spec.getIssuer()).isNull();
    }

    @Test
    void setType_updatesValue() {
        TokenSpec spec = new TokenSpec();
        spec.setType(TokenType.API_KEY);

        assertThat(spec.getType()).isEqualTo(TokenType.API_KEY);
    }

    @Test
    void setHeaderName_updatesValue() {
        TokenSpec spec = new TokenSpec();
        spec.setHeaderName("X-Api-Key");

        assertThat(spec.getHeaderName()).isEqualTo("X-Api-Key");
    }

    @Test
    void setSecret_updatesValue() {
        TokenSpec spec = new TokenSpec();
        spec.setSecret("my-secret-key");

        assertThat(spec.getSecret()).isEqualTo("my-secret-key");
    }

    @Test
    void setExpireSeconds_updatesValue() {
        TokenSpec spec = new TokenSpec();
        spec.setExpireSeconds(7200);

        assertThat(spec.getExpireSeconds()).isEqualTo(7200);
    }

    @Test
    void setIssuer_updatesValue() {
        TokenSpec spec = new TokenSpec();
        spec.setIssuer("my-app");

        assertThat(spec.getIssuer()).isEqualTo("my-app");
    }
}
