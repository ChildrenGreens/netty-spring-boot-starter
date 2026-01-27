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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * Profile for TCP with length-field based framing and JSON codec.
 *
 * <p>This profile configures:
 * <ul>
 * <li>{@link LengthFieldBasedFrameDecoder} for frame decoding</li>
 * <li>{@link LengthFieldPrepender} for frame encoding</li>
 * <li>JSON codec for message serialization</li>
 * </ul>
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public class TcpLengthFieldJsonProfile implements Profile {

    /**
     * Profile name constant.
     */
    public static final String NAME = "tcp-lengthfield-json";

    private static final int MAX_FRAME_LENGTH = 1024 * 1024; // 1MB
    private static final int LENGTH_FIELD_OFFSET = 0;
    private static final int LENGTH_FIELD_LENGTH = 4;
    private static final int LENGTH_ADJUSTMENT = 0;
    private static final int INITIAL_BYTES_TO_STRIP = 4;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        // Frame decoder - reads length prefix and extracts frame
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                MAX_FRAME_LENGTH,
                LENGTH_FIELD_OFFSET,
                LENGTH_FIELD_LENGTH,
                LENGTH_ADJUSTMENT,
                INITIAL_BYTES_TO_STRIP
        ));

        // Frame encoder - prepends length to outgoing messages
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(LENGTH_FIELD_LENGTH));
    }

}
