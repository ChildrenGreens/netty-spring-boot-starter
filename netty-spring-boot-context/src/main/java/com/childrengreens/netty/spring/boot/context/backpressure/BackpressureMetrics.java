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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics for backpressure handling.
 *
 * <p>Tracks the number of times various backpressure actions are taken.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class BackpressureMetrics {

    private final String serverName;
    private final AtomicLong suspendCount = new AtomicLong(0);
    private final AtomicLong resumeCount = new AtomicLong(0);
    private final AtomicLong droppedCount = new AtomicLong(0);
    private final AtomicLong disconnectCount = new AtomicLong(0);

    /**
     * Create a new BackpressureMetrics.
     * @param serverName the server name
     */
    public BackpressureMetrics(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Get the server name.
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Increment the suspend count (autoRead set to false).
     */
    public void incrementSuspend() {
        suspendCount.incrementAndGet();
    }

    /**
     * Increment the resume count (autoRead set to true).
     */
    public void incrementResume() {
        resumeCount.incrementAndGet();
    }

    /**
     * Increment the dropped message count.
     */
    public void incrementDropped() {
        droppedCount.incrementAndGet();
    }

    /**
     * Increment the disconnect count.
     */
    public void incrementDisconnect() {
        disconnectCount.incrementAndGet();
    }

    /**
     * Get the total number of times reading was suspended.
     * @return the suspend count
     */
    public long getSuspendCount() {
        return suspendCount.get();
    }

    /**
     * Get the total number of times reading was resumed.
     * @return the resume count
     */
    public long getResumeCount() {
        return resumeCount.get();
    }

    /**
     * Get the total number of dropped messages.
     * @return the dropped count
     */
    public long getDroppedCount() {
        return droppedCount.get();
    }

    /**
     * Get the total number of disconnections due to backpressure.
     * @return the disconnect count
     */
    public long getDisconnectCount() {
        return disconnectCount.get();
    }

    @Override
    public String toString() {
        return "BackpressureMetrics{" +
                "serverName='" + serverName + '\'' +
                ", suspendCount=" + suspendCount +
                ", resumeCount=" + resumeCount +
                ", droppedCount=" + droppedCount +
                ", disconnectCount=" + disconnectCount +
                '}';
    }

}
