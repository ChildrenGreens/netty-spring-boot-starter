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

import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * Profile for WebSocket connections.
 *
 * <p>This profile configures:
 * <ul>
 * <li>{@link HttpServerCodec} for HTTP upgrade handling</li>
 * <li>{@link HttpObjectAggregator} for combining HTTP message parts</li>
 * <li>WebSocket frame handlers added after upgrade</li>
 * </ul>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class WebSocketProfile implements Profile {

    /**
     * Profile name constant.
     */
    public static final String NAME = "websocket";

    private static final int MAX_CONTENT_LENGTH = 64 * 1024; // 64KB for handshake
    private static final int MAX_FRAME_SIZE = 64 * 1024; // 64KB for frames

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        // HTTP codec for WebSocket handshake
        pipeline.addLast("httpCodec", new HttpServerCodec());

        // Aggregator for handshake request
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));

        // WebSocket upgrade + frame handling
        WebSocketServerProtocolConfig config = WebSocketServerProtocolConfig.newBuilder()
                .websocketPath("/")
                .checkStartsWith(true)
                .maxFramePayloadLength(MAX_FRAME_SIZE)
                .build();
        pipeline.addLast("wsProtocol", new WebSocketServerProtocolHandler(config));

        // Aggregator for fragmented WebSocket frames
        pipeline.addLast("wsAggregator", new WebSocketFrameAggregator(MAX_FRAME_SIZE));
    }

    @Override
    public String getProtocolType() {
        return NettyContext.PROTOCOL_WEBSOCKET;
    }

}
