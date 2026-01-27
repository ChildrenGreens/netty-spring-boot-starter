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

/**
 * Strategy interface for message encoding and decoding.
 *
 * <p>Implementations provide serialization/deserialization support
 * for various formats such as JSON, Protobuf, or custom binary protocols.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 * @see JsonNettyCodec
 * @see CodecRegistry
 */
public interface NettyCodec {

    /**
     * Return the unique name of this codec.
     * @return the codec name (e.g., "json", "protobuf")
     */
    String getName();

    /**
     * Encode an object to bytes.
     * @param object the object to encode
     * @return the encoded bytes
     * @throws CodecException if encoding fails
     */
    byte[] encode(Object object) throws CodecException;

    /**
     * Decode bytes to an object of the specified type.
     * @param bytes the bytes to decode
     * @param targetType the target type
     * @param <T> the type parameter
     * @return the decoded object
     * @throws CodecException if decoding fails
     */
    <T> T decode(byte[] bytes, Class<T> targetType) throws CodecException;

    /**
     * Return the content type produced by this codec.
     * @return the content type (e.g., "application/json")
     */
    default String getContentType() {
        return "application/octet-stream";
    }

}
