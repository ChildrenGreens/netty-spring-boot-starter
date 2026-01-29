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

package com.childrengreens.netty.spring.boot.context.handler;

import com.childrengreens.netty.spring.boot.context.codec.NettyCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Channel handler for encoding and decoding messages using a codec.
 *
 * <p>This handler sits in the pipeline after frame decoders and before
 * business logic handlers. It converts between ByteBuf and Java objects.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see NettyCodec
 */
public class JsonCodecHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(JsonCodecHandler.class);

    private final NettyCodec codec;

    /**
     * Create a new JsonCodecHandler.
     * @param codec the codec to use for encoding/decoding
     */
    public JsonCodecHandler(NettyCodec codec) {
        this.codec = codec;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            try {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);

                // Decode to Map
                @SuppressWarnings("unchecked")
                Map<String, Object> decoded = codec.decode(bytes, Map.class);

                ctx.fireChannelRead(decoded);
            } finally {
                buf.release();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Map || msg instanceof String || !(msg instanceof ByteBuf)) {
            try {
                byte[] encoded = codec.encode(msg);
                ByteBuf buf = ctx.alloc().buffer(encoded.length);
                buf.writeBytes(encoded);
                ctx.write(buf, promise);
            } catch (Exception e) {
                logger.error("Failed to encode message: {}", e.getMessage());
                promise.setFailure(e);
            }
        } else {
            ctx.write(msg, promise);
        }
    }

}
