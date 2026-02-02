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
 * Tests for {@link ConnectionPolicy}.
 */
class ConnectionPolicyTest {

    @Test
    void defaultValues() {
        ConnectionPolicy policy = new ConnectionPolicy();

        assertThat(policy.isAllowMultiple()).isFalse();
        assertThat(policy.getStrategy()).isEqualTo(ConnectionStrategy.KICK_OLD);
        assertThat(policy.getMaxConnectionsPerUser()).isEqualTo(1);
    }

    @Test
    void setAllowMultiple_updatesValue() {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setAllowMultiple(true);

        assertThat(policy.isAllowMultiple()).isTrue();
    }

    @Test
    void setStrategy_updatesValue() {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setStrategy(ConnectionStrategy.REJECT_NEW);

        assertThat(policy.getStrategy()).isEqualTo(ConnectionStrategy.REJECT_NEW);
    }

    @Test
    void setMaxConnectionsPerUser_updatesValue() {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setMaxConnectionsPerUser(5);

        assertThat(policy.getMaxConnectionsPerUser()).isEqualTo(5);
    }
}
