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

import io.netty.handler.logging.LogLevel;

/**
 * Logging configuration for Netty handler.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public class LoggingSpec {

    /**
     * Whether logging is enabled.
     */
    private boolean enabled = false;

    /**
     * Log level for the logging handler.
     */
    private LogLevel level = LogLevel.DEBUG;

    /**
     * Return whether logging is enabled.
     * @return {@code true} if logging is enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Set whether to enable logging.
     * @param enabled {@code true} to enable logging
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the log level.
     * @return the log level
     */
    public LogLevel getLevel() {
        return this.level;
    }

    /**
     * Set the log level.
     * @param level the log level
     */
    public void setLevel(LogLevel level) {
        this.level = level;
    }

}
