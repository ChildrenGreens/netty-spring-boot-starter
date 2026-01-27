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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ProfileRegistry}.
 */
class ProfileRegistryTest {

    private ProfileRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ProfileRegistry();
    }

    @Test
    void register_withValidProfile_addsToRegistry() {
        Profile profile = createProfile("test");
        registry.register(profile);
        assertThat(registry.getProfile("test")).isSameAs(profile);
    }

    @Test
    void register_withNullProfile_throwsException() {
        assertThatThrownBy(() -> registry.register(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void register_withDuplicateName_replacesExisting() {
        Profile profile1 = createProfile("test");
        Profile profile2 = createProfile("test");
        registry.register(profile1);
        registry.register(profile2);
        assertThat(registry.getProfile("test")).isSameAs(profile2);
    }

    @Test
    void getProfile_withUnknownName_returnsNull() {
        assertThat(registry.getProfile("unknown")).isNull();
    }

    @Test
    void getRequiredProfile_withExistingProfile_returnsProfile() {
        Profile profile = createProfile("test");
        registry.register(profile);
        assertThat(registry.getRequiredProfile("test")).isSameAs(profile);
    }

    @Test
    void getRequiredProfile_withUnknownName_throwsException() {
        assertThatThrownBy(() -> registry.getRequiredProfile("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void hasProfile_withRegisteredProfile_returnsTrue() {
        registry.register(createProfile("test"));
        assertThat(registry.hasProfile("test")).isTrue();
    }

    @Test
    void hasProfile_withUnregisteredProfile_returnsFalse() {
        assertThat(registry.hasProfile("test")).isFalse();
    }

    @Test
    void getAllProfiles_returnsUnmodifiableMap() {
        registry.register(createProfile("test"));
        assertThatThrownBy(() -> registry.getAllProfiles().put("new", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private Profile createProfile(String name) {
        Profile profile = mock(Profile.class);
        when(profile.getName()).thenReturn(name);
        return profile;
    }
}
