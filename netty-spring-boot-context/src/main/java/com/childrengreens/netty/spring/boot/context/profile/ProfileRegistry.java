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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing protocol stack profiles.
 *
 * <p>This registry holds all available profiles and provides lookup
 * functionality for profile configuration during server startup.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see Profile
 */
public class ProfileRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ProfileRegistry.class);

    private final Map<String, Profile> profiles = new ConcurrentHashMap<>();

    /**
     * Register a profile.
     * @param profile the profile to register
     */
    public void register(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile must not be null");
        }
        Profile existing = this.profiles.put(profile.getName(), profile);
        if (existing != null) {
            logger.warn("Replaced existing profile: {}", profile.getName());
        } else {
            logger.debug("Registered profile: {}", profile.getName());
        }
    }

    /**
     * Get a profile by name.
     * @param name the profile name
     * @return the profile, or {@code null} if not found
     */
    public Profile getProfile(String name) {
        return this.profiles.get(name);
    }

    /**
     * Get a profile by name, throwing an exception if not found.
     * @param name the profile name
     * @return the profile
     * @throws IllegalArgumentException if the profile is not found
     */
    public Profile getRequiredProfile(String name) {
        Profile profile = getProfile(name);
        if (profile == null) {
            throw new IllegalArgumentException("Profile not found: " + name +
                    ". Available profiles: " + this.profiles.keySet());
        }
        return profile;
    }

    /**
     * Return whether a profile with the given name exists.
     * @param name the profile name
     * @return {@code true} if the profile exists
     */
    public boolean hasProfile(String name) {
        return this.profiles.containsKey(name);
    }

    /**
     * Return all registered profiles.
     * @return an unmodifiable map of profiles
     */
    public Map<String, Profile> getAllProfiles() {
        return Collections.unmodifiableMap(this.profiles);
    }

    /**
     * Remove a profile by name.
     * @param name the profile name
     * @return the removed profile, or {@code null} if not found
     */
    public Profile remove(String name) {
        return this.profiles.remove(name);
    }

}
