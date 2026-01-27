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

package com.childrengreens.netty.spring.boot.context.transport;

import com.childrengreens.netty.spring.boot.context.properties.TransportImpl;
import com.childrengreens.netty.spring.boot.context.properties.TransportType;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TransportFactory}.
 */
class TransportFactoryTest {

    @Test
    void constructor_withAuto_resolvesAppropriateTransport() {
        TransportFactory factory = new TransportFactory(TransportImpl.AUTO);

        TransportImpl resolved = factory.getResolvedTransport();

        // Should resolve to either EPOLL, KQUEUE, or NIO based on platform
        assertThat(resolved).isIn(TransportImpl.EPOLL, TransportImpl.KQUEUE, TransportImpl.NIO);
    }

    @Test
    void constructor_withNio_usesNio() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        assertThat(factory.getResolvedTransport()).isEqualTo(TransportImpl.NIO);
    }

    @Test
    void createBossGroup_returnsEventLoopGroup() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        EventLoopGroup group = factory.createBossGroup(1);

        assertThat(group).isNotNull();
        group.shutdownGracefully();
    }

    @Test
    void createWorkerGroup_returnsEventLoopGroup() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        EventLoopGroup group = factory.createWorkerGroup(2);

        assertThat(group).isNotNull();
        group.shutdownGracefully();
    }

    @Test
    void getServerChannelClass_returnsCorrectClass() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        Class<? extends ServerChannel> channelClass = factory.getServerChannelClass();

        assertThat(channelClass).isNotNull();
    }

    @Test
    void getTransportStarter_forTcp_returnsTcpStarter() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        TransportStarter starter = factory.getTransportStarter(TransportType.TCP);

        assertThat(starter).isNotNull();
        assertThat(starter).isInstanceOf(TcpTransportStarter.class);
    }

    @Test
    void getTransportStarter_forHttp_returnsTcpStarter() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        TransportStarter starter = factory.getTransportStarter(TransportType.HTTP);

        assertThat(starter).isNotNull();
        assertThat(starter).isInstanceOf(TcpTransportStarter.class);
    }

    @Test
    void getTransportStarter_forUdp_returnsUdpStarter() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        TransportStarter starter = factory.getTransportStarter(TransportType.UDP);

        assertThat(starter).isNotNull();
        assertThat(starter).isInstanceOf(UdpTransportStarter.class);
    }
}
