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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FeatureRegistry}.
 */
class FeatureRegistryTest {

    private FeatureRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new FeatureRegistry();
    }

    @Test
    void register_withValidFeature_addsToRegistry() {
        FeatureProvider feature = createFeature("test", 100);
        registry.register(feature);
        assertThat(registry.getFeature("test")).isSameAs(feature);
    }

    @Test
    void register_withNullFeature_throwsException() {
        assertThatThrownBy(() -> registry.register(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void register_withDuplicateName_replacesExisting() {
        FeatureProvider feature1 = createFeature("test", 100);
        FeatureProvider feature2 = createFeature("test", 200);
        registry.register(feature1);
        registry.register(feature2);
        assertThat(registry.getFeature("test")).isSameAs(feature2);
    }

    @Test
    void getFeature_withUnknownName_returnsNull() {
        assertThat(registry.getFeature("unknown")).isNull();
    }

    @Test
    void hasFeature_withRegisteredFeature_returnsTrue() {
        registry.register(createFeature("test", 100));
        assertThat(registry.hasFeature("test")).isTrue();
    }

    @Test
    void hasFeature_withUnregisteredFeature_returnsFalse() {
        assertThat(registry.hasFeature("test")).isFalse();
    }

    @Test
    void getOrderedFeatures_returnsSortedByOrder() {
        FeatureProvider feature1 = createFeature("feature1", 300);
        FeatureProvider feature2 = createFeature("feature2", 100);
        FeatureProvider feature3 = createFeature("feature3", 200);

        registry.register(feature1);
        registry.register(feature2);
        registry.register(feature3);

        List<FeatureProvider> ordered = registry.getOrderedFeatures();

        assertThat(ordered).hasSize(3);
        assertThat(ordered.get(0).getName()).isEqualTo("feature2");
        assertThat(ordered.get(1).getName()).isEqualTo("feature3");
        assertThat(ordered.get(2).getName()).isEqualTo("feature1");
    }

    @Test
    void getOrderedFeatures_returnsUnmodifiableList() {
        registry.register(createFeature("test", 100));
        List<FeatureProvider> ordered = registry.getOrderedFeatures();
        assertThatThrownBy(() -> ordered.add(createFeature("new", 50)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getAllFeatures_returnsUnmodifiableMap() {
        registry.register(createFeature("test", 100));
        assertThatThrownBy(() -> registry.getAllFeatures().put("new", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private FeatureProvider createFeature(String name, int order) {
        FeatureProvider feature = mock(FeatureProvider.class);
        when(feature.getName()).thenReturn(name);
        when(feature.getOrder()).thenReturn(order);
        return feature;
    }
}
