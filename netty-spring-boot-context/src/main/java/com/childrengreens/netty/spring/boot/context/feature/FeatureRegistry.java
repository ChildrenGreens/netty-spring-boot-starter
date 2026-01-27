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

package com.childrengreens.netty.spring.boot.context.feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing feature providers.
 *
 * <p>This registry holds all available feature providers and provides
 * ordered access for pipeline configuration.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 * @see FeatureProvider
 */
public class FeatureRegistry {

    private static final Logger logger = LoggerFactory.getLogger(FeatureRegistry.class);

    private final Map<String, FeatureProvider> features = new ConcurrentHashMap<>();

    /**
     * Register a feature provider.
     * @param provider the feature provider to register
     */
    public void register(FeatureProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("FeatureProvider must not be null");
        }
        FeatureProvider existing = this.features.put(provider.getName(), provider);
        if (existing != null) {
            logger.warn("Replaced existing feature: {}", provider.getName());
        } else {
            logger.debug("Registered feature: {} (order={})", provider.getName(), provider.getOrder());
        }
    }

    /**
     * Get a feature provider by name.
     * @param name the feature name
     * @return the feature provider, or {@code null} if not found
     */
    public FeatureProvider getFeature(String name) {
        return this.features.get(name);
    }

    /**
     * Return all registered features sorted by order.
     * @return a sorted list of feature providers
     */
    public List<FeatureProvider> getOrderedFeatures() {
        List<FeatureProvider> sorted = new ArrayList<>(this.features.values());
        sorted.sort(Comparator.comparingInt(FeatureProvider::getOrder));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Return whether a feature with the given name exists.
     * @param name the feature name
     * @return {@code true} if the feature exists
     */
    public boolean hasFeature(String name) {
        return this.features.containsKey(name);
    }

    /**
     * Return all registered features.
     * @return an unmodifiable map of features
     */
    public Map<String, FeatureProvider> getAllFeatures() {
        return Collections.unmodifiableMap(this.features);
    }

}
