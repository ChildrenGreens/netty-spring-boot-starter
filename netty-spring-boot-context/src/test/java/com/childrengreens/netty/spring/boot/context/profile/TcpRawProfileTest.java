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

package com.childrengreens.netty.spring.boot.context.profile;

import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TcpRawProfile}.
 */
class TcpRawProfileTest {

    private TcpRawProfile profile;
    private ServerSpec serverSpec;

    @BeforeEach
    void setUp() {
        profile = new TcpRawProfile();
        serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
    }

    @Test
    void getName_returnsTcpRaw() {
        assertThat(profile.getName()).isEqualTo("tcp-raw");
    }

    @Test
    void getDefaultCodec_returnsNone() {
        assertThat(profile.getDefaultCodec()).isEqualTo("none");
    }

    @Test
    void supportsDispatcher_returnsFalse() {
        assertThat(profile.supportsDispatcher()).isFalse();
    }

    @Test
    void configure_addsNoHandlers() {
        EmbeddedChannel channel = new EmbeddedChannel();
        int initialHandlerCount = channel.pipeline().names().size();

        profile.configure(channel.pipeline(), serverSpec);

        // No new handlers added
        assertThat(channel.pipeline().names().size()).isEqualTo(initialHandlerCount);

        channel.close();
    }

}
