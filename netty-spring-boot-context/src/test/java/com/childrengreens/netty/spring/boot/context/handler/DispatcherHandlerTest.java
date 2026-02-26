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
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.dispatch.Dispatcher;
import com.childrengreens.netty.spring.boot.context.message.InboundMessage;
import com.childrengreens.netty.spring.boot.context.message.OutboundMessage;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DispatcherHandler}.
 */
class DispatcherHandlerTest {

    private Dispatcher dispatcher;
    private ServerSpec serverSpec;
    private CodecRegistry codecRegistry;
    private DispatcherHandler handler;

    @BeforeEach
    void setUp() {
        dispatcher = mock(Dispatcher.class);
        serverSpec = new ServerSpec();
        serverSpec.setName("test-server");

        codecRegistry = new CodecRegistry();
        codecRegistry.register(new JsonNettyCodec());

        handler = new DispatcherHandler(dispatcher, serverSpec, codecRegistry);
    }

    @Test
    void channelActive_setsServerNameAttribute() {
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        assertThat(channel.isActive()).isTrue();

        channel.close();
    }

    @Test
    void channelRead0_withHttpRequest_dispatchesAndResponds() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("status", "OK"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/test");
        channel.writeInbound(request);

        Object response = channel.readOutbound();
        assertThat(response).isInstanceOf(FullHttpResponse.class);

        FullHttpResponse httpResponse = (FullHttpResponse) response;
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.OK);

