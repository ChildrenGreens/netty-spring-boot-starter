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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ThreadsSpec}.
 */
class ThreadsSpecTest {

    @Test
    void defaultValues() {
        ThreadsSpec spec = new ThreadsSpec();

        assertThat(spec.getBoss()).isEqualTo(1);
        assertThat(spec.getWorker()).isEqualTo(0);
    }

    @Test
    void setBoss() {
        ThreadsSpec spec = new ThreadsSpec();

        spec.setBoss(2);

        assertThat(spec.getBoss()).isEqualTo(2);
    }

    @Test
    void setWorker() {
        ThreadsSpec spec = new ThreadsSpec();

        spec.setWorker(4);

        assertThat(spec.getWorker()).isEqualTo(4);
    }

}
