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

package com.childrengreens.netty.spring.boot.context.profile;

import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Profile for HTTP/1.1 with JSON codec.
 *
 * <p>This profile configures:
 * <ul>
 * <li>{@link HttpServerCodec} for HTTP encoding/decoding</li>
 * <li>{@link HttpObjectAggregator} for combining HTTP message parts</li>
 * <li>JSON codec for request/response body serialization</li>
 * </ul>
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 */
public class Http1JsonProfile implements Profile {

    /**
     * Profile name constant.
     */
    public static final String NAME = "http1-json";

    private static final int MAX_CONTENT_LENGTH = 1024 * 1024; // 1MB

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        // HTTP codec for encoding/decoding HTTP messages
        pipeline.addLast("httpCodec", new HttpServerCodec());

        // Aggregator to combine HttpMessage and HttpContent
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));
    }

}
