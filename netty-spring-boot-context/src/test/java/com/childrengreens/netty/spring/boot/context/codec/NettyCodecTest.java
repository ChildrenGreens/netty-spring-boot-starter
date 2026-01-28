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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NettyCodec} interface default methods.
 */
class NettyCodecTest {

    @Test
    void getContentType_returnsDefaultOctetStream() {
        NettyCodec codec = new TestNettyCodec();

        assertThat(codec.getContentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void getName_returnsImplementationName() {
        NettyCodec codec = new TestNettyCodec();

        assertThat(codec.getName()).isEqualTo("test");
    }

    @Test
    void encode_returnsEncodedBytes() {
        NettyCodec codec = new TestNettyCodec();

        byte[] result = codec.encode("test");

        assertThat(result).isEqualTo("test".getBytes());
    }

    @Test
    void decode_returnsDecodedObject() {
        NettyCodec codec = new TestNettyCodec();

        String result = codec.decode("test".getBytes(), String.class);

        assertThat(result).isEqualTo("test");
    }

    /**
     * Test implementation of NettyCodec.
     */
    private static class TestNettyCodec implements NettyCodec {

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public byte[] encode(Object object) throws CodecException {
            return object.toString().getBytes();
        }

        @Override
        public <T> T decode(byte[] bytes, Class<T> targetType) throws CodecException {
            return targetType.cast(new String(bytes));
        }
    }
}
