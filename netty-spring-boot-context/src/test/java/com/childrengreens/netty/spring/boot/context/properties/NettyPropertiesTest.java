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

package com.childrengreens.netty.spring.boot.context.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NettyProperties}.
 */
class NettyPropertiesTest {

    @Test
    void defaultValues_areCorrect() {
        NettyProperties properties = new NettyProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getServers()).isEmpty();
        assertThat(properties.getDefaults()).isNotNull();
    }

    @Test
    void setEnabled_changesValue() {
        NettyProperties properties = new NettyProperties();
        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    void setServers_changesValue() {
        NettyProperties properties = new NettyProperties();
        ServerSpec server = new ServerSpec();
        server.setName("test");
        properties.getServers().add(server);
        assertThat(properties.getServers()).hasSize(1);
        assertThat(properties.getServers().get(0).getName()).isEqualTo("test");
    }

    @Test
    void setDefaults_changesValue() {
        NettyProperties properties = new NettyProperties();
        DefaultsSpec defaults = new DefaultsSpec();
        properties.setDefaults(defaults);
        assertThat(properties.getDefaults()).isSameAs(defaults);
    }
}
