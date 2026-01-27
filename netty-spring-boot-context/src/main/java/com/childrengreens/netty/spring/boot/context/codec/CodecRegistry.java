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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing codec implementations.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 * @see NettyCodec
 */
public class CodecRegistry {

    private static final Logger logger = LoggerFactory.getLogger(CodecRegistry.class);

    private final Map<String, NettyCodec> codecs = new ConcurrentHashMap<>();

    private String defaultCodecName = JsonNettyCodec.NAME;

    /**
     * Register a codec.
     * @param codec the codec to register
     */
    public void register(NettyCodec codec) {
        if (codec == null) {
            throw new IllegalArgumentException("Codec must not be null");
        }
        NettyCodec existing = this.codecs.put(codec.getName(), codec);
        if (existing != null) {
            logger.warn("Replaced existing codec: {}", codec.getName());
        } else {
            logger.debug("Registered codec: {}", codec.getName());
        }
    }

    /**
     * Get a codec by name.
     * @param name the codec name
     * @return the codec, or {@code null} if not found
     */
    public NettyCodec getCodec(String name) {
        return this.codecs.get(name);
    }

    /**
     * Get a codec by name, falling back to the default codec.
     * @param name the codec name
     * @return the codec
     */
    public NettyCodec getCodecOrDefault(String name) {
        NettyCodec codec = this.codecs.get(name);
        if (codec == null) {
            codec = this.codecs.get(this.defaultCodecName);
        }
        return codec;
    }

    /**
     * Get the default codec.
     * @return the default codec
     */
    public NettyCodec getDefaultCodec() {
        return this.codecs.get(this.defaultCodecName);
    }

    /**
     * Set the default codec name.
     * @param name the default codec name
     */
    public void setDefaultCodecName(String name) {
        this.defaultCodecName = name;
    }

    /**
     * Return whether a codec with the given name exists.
     * @param name the codec name
     * @return {@code true} if the codec exists
     */
    public boolean hasCodec(String name) {
        return this.codecs.containsKey(name);
    }

    /**
     * Return all registered codecs.
     * @return an unmodifiable map of codecs
     */
    public Map<String, NettyCodec> getAllCodecs() {
        return Collections.unmodifiableMap(this.codecs);
    }

}
