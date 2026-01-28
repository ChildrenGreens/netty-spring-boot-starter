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

package com.childrengreens.netty.spring.boot.context.client;

import com.childrengreens.netty.spring.boot.context.properties.ClientSpec;
import io.netty.channel.ChannelPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClientProfileRegistry}.
 */
class ClientProfileRegistryTest {

    private ClientProfileRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ClientProfileRegistry();
    }

    @Test
    void register_addsProfileToRegistry() {
        ClientProfile profile = createTestProfile("test-profile");

        registry.register(profile);

        assertThat(registry.hasProfile("test-profile")).isTrue();
    }

    @Test
    void getProfile_returnsRegisteredProfile() {
        ClientProfile profile = createTestProfile("test-profile");
        registry.register(profile);

        ClientProfile result = registry.getProfile("test-profile");

        assertThat(result).isSameAs(profile);
    }

    @Test
    void getProfile_withUnknownName_returnsNull() {
        ClientProfile result = registry.getProfile("unknown");

        assertThat(result).isNull();
    }

    @Test
    void hasProfile_withRegisteredProfile_returnsTrue() {
        registry.register(createTestProfile("existing"));

        assertThat(registry.hasProfile("existing")).isTrue();
    }

    @Test
    void hasProfile_withUnknownProfile_returnsFalse() {
        assertThat(registry.hasProfile("unknown")).isFalse();
    }

    @Test
    void getAllProfiles_returnsAllRegistered() {
        registry.register(createTestProfile("profile1"));
        registry.register(createTestProfile("profile2"));

        assertThat(registry.getAllProfiles()).hasSize(2);
        assertThat(registry.getAllProfiles()).containsKeys("profile1", "profile2");
    }

    @Test
    void register_withDuplicateName_replacesExisting() {
        ClientProfile first = createTestProfile("duplicate");
        ClientProfile second = createTestProfile("duplicate");

        registry.register(first);
        registry.register(second);

        assertThat(registry.getProfile("duplicate")).isSameAs(second);
    }

    private ClientProfile createTestProfile(String name) {
        return new ClientProfile() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public void configure(ChannelPipeline pipeline, ClientSpec clientSpec) {
                // No-op for testing
            }
        };
    }

}
