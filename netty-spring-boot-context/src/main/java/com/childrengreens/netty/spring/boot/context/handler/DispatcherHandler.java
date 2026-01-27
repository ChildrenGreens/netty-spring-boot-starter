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

import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.codec.NettyCodec;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.dispatch.Dispatcher;
import com.childrengreens.netty.spring.boot.context.message.InboundMessage;
import com.childrengreens.netty.spring.boot.context.message.OutboundMessage;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import com.childrengreens.netty.spring.boot.context.properties.TransportType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Channel handler for dispatching inbound messages to handler methods.
 *
 * <p>This handler converts incoming data to {@link InboundMessage} instances
 * and delegates to the {@link Dispatcher} for routing and invocation.
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 */
public class DispatcherHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(DispatcherHandler.class);

    private final Dispatcher dispatcher;
    private final ServerSpec serverSpec;
    private final CodecRegistry codecRegistry;

    /**
     * Create a new DispatcherHandler.
     * @param dispatcher the message dispatcher
     * @param serverSpec the server specification
     * @param codecRegistry the codec registry for encoding responses
     */
    public DispatcherHandler(Dispatcher dispatcher, ServerSpec serverSpec, CodecRegistry codecRegistry) {
        this.dispatcher = dispatcher;
        this.serverSpec = serverSpec;
        this.codecRegistry = codecRegistry;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Store server name in channel attributes
        ctx.channel().attr(NettyContext.SERVER_NAME_KEY).set(serverSpec.getName());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        NettyContext context = new NettyContext(ctx.channel());
        InboundMessage inbound;

        if (msg instanceof FullHttpRequest) {
            inbound = convertHttpRequest((FullHttpRequest) msg);
        } else if (msg instanceof ByteBuf) {
            inbound = convertByteBuf((ByteBuf) msg);
        } else if (msg instanceof String) {
            inbound = convertString((String) msg);
        } else {
            logger.warn("Unsupported message type: {}", msg.getClass());
            return;
        }

        dispatcher.dispatch(inbound, context)
                .thenAccept(outbound -> writeResponse(ctx, msg, outbound))
                .exceptionally(ex -> {
                    logger.error("Error processing request", ex);
                    writeErrorResponse(ctx, msg, ex);
                    return null;
                });
    }

    /**
     * Convert FullHttpRequest to InboundMessage.
     */
    private InboundMessage convertHttpRequest(FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        String path = decoder.path();
        String method = request.method().name();

        Map<String, Object> headers = new HashMap<>();
        headers.put("httpMethod", method);
        headers.put("uri", request.uri());

        // Extract query parameters
        Map<String, String> queryParams = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : decoder.parameters().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                queryParams.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        headers.put("queryParams", queryParams);

        // Extract HTTP headers
        for (Map.Entry<String, String> header : request.headers()) {
            headers.put("header." + header.getKey().toLowerCase(), header.getValue());
        }

        // Extract body
        byte[] body = null;
        if (request.content().readableBytes() > 0) {
            body = new byte[request.content().readableBytes()];
            request.content().readBytes(body);
        }

        return InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey(path)
                .headers(headers)
                .rawPayload(body)
                .build();
    }

    /**
     * Convert ByteBuf to InboundMessage (for TCP).
     */
    private InboundMessage convertByteBuf(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);

        // Try to extract type from JSON for MESSAGE_TYPE routing
        String routeKey = extractTypeFromJson(bytes);

        return InboundMessage.builder()
                .transport(TransportType.TCP)
                .routeKey(routeKey)
                .rawPayload(bytes)
                .build();
    }

    /**
     * Convert String to InboundMessage (for line-based protocols).
     */
    private InboundMessage convertString(String msg) {
        return InboundMessage.builder()
                .transport(TransportType.TCP)
                .routeKey(msg.split("\\s+")[0]) // First word as route key
                .payload(msg)
                .build();
    }

    /**
     * Extract type field from JSON bytes.
     */
    private String extractTypeFromJson(byte[] bytes) {
        try {
            String json = new String(bytes, StandardCharsets.UTF_8);
            // Simple extraction - a more robust implementation would use Jackson
            int typeIndex = json.indexOf("\"type\"");
            if (typeIndex == -1) {
                typeIndex = json.indexOf("\"cmd\"");
            }
            if (typeIndex != -1) {
                int colonIndex = json.indexOf(':', typeIndex);
                int quoteStart = json.indexOf('"', colonIndex);
                int quoteEnd = json.indexOf('"', quoteStart + 1);
                if (quoteStart != -1 && quoteEnd != -1) {
                    return json.substring(quoteStart + 1, quoteEnd);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract type from JSON", e);
        }
        return "unknown";
    }

    /**
     * Write response back to the client.
     */
    private void writeResponse(ChannelHandlerContext ctx, Object originalMsg, OutboundMessage outbound) {
        if (outbound == null) {
            return;
        }

        if (originalMsg instanceof FullHttpRequest) {
            writeHttpResponse(ctx, (FullHttpRequest) originalMsg, outbound);
        } else {
            writeTcpResponse(ctx, outbound);
        }
    }

    /**
     * Write HTTP response.
     */
    private void writeHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request,
                                    OutboundMessage outbound) {
        byte[] content;
        try {
            if (outbound.getPayload() instanceof String) {
                content = ((String) outbound.getPayload()).getBytes(StandardCharsets.UTF_8);
            } else if (outbound.getPayload() instanceof byte[]) {
                content = (byte[]) outbound.getPayload();
            } else {
                // Serialize to JSON (simplified - should use codec)
                content = serializeToJson(outbound.getPayload());
            }
        } catch (Exception e) {
            logger.error("Failed to serialize response", e);
            content = ("{\"error\":\"" + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        ByteBuf buffer = ctx.alloc().buffer(content.length);
        buffer.writeBytes(content);

        HttpResponseStatus status = HttpResponseStatus.valueOf(outbound.getStatusCode());
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, buffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);

        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
            ctx.writeAndFlush(response);
        } else {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * Write TCP response.
     */
    private void writeTcpResponse(ChannelHandlerContext ctx, OutboundMessage outbound) {
        try {
            byte[] content;
            if (outbound.getPayload() instanceof byte[]) {
                content = (byte[]) outbound.getPayload();
            } else {
                content = serializeToJson(outbound.getPayload());
            }

            ByteBuf buffer = ctx.alloc().buffer(content.length);
            buffer.writeBytes(content);
            ctx.writeAndFlush(buffer);
        } catch (Exception e) {
            logger.error("Failed to write TCP response", e);
        }
    }

    /**
     * Write error response.
     */
    private void writeErrorResponse(ChannelHandlerContext ctx, Object originalMsg, Throwable ex) {
        OutboundMessage error = OutboundMessage.error(500, ex.getMessage());
        writeResponse(ctx, originalMsg, error);
    }

    /**
     * Serialize object to JSON bytes using the codec registry.
     */
    private byte[] serializeToJson(Object obj) {
        if (obj == null) {
            return "null".getBytes(StandardCharsets.UTF_8);
        }
        NettyCodec codec = codecRegistry.getDefaultCodec();
        if (codec != null) {
            try {
                return codec.encode(obj);
            } catch (Exception e) {
                logger.error("Failed to serialize object using codec", e);
            }
        }
        // Fallback to toString if no codec available
        return obj.toString().getBytes(StandardCharsets.UTF_8);
    }

}
