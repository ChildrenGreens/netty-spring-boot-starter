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

/**
 * Observability configuration for metrics and health checks.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 */
public class ObservabilitySpec {

    /**
     * Whether metrics collection is enabled.
     */
    private boolean metrics = true;

    /**
     * Whether health indicator is enabled.
     */
    private boolean health = true;

    /**
     * Return whether metrics is enabled.
     * @return {@code true} if metrics is enabled
     */
    public boolean isMetrics() {
        return this.metrics;
    }

    /**
     * Set whether to enable metrics.
     * @param metrics {@code true} to enable metrics
     */
    public void setMetrics(boolean metrics) {
        this.metrics = metrics;
    }

    /**
     * Return whether health indicator is enabled.
     * @return {@code true} if health is enabled
     */
    public boolean isHealth() {
        return this.health;
    }

    /**
     * Set whether to enable health indicator.
     * @param health {@code true} to enable health
     */
    public void setHealth(boolean health) {
        this.health = health;
    }

}
