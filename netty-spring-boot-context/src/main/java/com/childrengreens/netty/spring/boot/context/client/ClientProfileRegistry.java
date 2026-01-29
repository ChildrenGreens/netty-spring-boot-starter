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

import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for client profiles.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see ClientProfile
 */
public class ClientProfileRegistry {

    private final Map<String, ClientProfile> profiles = new ConcurrentHashMap<>();

    /**
     * Register a profile.
     * @param profile the profile to register
     */
    public void register(ClientProfile profile) {
        profiles.put(profile.getName(), profile);
    }

    /**
     * Get a profile by name.
     * @param name the profile name
     * @return the profile, or {@code null} if not found
     */
    @Nullable
    public ClientProfile getProfile(String name) {
        return profiles.get(name);
    }

    /**
     * Check if a profile is registered.
     * @param name the profile name
     * @return {@code true} if registered
     */
    public boolean hasProfile(String name) {
        return profiles.containsKey(name);
    }

    /**
     * Get all registered profiles.
     * @return the profiles map
     */
    public Map<String, ClientProfile> getAllProfiles() {
        return Map.copyOf(profiles);
    }

}