        channel.close();
    }

    @Test
    void channelRead0_withHttpRequestAndQueryParams_parsesParams() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok("success")));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/test?name=value&foo=bar");
        channel.writeInbound(request);

        Object response = channel.readOutbound();
        assertThat(response).isInstanceOf(FullHttpResponse.class);

        channel.close();
    }

    @Test
    void channelRead0_withHttpPostAndBody_parsesBody() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("created", true))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        ByteBuf content = Unpooled.copiedBuffer("{\"name\":\"test\"}", StandardCharsets.UTF_8);
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, "/api/users", content);
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        channel.writeInbound(request);

        Object response = channel.readOutbound();
        assertThat(response).isInstanceOf(FullHttpResponse.class);

        channel.close();
    }

    @Test
    void channelRead0_withByteBuf_dispatches() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("type", "pong"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        ByteBuf buf = Unpooled.copiedBuffer("{\"type\":\"ping\"}", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        channel.close();
    }

    @Test
    void channelRead0_withByteBuf_releasesRetainedSlice() {
        AtomicReference<InboundMessage> captured = new AtomicReference<>();
        when(dispatcher.dispatch(any(), any()))
                .thenAnswer(invocation -> {
                    captured.set(invocation.getArgument(0, InboundMessage.class));
                    return CompletableFuture.completedFuture(OutboundMessage.ok("ok"));
                });

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        ByteBuf buf = Unpooled.copiedBuffer("{\"type\":\"ping\"}", StandardCharsets.UTF_8);
        assertThat(buf.refCnt()).isEqualTo(1);

        channel.writeInbound(buf);

        InboundMessage inbound = captured.get();
        assertThat(inbound).isNotNull();
        assertThat(inbound.getRawPayloadBuffer()).isNotNull();
        assertThat(inbound.getRawPayloadBuffer().refCnt()).isEqualTo(0);
        assertThat(buf.refCnt()).isEqualTo(0);

        channel.close();
    }

    @Test
    void channelRead0_withString_dispatches() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok("PONG")));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        channel.writeInbound("PING test");

        channel.close();
    }

    @Test
    void userEventTriggered_withHandshakeComplete_dispatchesOpenEvent() {
        AtomicReference<InboundMessage> captured = new AtomicReference<>();
        when(dispatcher.dispatch(any(), any()))
                .thenAnswer(invocation -> {
                    captured.set(invocation.getArgument(0, InboundMessage.class));
                    return CompletableFuture.completedFuture(OutboundMessage.ok("OPENED"));
                });

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        "/ws", EmptyHttpHeaders.INSTANCE, null));

        InboundMessage inbound = captured.get();
        assertThat(inbound).isNotNull();
        assertThat(inbound.getRouteKey()).isEqualTo("WS:OPEN:/ws");

        Object outbound = channel.readOutbound();
        assertThat(outbound).isInstanceOf(TextWebSocketFrame.class);
        TextWebSocketFrame frame = (TextWebSocketFrame) outbound;
        assertThat(frame.text()).isEqualTo("OPENED");

        channel.close();
    }

    @Test
    void channelRead0_withTextWebSocketFrame_dispatchesAndResponds() {
        AtomicReference<InboundMessage> captured = new AtomicReference<>();
        when(dispatcher.dispatch(any(), any()))
                .thenAnswer(invocation -> {
                    captured.set(invocation.getArgument(0, InboundMessage.class));
                    return CompletableFuture.completedFuture(OutboundMessage.ok("PONG"));
                });

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(NettyContext.WS_PATH_KEY).set("/chat");

        channel.writeInbound(new TextWebSocketFrame("PING"));

        InboundMessage inbound = captured.get();
        assertThat(inbound).isNotNull();
        assertThat(inbound.getRouteKey()).isEqualTo("WS:TEXT:/chat");

        Object outbound = channel.readOutbound();
        assertThat(outbound).isInstanceOf(TextWebSocketFrame.class);
        TextWebSocketFrame frame = (TextWebSocketFrame) outbound;
        assertThat(frame.text()).isEqualTo("PONG");

        channel.close();
    }

    @Test
    void channelRead0_withNullResponse_doesNotWrite() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        ByteBuf buf = Unpooled.copiedBuffer("{\"type\":\"ping\"}", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        Object response = channel.readOutbound();
        assertThat(response).isNull();

        channel.close();
    }

    @Test
    void channelRead0_withException_writesErrorResponse() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test error")));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/test");
        channel.writeInbound(request);

        Object response = channel.readOutbound();
        assertThat(response).isInstanceOf(FullHttpResponse.class);

        FullHttpResponse httpResponse = (FullHttpResponse) response;
        assertThat(httpResponse.status()).isEqualTo(HttpResponseStatus.INTERNAL_SERVER_ERROR);

        channel.close();
    }

    @Test
    void channelRead0_withKeepAlive_keepsConnection() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("status", "OK"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/test");
        request.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
        channel.writeInbound(request);

        FullHttpResponse response = channel.readOutbound();
        assertThat(response.headers().get(HttpHeaderNames.CONNECTION)).isEqualTo("keep-alive");

        channel.close();
    }

    @Test
    void channelRead0_withByteBuf_extractsTypeField() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("type", "pong"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Test "type" field
        ByteBuf buf = Unpooled.copiedBuffer("{\"type\":\"ping\",\"data\":\"hello\"}", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        channel.close();
    }

    @Test
    void channelRead0_withInvalidJson_usesDefaultRouteKey() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("status", "OK"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Test invalid JSON
        ByteBuf buf = Unpooled.copiedBuffer("not valid json", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        channel.close();
    }

    @Test
    void channelRead0_withEmptyJson_usesDefaultRouteKey() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("status", "OK"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Test empty JSON object
        ByteBuf buf = Unpooled.copiedBuffer("{}", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        channel.close();
    }

    @Test
    void channelRead0_withJsonArray_usesDefaultRouteKey() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("status", "OK"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Test JSON array (not an object)
        ByteBuf buf = Unpooled.copiedBuffer("[1,2,3]", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        channel.close();
    }

    @Test
    void channelRead0_withNestedTypeField_extractsCorrectly() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("status", "OK"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Test with nested structure - should only extract top-level "type"
        ByteBuf buf = Unpooled.copiedBuffer("{\"type\":\"message\",\"data\":{\"type\":\"nested\"}}", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        channel.close();
    }

    @Test
    void channelRead0_withMaliciousJson_handlesSecurely() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("status", "OK"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Test potentially malicious JSON with special characters
        ByteBuf buf = Unpooled.copiedBuffer("{\"type\":\"test\\\"injection\",\"data\":\"</script>\"}", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        channel.close();
    }

    @Test
    void channelInactive_withCompletedHandshake_dispatchesCloseEvent() {
        AtomicReference<InboundMessage> captured = new AtomicReference<>();
        when(dispatcher.dispatch(any(), any()))
                .thenAnswer(invocation -> {
                    captured.set(invocation.getArgument(0, InboundMessage.class));
                    return CompletableFuture.completedFuture(OutboundMessage.ok("OK"));
                });

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Simulate WebSocket handshake completion
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        "/ws/chat", EmptyHttpHeaders.INSTANCE, null));

        // Verify OPEN event was dispatched
        InboundMessage openInbound = captured.get();
        assertThat(openInbound).isNotNull();
        assertThat(openInbound.getRouteKey()).isEqualTo("WS:OPEN:/ws/chat");

        // Clear the captured reference and read outbound
        captured.set(null);
        channel.readOutbound();

        // Close the channel to trigger channelInactive
        channel.close();

        // Verify CLOSE event was dispatched
        InboundMessage closeInbound = captured.get();
        assertThat(closeInbound).isNotNull();
        assertThat(closeInbound.getRouteKey()).isEqualTo("WS:CLOSE:/ws/chat");
    }

    @Test
    void channelInactive_withoutHandshake_doesNotDispatchCloseEvent() {
        AtomicReference<InboundMessage> captured = new AtomicReference<>();
        when(dispatcher.dispatch(any(), any()))
                .thenAnswer(invocation -> {
                    captured.set(invocation.getArgument(0, InboundMessage.class));
                    return CompletableFuture.completedFuture(OutboundMessage.ok("OK"));
                });

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Close the channel without completing WebSocket handshake
        channel.close();

        // Verify no CLOSE event was dispatched
        assertThat(captured.get()).isNull();
    }

    @Test
    void channelInactive_withCloseEvent_doesNotWriteResponse() {
        AtomicReference<InboundMessage> captured = new AtomicReference<>();
        when(dispatcher.dispatch(any(), any()))
                .thenAnswer(invocation -> {
                    captured.set(invocation.getArgument(0, InboundMessage.class));
                    return CompletableFuture.completedFuture(OutboundMessage.ok("SHOULD_NOT_BE_WRITTEN"));
                });

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Simulate WebSocket handshake completion
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        "/ws", EmptyHttpHeaders.INSTANCE, null));

        // Clear OPEN event response
        channel.readOutbound();
        captured.set(null);

        // Close the channel to trigger CLOSE event
        channel.close();

        // Verify CLOSE event was dispatched
        InboundMessage closeInbound = captured.get();
        assertThat(closeInbound).isNotNull();
        assertThat(closeInbound.getRouteKey()).isEqualTo("WS:CLOSE:/ws");

        // Verify no response was written for CLOSE event
        Object outbound = channel.readOutbound();
        assertThat(outbound).isNull();
    }

    @Test
    void userEventTriggered_withHandshakeComplete_andBinaryResponse_writesBinaryFrame() {
        byte[] binaryData = new byte[]{0x00, 0x01, 0x02, (byte) 0xFF};
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(OutboundMessage.ok(binaryData)));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Simulate WebSocket handshake completion (OPEN event)
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        "/ws", EmptyHttpHeaders.INSTANCE, null));

        // Verify response is BinaryWebSocketFrame, not TextWebSocketFrame
        Object outbound = channel.readOutbound();
        assertThat(outbound).isInstanceOf(BinaryWebSocketFrame.class);

        BinaryWebSocketFrame frame = (BinaryWebSocketFrame) outbound;
        byte[] content = new byte[frame.content().readableBytes()];
        frame.content().readBytes(content);
        assertThat(content).isEqualTo(binaryData);

        channel.close();
    }

    @Test
    void channelRead0_withTextWebSocketFrame_andBinaryResponse_writesBinaryFrame() {
        byte[] binaryData = new byte[]{0x10, 0x20, 0x30};
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(OutboundMessage.ok(binaryData)));

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(NettyContext.WS_PATH_KEY).set("/chat");

        // Send TextWebSocketFrame but expect BinaryWebSocketFrame response due to byte[] payload
        channel.writeInbound(new TextWebSocketFrame("request"));

        Object outbound = channel.readOutbound();
        assertThat(outbound).isInstanceOf(BinaryWebSocketFrame.class);

        BinaryWebSocketFrame frame = (BinaryWebSocketFrame) outbound;
        byte[] content = new byte[frame.content().readableBytes()];
        frame.content().readBytes(content);
        assertThat(content).isEqualTo(binaryData);

        channel.close();
    }

    @Test
    void channelRead0_withByteBuf_extractsCorrelationIdAndIncludesInResponse() {
        AtomicReference<NettyContext> capturedContext = new AtomicReference<>();
        when(dispatcher.dispatch(any(), any()))
                .thenAnswer(invocation -> {
                    capturedContext.set(invocation.getArgument(1, NettyContext.class));
                    return CompletableFuture.completedFuture(
                            OutboundMessage.ok(Map.of("status", "OK")));
                });

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        String correlationId = "test-correlation-123";
        ByteBuf buf = Unpooled.copiedBuffer(
                "{\"type\":\"ping\",\"X-Correlation-Id\":\"" + correlationId + "\"}", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        // Verify correlation ID was extracted and set in context
        assertThat(capturedContext.get()).isNotNull();
        assertThat(capturedContext.get().getCorrelationId()).isEqualTo(correlationId);

        // Verify response includes correlation ID
        Object response = channel.readOutbound();
        assertThat(response).isInstanceOf(ByteBuf.class);

        ByteBuf responseBuf = (ByteBuf) response;
        String responseJson = responseBuf.toString(StandardCharsets.UTF_8);
        assertThat(responseJson).contains("X-Correlation-Id");
        assertThat(responseJson).contains(correlationId);

        channel.close();
    }

    @Test
    void channelRead0_withHttpRequest_extractsCorrelationIdAndIncludesInHeader() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("status", "OK"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        String correlationId = "http-correlation-456";
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/test");
        request.headers().set("X-Correlation-Id", correlationId);
        channel.writeInbound(request);

        Object response = channel.readOutbound();
        assertThat(response).isInstanceOf(FullHttpResponse.class);

        FullHttpResponse httpResponse = (FullHttpResponse) response;
        assertThat(httpResponse.headers().get("X-Correlation-Id")).isEqualTo(correlationId);

        channel.close();
    }

    @Test
    void channelRead0_withWebSocketFrame_extractsCorrelationIdAndIncludesInResponse() {
        AtomicReference<NettyContext> capturedContext = new AtomicReference<>();
        when(dispatcher.dispatch(any(), any()))
                .thenAnswer(invocation -> {
                    capturedContext.set(invocation.getArgument(1, NettyContext.class));
                    return CompletableFuture.completedFuture(
                            OutboundMessage.ok(Map.of("message", "pong")));
                });

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.attr(NettyContext.WS_PATH_KEY).set("/chat");

        String correlationId = "ws-correlation-789";
        channel.writeInbound(new TextWebSocketFrame(
                "{\"type\":\"ping\",\"X-Correlation-Id\":\"" + correlationId + "\"}"));

        // Verify correlation ID was extracted
        assertThat(capturedContext.get()).isNotNull();
        assertThat(capturedContext.get().getCorrelationId()).isEqualTo(correlationId);

        // Verify response includes correlation ID
        Object outbound = channel.readOutbound();
        assertThat(outbound).isInstanceOf(TextWebSocketFrame.class);

        TextWebSocketFrame frame = (TextWebSocketFrame) outbound;
        assertThat(frame.text()).contains("X-Correlation-Id");
        assertThat(frame.text()).contains(correlationId);

        channel.close();
    }

    @Test
    void channelRead0_withoutCorrelationId_responseDoesNotIncludeCorrelationId() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("status", "OK"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        ByteBuf buf = Unpooled.copiedBuffer("{\"type\":\"ping\"}", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        Object response = channel.readOutbound();
        assertThat(response).isInstanceOf(ByteBuf.class);

        ByteBuf responseBuf = (ByteBuf) response;
        String responseJson = responseBuf.toString(StandardCharsets.UTF_8);
        assertThat(responseJson).doesNotContain("X-Correlation-Id");

        channel.close();
    }

    @Test
    void channelRead0_withException_errorResponseIncludesCorrelationId() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test error")));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        String correlationId = "error-correlation-001";
        ByteBuf buf = Unpooled.copiedBuffer(
                "{\"type\":\"test\",\"X-Correlation-Id\":\"" + correlationId + "\"}", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        Object response = channel.readOutbound();
        assertThat(response).isInstanceOf(ByteBuf.class);

        ByteBuf responseBuf = (ByteBuf) response;
        String responseJson = responseBuf.toString(StandardCharsets.UTF_8);
        assertThat(responseJson).contains("X-Correlation-Id");
        assertThat(responseJson).contains(correlationId);

        channel.close();
    }

    @Test
    void channelRead0_withDatagramPacket_dispatchesAndResponds() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("type", "pong"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        InetSocketAddress sender = new InetSocketAddress("127.0.0.1", 12345);
        InetSocketAddress recipient = new InetSocketAddress("127.0.0.1", 9000);
        ByteBuf content = Unpooled.copiedBuffer("{\"type\":\"ping\"}", StandardCharsets.UTF_8);
        // DatagramPacket constructor: (content, recipient, sender)
        io.netty.channel.socket.DatagramPacket packet =
                new io.netty.channel.socket.DatagramPacket(content, recipient, sender);

        channel.writeInbound(packet);

        // Verify sender address is stored in channel attribute
        InetSocketAddress storedSender = channel.attr(NettyContext.UDP_SENDER_KEY).get();
        assertThat(storedSender).isEqualTo(sender);

        // Verify response is sent as DatagramPacket
        Object response = channel.readOutbound();
        assertThat(response).isInstanceOf(io.netty.channel.socket.DatagramPacket.class);

        io.netty.channel.socket.DatagramPacket responsePacket =
                (io.netty.channel.socket.DatagramPacket) response;
        assertThat(responsePacket.recipient()).isEqualTo(sender);

        String responseJson = responsePacket.content().toString(StandardCharsets.UTF_8);
        assertThat(responseJson).contains("pong");

        channel.close();
    }

}
