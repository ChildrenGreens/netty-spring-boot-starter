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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;

/**
 * JSON codec implementation using Jackson.
 *
 * <p>This codec provides JSON serialization and deserialization
 * using the Jackson library.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class JsonNettyCodec implements NettyCodec {

    /**
     * Codec name constant.
     */
    public static final String NAME = "json";

    private final ObjectMapper objectMapper;

    /**
     * Create a new JsonNettyCodec with default ObjectMapper settings.
     */
    public JsonNettyCodec() {
        this(createDefaultObjectMapper());
    }

    /**
     * Create a new JsonNettyCodec with the specified ObjectMapper.
     * @param objectMapper the ObjectMapper to use
     */
    public JsonNettyCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        registerJavaTimeModule(this.objectMapper);
    }

    /**
     * Create a default ObjectMapper with common settings.
     * @return the configured ObjectMapper
     */
    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        registerJavaTimeModule(mapper);
        return mapper;
    }

    private static void registerJavaTimeModule(ObjectMapper mapper) {
        mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] encode(Object object) throws CodecException {
        try {
            return this.objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new CodecException("Failed to encode object to JSON", e);
        }
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> targetType) throws CodecException {
        try {
            return this.objectMapper.readValue(bytes, targetType);
        } catch (IOException e) {
            throw new CodecException("Failed to decode JSON to " + targetType.getName(), e);
        }
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    /**
     * Return the underlying ObjectMapper.
     * @return the ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

}
