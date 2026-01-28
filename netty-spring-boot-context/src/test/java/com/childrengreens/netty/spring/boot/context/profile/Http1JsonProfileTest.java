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
 * Tests for {@link Http1JsonProfile}.
 */
class Http1JsonProfileTest {

    private Http1JsonProfile profile;
    private ServerSpec serverSpec;

    @BeforeEach
    void setUp() {
        profile = new Http1JsonProfile();
        serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
    }

    @Test
    void getName_returnsHttp1Json() {
        assertThat(profile.getName()).isEqualTo("http1-json");
    }

    @Test
    void getDefaultCodec_returnsJson() {
        assertThat(profile.getDefaultCodec()).isEqualTo("json");
    }

    @Test
    void supportsDispatcher_returnsTrue() {
        assertThat(profile.supportsDispatcher()).isTrue();
    }

    @Test
    void configure_addsHttpCodecAndAggregator() {
        EmbeddedChannel channel = new EmbeddedChannel();

        profile.configure(channel.pipeline(), serverSpec);

        assertThat(channel.pipeline().names()).contains("httpCodec", "httpAggregator");

        channel.close();
    }

}
