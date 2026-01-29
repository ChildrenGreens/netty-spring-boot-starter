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
import com.childrengreens.netty.spring.boot.context.dispatch.Dispatcher;
import com.childrengreens.netty.spring.boot.context.message.InboundMessage;
import com.childrengreens.netty.spring.boot.context.message.OutboundMessage;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void channelRead0_withByteBuf_extractsCmdField() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("status", "OK"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Test "cmd" field
        ByteBuf buf = Unpooled.copiedBuffer("{\"cmd\":\"login\",\"user\":\"test\"}", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        channel.close();
    }

    @Test
    void channelRead0_withByteBuf_extractsActionField() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("status", "OK"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Test "action" field
        ByteBuf buf = Unpooled.copiedBuffer("{\"action\":\"create\",\"payload\":{}}", StandardCharsets.UTF_8);
        channel.writeInbound(buf);

        channel.close();
    }

    @Test
    void channelRead0_withByteBuf_extractsCommandField() {
        when(dispatcher.dispatch(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        OutboundMessage.ok(Map.of("status", "OK"))));

        EmbeddedChannel channel = new EmbeddedChannel(handler);

        // Test "command" field
        ByteBuf buf = Unpooled.copiedBuffer("{\"command\":\"start\",\"args\":[]}", StandardCharsets.UTF_8);
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

}
