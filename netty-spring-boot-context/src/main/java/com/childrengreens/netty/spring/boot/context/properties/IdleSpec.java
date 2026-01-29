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
 * Idle connection detection configuration.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class IdleSpec {

    /**
     * Whether idle detection is enabled.
     */
    private boolean enabled = true;

    /**
     * Read idle timeout in seconds.
     */
    private int readSeconds = 60;

    /**
     * Write idle timeout in seconds.
     */
    private int writeSeconds = 0;

    /**
     * All idle timeout in seconds.
     */
    private int allSeconds = 0;

    /**
     * Return whether idle detection is enabled.
     * @return {@code true} if enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Set whether to enable idle detection.
     * @param enabled {@code true} to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the read idle timeout in seconds.
     * @return the read timeout
     */
    public int getReadSeconds() {
        return this.readSeconds;
    }

    /**
     * Set the read idle timeout in seconds.
     * @param readSeconds the read timeout
     */
    public void setReadSeconds(int readSeconds) {
        this.readSeconds = readSeconds;
    }

    /**
     * Return the write idle timeout in seconds.
     * @return the write timeout
     */
    public int getWriteSeconds() {
        return this.writeSeconds;
    }

    /**
     * Set the write idle timeout in seconds.
     * @param writeSeconds the write timeout
     */
    public void setWriteSeconds(int writeSeconds) {
        this.writeSeconds = writeSeconds;
    }

    /**
     * Return the all idle timeout in seconds.
     * @return the all idle timeout
     */
    public int getAllSeconds() {
        return this.allSeconds;
    }

    /**
     * Set the all idle timeout in seconds.
     * @param allSeconds the all idle timeout
     */
    public void setAllSeconds(int allSeconds) {
        this.allSeconds = allSeconds;
    }

}
