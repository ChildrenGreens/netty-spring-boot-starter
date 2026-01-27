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

package com.childrengreens.netty.spring.boot.context.context;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.util.Attribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link NettyContext}.
 */
class NettyContextTest {

    private Channel channel;
    private NettyContext context;

    @BeforeEach
    void setUp() {
        channel = mock(Channel.class);
        ChannelId channelId = mock(ChannelId.class);
        when(channel.id()).thenReturn(channelId);
        when(channelId.asShortText()).thenReturn("abc123");
        context = new NettyContext(channel);
    }

    @Test
    void getChannel_returnsChannel() {
        assertThat(context.getChannel()).isSameAs(channel);
    }

    @Test
    void getChannelId_returnsShortText() {
        assertThat(context.getChannelId()).isEqualTo("abc123");
    }

    @Test
    void getRemoteAddress_returnsAddress() {
        InetSocketAddress address = new InetSocketAddress("192.168.1.1", 8080);
        when(channel.remoteAddress()).thenReturn(address);

        assertThat(context.getRemoteAddress()).isEqualTo(address);
    }

    @Test
    void getRemoteIp_returnsIpString() {
        InetSocketAddress address = new InetSocketAddress("192.168.1.1", 8080);
        when(channel.remoteAddress()).thenReturn(address);

        assertThat(context.getRemoteIp()).isEqualTo("192.168.1.1");
    }

    @Test
    void getRemoteIp_withNullAddress_returnsNull() {
        when(channel.remoteAddress()).thenReturn(null);

        assertThat(context.getRemoteIp()).isNull();
    }

    @Test
    void getServerName_returnsValueFromChannel() {
        @SuppressWarnings("unchecked")
        Attribute<String> attr = mock(Attribute.class);
        when(channel.attr(NettyContext.SERVER_NAME_KEY)).thenReturn(attr);
        when(attr.get()).thenReturn("myServer");

        assertThat(context.getServerName()).isEqualTo("myServer");
    }

    @Test
    void getWsPath_returnsValueFromChannel() {
        @SuppressWarnings("unchecked")
        Attribute<String> attr = mock(Attribute.class);
        when(channel.attr(NettyContext.WS_PATH_KEY)).thenReturn(attr);
        when(attr.get()).thenReturn("/ws/chat");

        assertThat(context.getWsPath()).isEqualTo("/ws/chat");
    }

    @Test
    void getTraceId_returnsValueFromChannel() {
        @SuppressWarnings("unchecked")
        Attribute<String> attr = mock(Attribute.class);
        when(channel.attr(NettyContext.TRACE_ID_KEY)).thenReturn(attr);
        when(attr.get()).thenReturn("trace-123");

        assertThat(context.getTraceId()).isEqualTo("trace-123");
    }

    @Test
    void setTraceId_setsValueOnChannel() {
        @SuppressWarnings("unchecked")
        Attribute<String> attr = mock(Attribute.class);
        when(channel.attr(NettyContext.TRACE_ID_KEY)).thenReturn(attr);

        context.setTraceId("new-trace");

        verify(attr).set("new-trace");
    }

    @Test
    void getAttribute_returnsStoredValue() {
        context.setAttribute("key", "value");

        String value = context.getAttribute("key");

        assertThat(value).isEqualTo("value");
    }

    @Test
    void getAttribute_withMissingKey_returnsNull() {
        Object value = context.getAttribute("nonexistent");

        assertThat(value).isNull();
    }

    @Test
    void removeAttribute_removesAndReturnsValue() {
        context.setAttribute("key", "value");

        String removed = context.removeAttribute("key");

        assertThat(removed).isEqualTo("value");
        assertThat((Object) context.getAttribute("key")).isNull();
    }

    @Test
    void isActive_delegatesToChannel() {
        when(channel.isActive()).thenReturn(true);

        assertThat(context.isActive()).isTrue();
    }

    @Test
    void close_closesChannel() {
        context.close();

        verify(channel).close();
    }

    @Test
    void attributeKeys_areNotNull() {
        assertThat((Object) NettyContext.SERVER_NAME_KEY).isNotNull();
        assertThat((Object) NettyContext.WS_PATH_KEY).isNotNull();
        assertThat((Object) NettyContext.TRACE_ID_KEY).isNotNull();
    }
}
