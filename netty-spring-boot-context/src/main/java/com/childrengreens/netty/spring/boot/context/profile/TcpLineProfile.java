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
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;

/**
 * Profile for TCP with line-based framing.
 *
 * <p>This profile configures:
 * <ul>
 * <li>{@link DelimiterBasedFrameDecoder} with line delimiters</li>
 * <li>{@link StringDecoder} for converting bytes to strings</li>
 * <li>{@link StringEncoder} for converting strings to bytes</li>
 * </ul>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class TcpLineProfile implements Profile {

    /**
     * Profile name constant.
     */
    public static final String NAME = "tcp-line";

    private static final int MAX_FRAME_LENGTH = 8192;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        // Line-based frame decoder
        pipeline.addLast("frameDecoder", new DelimiterBasedFrameDecoder(
                MAX_FRAME_LENGTH, Delimiters.lineDelimiter()));

        // String codecs
        pipeline.addLast("stringDecoder", new StringDecoder(StandardCharsets.UTF_8));
        pipeline.addLast("stringEncoder", new StringEncoder(StandardCharsets.UTF_8));
    }

    @Override
    public String getDefaultCodec() {
        return "string";
    }

}
