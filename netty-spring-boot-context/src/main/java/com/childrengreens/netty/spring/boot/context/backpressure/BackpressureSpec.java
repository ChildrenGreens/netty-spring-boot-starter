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

package com.childrengreens.netty.spring.boot.context.backpressure;

/**
 * Configuration specification for backpressure handling.
 *
 * <p>Backpressure is used to protect the server from being overwhelmed by fast
 * producers (clients). When the write buffer exceeds the high water mark, the
 * configured strategy is applied.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class BackpressureSpec {

    /**
     * Default high water mark (64KB).
     */
    public static final int DEFAULT_HIGH_WATER_MARK = 64 * 1024;

    /**
     * Default low water mark (32KB).
     */
    public static final int DEFAULT_LOW_WATER_MARK = 32 * 1024;

    /**
     * Default overflow threshold for DISCONNECT strategy (10MB).
     */
    public static final int DEFAULT_OVERFLOW_THRESHOLD = 10 * 1024 * 1024;

    /**
     * Whether backpressure handling is enabled.
     */
    private boolean enabled = false;

    /**
     * High water mark in bytes. When write buffer exceeds this, backpressure is applied.
     */
    private int highWaterMark = DEFAULT_HIGH_WATER_MARK;

    /**
     * Low water mark in bytes. When write buffer drops below this, normal operation resumes.
     */
    private int lowWaterMark = DEFAULT_LOW_WATER_MARK;

    /**
     * The backpressure strategy to use.
     */
    private BackpressureStrategy strategy = BackpressureStrategy.SUSPEND_READ;

    /**
     * Drop policy when using DROP strategy.
     */
    private DropPolicy dropPolicy = DropPolicy.NEWEST;

    /**
     * Overflow threshold for DISCONNECT strategy. When buffer exceeds this, connection is closed.
     */
    private int overflowThreshold = DEFAULT_OVERFLOW_THRESHOLD;

    /**
     * Whether to collect backpressure metrics.
     */
    private boolean metrics = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getHighWaterMark() {
        return highWaterMark;
    }

    public void setHighWaterMark(int highWaterMark) {
        this.highWaterMark = highWaterMark;
    }

    public int getLowWaterMark() {
        return lowWaterMark;
    }

    public void setLowWaterMark(int lowWaterMark) {
        this.lowWaterMark = lowWaterMark;
    }

    public BackpressureStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(BackpressureStrategy strategy) {
        this.strategy = strategy;
    }

    public DropPolicy getDropPolicy() {
        return dropPolicy;
    }

    public void setDropPolicy(DropPolicy dropPolicy) {
        this.dropPolicy = dropPolicy;
    }

    public int getOverflowThreshold() {
        return overflowThreshold;
    }

    public void setOverflowThreshold(int overflowThreshold) {
        this.overflowThreshold = overflowThreshold;
    }

    public boolean isMetrics() {
        return metrics;
    }

    public void setMetrics(boolean metrics) {
        this.metrics = metrics;
    }

}
