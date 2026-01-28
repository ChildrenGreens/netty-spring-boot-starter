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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FeaturesSpec}.
 */
class FeaturesSpecTest {

    @Test
    void defaultValues() {
        FeaturesSpec spec = new FeaturesSpec();

        assertThat(spec.getIdle()).isNull();
        assertThat(spec.getSsl()).isNull();
        assertThat(spec.getLogging()).isNull();
        assertThat(spec.getRateLimit()).isNull();
        assertThat(spec.getConnectionLimit()).isNull();
    }

    @Test
    void setIdle() {
        FeaturesSpec spec = new FeaturesSpec();
        IdleSpec idle = new IdleSpec();
        idle.setEnabled(true);
        idle.setReadSeconds(60);

        spec.setIdle(idle);

        assertThat(spec.getIdle()).isNotNull();
        assertThat(spec.getIdle().isEnabled()).isTrue();
        assertThat(spec.getIdle().getReadSeconds()).isEqualTo(60);
    }

    @Test
    void setSsl() {
        FeaturesSpec spec = new FeaturesSpec();
        SslSpec ssl = new SslSpec();
        ssl.setEnabled(true);
        ssl.setCertPath("/path/to/cert");

        spec.setSsl(ssl);

        assertThat(spec.getSsl()).isNotNull();
        assertThat(spec.getSsl().isEnabled()).isTrue();
        assertThat(spec.getSsl().getCertPath()).isEqualTo("/path/to/cert");
    }

    @Test
    void setLogging() {
        FeaturesSpec spec = new FeaturesSpec();
        LoggingSpec logging = new LoggingSpec();
        logging.setEnabled(true);
        logging.setLevel(LogLevel.DEBUG);

        spec.setLogging(logging);

        assertThat(spec.getLogging()).isNotNull();
        assertThat(spec.getLogging().isEnabled()).isTrue();
        assertThat(spec.getLogging().getLevel()).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void setRateLimit() {
        FeaturesSpec spec = new FeaturesSpec();
        RateLimitSpec rateLimit = new RateLimitSpec();
        rateLimit.setEnabled(true);
        rateLimit.setRequestsPerSecond(100);

        spec.setRateLimit(rateLimit);

        assertThat(spec.getRateLimit()).isNotNull();
        assertThat(spec.getRateLimit().isEnabled()).isTrue();
        assertThat(spec.getRateLimit().getRequestsPerSecond()).isEqualTo(100);
    }

    @Test
    void setConnectionLimit() {
        FeaturesSpec spec = new FeaturesSpec();
        ConnectionLimitSpec connectionLimit = new ConnectionLimitSpec();
        connectionLimit.setEnabled(true);
        connectionLimit.setMaxConnections(1000);

        spec.setConnectionLimit(connectionLimit);

        assertThat(spec.getConnectionLimit()).isNotNull();
        assertThat(spec.getConnectionLimit().isEnabled()).isTrue();
        assertThat(spec.getConnectionLimit().getMaxConnections()).isEqualTo(1000);
    }

}
