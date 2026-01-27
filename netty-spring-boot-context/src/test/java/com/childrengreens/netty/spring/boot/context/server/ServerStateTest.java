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

package com.childrengreens.netty.spring.boot.context.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerState}.
 */
class ServerStateTest {

    @Test
    void values_containsAllStates() {
        ServerState[] values = ServerState.values();

        assertThat(values).containsExactly(
                ServerState.INITIALIZING,
                ServerState.STARTING,
                ServerState.RUNNING,
                ServerState.STOPPING,
                ServerState.STOPPED,
                ServerState.FAILED
        );
    }

    @Test
    void valueOf_returnsCorrectEnum() {
        assertThat(ServerState.valueOf("INITIALIZING")).isEqualTo(ServerState.INITIALIZING);
        assertThat(ServerState.valueOf("STARTING")).isEqualTo(ServerState.STARTING);
        assertThat(ServerState.valueOf("RUNNING")).isEqualTo(ServerState.RUNNING);
        assertThat(ServerState.valueOf("STOPPING")).isEqualTo(ServerState.STOPPING);
        assertThat(ServerState.valueOf("STOPPED")).isEqualTo(ServerState.STOPPED);
        assertThat(ServerState.valueOf("FAILED")).isEqualTo(ServerState.FAILED);
    }
}
