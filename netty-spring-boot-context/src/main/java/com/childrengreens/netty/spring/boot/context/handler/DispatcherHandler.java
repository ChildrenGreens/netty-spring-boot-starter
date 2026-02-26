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
import com.childrengreens.netty.spring.boot.context.codec.JsonNettyCodec;
import com.childrengreens.netty.spring.boot.context.codec.NettyCodec;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.dispatch.Dispatcher;
import com.childrengreens.netty.spring.boot.context.message.InboundMessage;
import com.childrengreens.netty.spring.boot.context.message.OutboundMessage;
import com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import com.childrengreens.netty.spring.boot.context.properties.TransportType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
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
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class DispatcherHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(DispatcherHandler.class);

    /**
     * Default route key when extraction fails.
     */
    private static final String DEFAULT_ROUTE_KEY = "unknown";

    /**
     * Route key field name in JSON messages.
     */
    private static final String ROUTE_KEY_FIELD = "type";

    private final Dispatcher dispatcher;
    private final ServerSpec serverSpec;
    private final CodecRegistry codecRegistry;
    private final ObjectMapper objectMapper;
    private final ServerMetrics serverMetrics;

    private static final AttributeKey<Boolean> WS_HANDSHAKE_COMPLETED_KEY =
            AttributeKey.valueOf("netty.ws.handshake.completed");

    private enum WsFrameKind {
        TEXT,
        BINARY
    }

    /**
     * Marker event for internal WebSocket lifecycle dispatch.
     */
    private static final class WsEvent {
        private final WsFrameKind kind;
        private final boolean skipResponse;

        private WsEvent(WsFrameKind kind) {
            this(kind, false);
        }

        private WsEvent(WsFrameKind kind, boolean skipResponse) {
            this.kind = kind;
            this.skipResponse = skipResponse;
        }
    }

    /**
     * Create a new DispatcherHandler.
     * @param dispatcher the message dispatcher
     * @param serverSpec the server specification
     * @param codecRegistry the codec registry for encoding responses
     */
    public DispatcherHandler(Dispatcher dispatcher, ServerSpec serverSpec, CodecRegistry codecRegistry) {
        this(dispatcher, serverSpec, codecRegistry, null);
    }

    /**
     * Create a new DispatcherHandler with metrics.
     * @param dispatcher the message dispatcher
     * @param serverSpec the server specification
     * @param codecRegistry the codec registry for encoding responses
     * @param serverMetrics the server metrics for recording request stats (may be null)
     * @since 0.0.2
     */
    public DispatcherHandler(Dispatcher dispatcher, ServerSpec serverSpec, CodecRegistry codecRegistry,
                             ServerMetrics serverMetrics) {
        this.dispatcher = dispatcher;
        this.serverSpec = serverSpec;
        this.codecRegistry = codecRegistry;
        this.objectMapper = resolveObjectMapper(codecRegistry);
        this.serverMetrics = serverMetrics;
    }

    /**
     * Resolve ObjectMapper from CodecRegistry or create a new one.
     * @param codecRegistry the codec registry
     * @return the ObjectMapper instance
     */
    private static ObjectMapper resolveObjectMapper(CodecRegistry codecRegistry) {
        NettyCodec codec = codecRegistry.getCodec(JsonNettyCodec.NAME);
        if (codec instanceof JsonNettyCodec) {
            return ((JsonNettyCodec) codec).getObjectMapper();
        }
        // Fallback to a new ObjectMapper if no JSON codec registered
        return new ObjectMapper();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Store server name in channel attributes
        ctx.channel().attr(NettyContext.SERVER_NAME_KEY).set(serverSpec.getName());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        long startTime = System.nanoTime();
        InboundMessage inbound;

        if (msg instanceof FullHttpRequest) {
            inbound = convertHttpRequest((FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg, startTime);
            return;
        } else if (msg instanceof DatagramPacket) {
            inbound = convertDatagramPacket(ctx, (DatagramPacket) msg);
        } else if (msg instanceof ByteBuf) {
            inbound = convertByteBuf((ByteBuf) msg);
        } else if (msg instanceof String) {
            inbound = convertString((String) msg);
        } else {
            logger.warn("Unsupported message type: {}", msg.getClass());
            return;
        }

        dispatchInbound(ctx, msg, inbound, startTime);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        Boolean completed = ctx.channel().attr(WS_HANDSHAKE_COMPLETED_KEY).get();
        if (Boolean.TRUE.equals(completed)) {
            super.userEventTriggered(ctx, evt);
            return;
        }

        // Handle WebSocket handshake completion and dispatch OPEN event once.
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete complete) {
            String path = new QueryStringDecoder(complete.requestUri()).path();
            ctx.channel().attr(NettyContext.WS_PATH_KEY).set(path);
            ctx.channel().attr(WS_HANDSHAKE_COMPLETED_KEY).set(Boolean.TRUE);
            dispatchWebSocketEvent(ctx, "OPEN", path, new WsEvent(WsFrameKind.TEXT));
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Dispatch CLOSE event for WebSocket connections that completed handshake
        Boolean completed = ctx.channel().attr(WS_HANDSHAKE_COMPLETED_KEY).get();
        if (Boolean.TRUE.equals(completed)) {
            String path = ctx.channel().attr(NettyContext.WS_PATH_KEY).get();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            dispatchWebSocketEvent(ctx, "CLOSE", path,
                    new WsEvent(WsFrameKind.TEXT, true));
        }
        super.channelInactive(ctx);
    }

    /**
     * Record request metrics if metrics are available.
     * @param startTimeNanos the request start time in nanoseconds
     */
    private void recordRequestMetrics(long startTimeNanos) {
        if (serverMetrics != null) {
            long latencyNanos = System.nanoTime() - startTimeNanos;
            serverMetrics.recordRequest(latencyNanos);
        }
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
        // Try to extract type from JSON for MESSAGE_TYPE routing
        ByteBuf retained = buf.retainedSlice();
        String routeKey = extractTypeFromJson(retained);

        return InboundMessage.builder()
                .transport(TransportType.TCP)
                .routeKey(routeKey)
                .rawPayload(retained)
                .build();
    }

    /**
     * Convert DatagramPacket to InboundMessage (for UDP).
     * <p>Stores the sender address in context for response routing.
     */
    private InboundMessage convertDatagramPacket(ChannelHandlerContext ctx, DatagramPacket packet) {
        // Store sender address for response
        ctx.channel().attr(NettyContext.UDP_SENDER_KEY).set(packet.sender());

        ByteBuf content = packet.content();
        ByteBuf retained = content.retainedSlice();
        String routeKey = extractTypeFromJson(retained);

        Map<String, Object> headers = new HashMap<>();
        headers.put("udpSender", packet.sender().toString());

        return InboundMessage.builder()
                .transport(TransportType.UDP)
                .routeKey(routeKey)
                .headers(headers)
                .rawPayload(retained)
                .build();
    }

    /**
     * Handle WebSocket frames and dispatch them into the same routing flow.
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame, long startTime) {
        String path = ctx.channel().attr(NettyContext.WS_PATH_KEY).get();
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        InboundMessage inbound;
        if (frame instanceof TextWebSocketFrame textFrame) {
            String text = textFrame.text();
            byte[] raw = text.getBytes(StandardCharsets.UTF_8);
            inbound = buildWebSocketInbound("TEXT", path, text, raw, null);
            dispatchInbound(ctx, frame, inbound, startTime);
            return;
        }
        if (frame instanceof BinaryWebSocketFrame binFrame) {
            ByteBuf retained = binFrame.content().retainedSlice();
            inbound = buildWebSocketInbound("BINARY", path, null, null, retained);
            dispatchInbound(ctx, frame, inbound, startTime);
        }
    }

    /**
     * Build an inbound message for WebSocket routing.
     */
    private InboundMessage buildWebSocketInbound(String eventType, String path,
                                                 Object payload, byte[] rawPayload, ByteBuf rawPayloadBuffer) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("wsEvent", eventType);
        headers.put("wsPath", path);
        String routeKey = "WS:" + eventType + ":" + path;
        return InboundMessage.builder()
                .transport(TransportType.HTTP)
                .routeKey(routeKey)
                .headers(headers)
                .payload(payload)
                .rawPayload(rawPayload)
                .rawPayload(rawPayloadBuffer)
                .build();
    }

    /**
     * Dispatch a WebSocket lifecycle event (OPEN/CLOSE) into the dispatcher.
     */
    private void dispatchWebSocketEvent(ChannelHandlerContext ctx, String eventType, String path,
                                        Object originalMsg) {
        long startTime = System.nanoTime();
        InboundMessage inbound = buildWebSocketInbound(eventType, path, null, null, null);
        dispatchInbound(ctx, originalMsg, inbound, startTime);
    }

    /**
     * Shared dispatch path with consistent error handling and metrics.
     */
    private void dispatchInbound(ChannelHandlerContext ctx, Object originalMsg, InboundMessage inbound,
                                 long startTime) {
        NettyContext context = new NettyContext(ctx.channel());
        // Extract correlation ID from inbound message headers
        String correlationId = extractCorrelationId(inbound);
        context.setCorrelationId(correlationId);
        try {
            dispatcher.dispatch(inbound, context)
                    .thenAccept(outbound -> writeResponse(ctx, originalMsg, outbound, correlationId))
                    .exceptionally(ex -> {
                        logger.error("Error processing request", ex);
                        writeErrorResponse(ctx, originalMsg, ex, correlationId);
                        return null;
                    })
                    .whenComplete((outbound, ex) -> {
                        inbound.releaseRawPayloadBuffer();
                        recordRequestMetrics(startTime);
                    });
        } catch (Exception ex) {
            inbound.releaseRawPayloadBuffer();
            logger.error("Error processing request", ex);
            writeErrorResponse(ctx, originalMsg, ex, correlationId);
            recordRequestMetrics(startTime);
        }
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
     * Extract route key field from JSON ByteBuf using Jackson.
     *
     * <p>This method safely parses JSON and looks for the "type" field.
     * It uses {@link ByteBufInputStream} with {@link ByteBuf#duplicate()} for zero-copy JSON parsing.
     *
     * @param buf the JSON ByteBuf
     * @return the extracted route key, or {@link #DEFAULT_ROUTE_KEY} if not found
     */
    private String extractTypeFromJson(ByteBuf buf) {
        if (buf == null || !buf.isReadable()) {
            return DEFAULT_ROUTE_KEY;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(new ByteBufInputStream(buf.duplicate()));

            if (rootNode == null || !rootNode.isObject()) {
                return DEFAULT_ROUTE_KEY;
            }

            JsonNode fieldNode = rootNode.get(ROUTE_KEY_FIELD);
            if (fieldNode != null && fieldNode.isTextual()) {
                String value = fieldNode.asText();
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract route key from JSON: {}", e.getMessage());
        }

        return DEFAULT_ROUTE_KEY;
    }

    /**
     * Extract correlation ID from inbound message.
     * <p>Looks for the correlation ID in headers first, then in the raw JSON payload.
     *
     * @param inbound the inbound message
     * @return the correlation ID, or {@code null} if not found
     */
    private String extractCorrelationId(InboundMessage inbound) {
        // Check headers first (direct header)
        Object headerValue = inbound.getHeaders().get(NettyContext.CORRELATION_ID_HEADER);
        if (headerValue instanceof String && !((String) headerValue).isEmpty()) {
            return (String) headerValue;
        }

        // Check HTTP headers (stored with "header." prefix and lowercase)
        Object httpHeaderValue = inbound.getHeaders().get("header." + NettyContext.CORRELATION_ID_HEADER.toLowerCase());
        if (httpHeaderValue instanceof String && !((String) httpHeaderValue).isEmpty()) {
            return (String) httpHeaderValue;
        }

        // Try to extract from raw payload buffer (JSON)
        ByteBuf buf = inbound.getRawPayloadBuffer();
        if (buf != null && buf.isReadable()) {
            String extracted = extractCorrelationIdFromJson(buf);
            if (extracted != null) {
                return extracted;
            }
        }

        // Try to extract from raw payload bytes (JSON)
        byte[] rawPayload = inbound.getRawPayload();
        if (rawPayload != null && rawPayload.length > 0) {
            String extracted = extractCorrelationIdFromJsonBytes(rawPayload);
            if (extracted != null) {
                return extracted;
            }
        }

        // Try to extract from payload if it's a String (WebSocket text)
        Object payload = inbound.getPayload();
        if (payload instanceof String) {
            return extractCorrelationIdFromJsonString((String) payload);
        }

        return null;
    }

    /**
     * Extract correlation ID from JSON ByteBuf.
     */
    private String extractCorrelationIdFromJson(ByteBuf buf) {
        try {
            JsonNode rootNode = objectMapper.readTree(new ByteBufInputStream(buf.duplicate()));
            if (rootNode != null && rootNode.isObject()) {
                JsonNode correlationNode = rootNode.get(NettyContext.CORRELATION_ID_HEADER);
                if (correlationNode != null && correlationNode.isTextual()) {
                    return correlationNode.asText();
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract correlation ID from JSON: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract correlation ID from JSON bytes.
     */
    private String extractCorrelationIdFromJsonBytes(byte[] bytes) {
        try {
            JsonNode rootNode = objectMapper.readTree(bytes);
            if (rootNode != null && rootNode.isObject()) {
                JsonNode correlationNode = rootNode.get(NettyContext.CORRELATION_ID_HEADER);
                if (correlationNode != null && correlationNode.isTextual()) {
                    return correlationNode.asText();
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract correlation ID from JSON bytes: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract correlation ID from JSON string.
     */
    private String extractCorrelationIdFromJsonString(String json) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            if (rootNode != null && rootNode.isObject()) {
                JsonNode correlationNode = rootNode.get(NettyContext.CORRELATION_ID_HEADER);
                if (correlationNode != null && correlationNode.isTextual()) {
                    return correlationNode.asText();
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract correlation ID from JSON string: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Write response back to the client.
     */
    private void writeResponse(ChannelHandlerContext ctx, Object originalMsg, OutboundMessage outbound,
                               String correlationId) {
        if (outbound == null) {
            return;
        }

        // Skip response for events that should not write (e.g., CLOSE on inactive channel)
        if (originalMsg instanceof WsEvent && ((WsEvent) originalMsg).skipResponse) {
            return;
        }

        if (originalMsg instanceof FullHttpRequest) {
            writeHttpResponse(ctx, (FullHttpRequest) originalMsg, outbound, correlationId);
        } else if (originalMsg instanceof WebSocketFrame || originalMsg instanceof WsEvent) {
            writeWebSocketResponse(ctx, originalMsg, outbound, correlationId);
        } else if (originalMsg instanceof DatagramPacket) {
            writeUdpResponse(ctx, outbound, correlationId);
        } else {
            writeTcpResponse(ctx, outbound, correlationId);
        }
    }

    /**
     * Write HTTP response.
     */
    private void writeHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request,
                                    OutboundMessage outbound, String correlationId) {
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

        // Add correlation ID header if present
        if (correlationId != null) {
            response.headers().set(NettyContext.CORRELATION_ID_HEADER, correlationId);
        }

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
    private void writeTcpResponse(ChannelHandlerContext ctx, OutboundMessage outbound, String correlationId) {
        try {
            Object payload = outbound.getPayload();
            byte[] content;

            // Build response with unified format (skip for binary payloads)
            if (!(payload instanceof byte[])) {
                payload = buildResponse(payload, correlationId);
            }

            if (payload instanceof byte[]) {
                content = (byte[]) payload;
            } else {
                content = serializeToJson(payload);
            }

            ByteBuf buffer = ctx.alloc().buffer(content.length);
            buffer.writeBytes(content);
            ctx.writeAndFlush(buffer);
        } catch (Exception e) {
            logger.error("Failed to write TCP response", e);
        }
    }

    /**
     * Write UDP response.
     * <p>Sends a DatagramPacket back to the original sender.
     */
    private void writeUdpResponse(ChannelHandlerContext ctx, OutboundMessage outbound, String correlationId) {
        try {
            InetSocketAddress sender = ctx.channel().attr(NettyContext.UDP_SENDER_KEY).get();
            if (sender == null) {
                logger.warn("Cannot send UDP response: sender address not available");
                return;
            }

            Object payload = outbound.getPayload();
            byte[] content;

            // Build response with unified format (skip for binary payloads)
            if (!(payload instanceof byte[])) {
                payload = buildResponse(payload, correlationId);
            }

            if (payload instanceof byte[]) {
                content = (byte[]) payload;
            } else {
                content = serializeToJson(payload);
            }

            ByteBuf buffer = ctx.alloc().buffer(content.length);
            buffer.writeBytes(content);
            DatagramPacket responsePacket = new DatagramPacket(buffer, sender);
            ctx.writeAndFlush(responsePacket);
        } catch (Exception e) {
            logger.error("Failed to write UDP response", e);
        }
    }

    /**
     * Write response as a WebSocket frame (text/binary).
     */
    private void writeWebSocketResponse(ChannelHandlerContext ctx, Object originalMsg, OutboundMessage outbound,
                                        String correlationId) {
        WsFrameKind kind = WsFrameKind.TEXT;
        if (originalMsg instanceof BinaryWebSocketFrame) {
            kind = WsFrameKind.BINARY;
        } else if (originalMsg instanceof WsEvent) {
            kind = ((WsEvent) originalMsg).kind;
        }

        Object payload = outbound.getPayload();

        // Override frame kind based on payload type to avoid corrupting binary data
        if (payload instanceof byte[]) {
            kind = WsFrameKind.BINARY;
        }

        // Build response with unified format (skip for binary payloads)
        if (!(payload instanceof byte[])) {
            payload = buildResponse(payload, correlationId);
        }

        try {
            if (kind == WsFrameKind.BINARY) {
                byte[] content = payload instanceof byte[]
                        ? (byte[]) payload
                        : serializeToJson(payload);
                ctx.writeAndFlush(new BinaryWebSocketFrame(ctx.alloc().buffer(content.length).writeBytes(content)));
            } else {
                String text;
                if (payload instanceof String) {
                    text = (String) payload;
                } else {
                    text = new String(serializeToJson(payload), StandardCharsets.UTF_8);
                }
                ctx.writeAndFlush(new TextWebSocketFrame(text));
            }
        } catch (Exception e) {
            logger.error("Failed to write WebSocket response", e);
        }
    }

    /**
     * Write error response.
     */
    private void writeErrorResponse(ChannelHandlerContext ctx, Object originalMsg, Throwable ex,
                                    String correlationId) {
        OutboundMessage error = OutboundMessage.error(500, ex.getMessage());
        writeResponse(ctx, originalMsg, error, correlationId);
    }

    /**
     * Build response with unified format.
     * <p>Response format: {@code {"X-Correlation-Id": "...", "data": {...}}}
     *
     * @param payload the original payload
     * @param correlationId the correlation ID (may be null)
     * @return the formatted response
     */
    private Object buildResponse(Object payload, String correlationId) {
        // If no correlation ID, return payload as-is for backward compatibility
        if (correlationId == null) {
            return payload;
        }

        Map<String, Object> response = new HashMap<>();
        response.put(NettyContext.CORRELATION_ID_HEADER, correlationId);
        response.put("data", payload);
        return response;
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
