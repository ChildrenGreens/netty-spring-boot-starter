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

package com.childrengreens.netty.spring.boot.context.codec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CodecRegistry}.
 */
class CodecRegistryTest {

    private CodecRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CodecRegistry();
    }

    @Test
    void register_withValidCodec_addsToRegistry() {
        NettyCodec codec = new JsonNettyCodec();
        registry.register(codec);
        assertThat(registry.getCodec("json")).isSameAs(codec);
    }

    @Test
    void register_withNullCodec_throwsException() {
        assertThatThrownBy(() -> registry.register(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void register_withDuplicateName_replacesExisting() {
        JsonNettyCodec codec1 = new JsonNettyCodec();
        JsonNettyCodec codec2 = new JsonNettyCodec();
        registry.register(codec1);
        registry.register(codec2);
        assertThat(registry.getCodec("json")).isSameAs(codec2);
    }

    @Test
    void getCodec_withUnknownName_returnsNull() {
        assertThat(registry.getCodec("unknown")).isNull();
    }

    @Test
    void getDefaultCodec_whenEmpty_returnsNull() {
        assertThat(registry.getDefaultCodec()).isNull();
    }

    @Test
    void getDefaultCodec_afterRegister_returnsJsonCodec() {
        JsonNettyCodec codec = new JsonNettyCodec();
        registry.register(codec);
        assertThat(registry.getDefaultCodec()).isSameAs(codec);
    }

    @Test
    void setDefaultCodecName_changesDefaultCodec() {
        JsonNettyCodec jsonCodec = new JsonNettyCodec();
        TestCodec xmlCodec = new TestCodec("xml");
        registry.register(jsonCodec);
        registry.register(xmlCodec);
        registry.setDefaultCodecName("xml");
        assertThat(registry.getDefaultCodec()).isSameAs(xmlCodec);
    }

    @Test
    void hasCodec_withRegisteredCodec_returnsTrue() {
        registry.register(new JsonNettyCodec());
        assertThat(registry.hasCodec("json")).isTrue();
    }

    @Test
    void hasCodec_withUnregisteredCodec_returnsFalse() {
        assertThat(registry.hasCodec("xml")).isFalse();
    }

    @Test
    void getAllCodecs_returnsUnmodifiableMap() {
        registry.register(new JsonNettyCodec());
        assertThat(registry.getAllCodecs()).hasSize(1);
        assertThatThrownBy(() -> registry.getAllCodecs().put("test", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getCodecOrDefault_withExistingCodec_returnsRequestedCodec() {
        JsonNettyCodec codec = new JsonNettyCodec();
        registry.register(codec);
        assertThat(registry.getCodecOrDefault("json")).isSameAs(codec);
    }

    @Test
    void getCodecOrDefault_withMissingCodec_returnsDefaultCodec() {
        JsonNettyCodec codec = new JsonNettyCodec();
        registry.register(codec);
        assertThat(registry.getCodecOrDefault("unknown")).isSameAs(codec);
    }

    /**
     * Test codec implementation.
     */
    static class TestCodec implements NettyCodec {
        private final String name;

        TestCodec(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public byte[] encode(Object object) {
            return new byte[0];
        }

        @Override
        public <T> T decode(byte[] bytes, Class<T> targetType) {
            return null;
        }

        @Override
        public String getContentType() {
            return "application/" + name;
        }
    }
}
