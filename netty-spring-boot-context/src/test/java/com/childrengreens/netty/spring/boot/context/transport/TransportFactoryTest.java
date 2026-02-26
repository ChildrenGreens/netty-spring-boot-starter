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

import com.childrengreens.netty.spring.boot.context.pipeline.PipelineAssembler;
import com.childrengreens.netty.spring.boot.context.properties.TransportImpl;
import com.childrengreens.netty.spring.boot.context.properties.TransportType;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

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
    void getTransportStarter_forUdp_withPipelineAssembler_returnsUdpStarter() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);
        PipelineAssembler mockAssembler = mock(PipelineAssembler.class);

        TransportStarter starter = factory.getTransportStarter(TransportType.UDP, mockAssembler);

        assertThat(starter).isNotNull();
        assertThat(starter).isInstanceOf(UdpTransportStarter.class);
    }

    @Test
    void getTransportStarter_forUdp_withoutPipelineAssembler_throwsException() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        assertThatThrownBy(() -> factory.getTransportStarter(TransportType.UDP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PipelineAssembler is required for UDP transport");
    }

    @Test
    void getClientChannelClass_returnsCorrectClass() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        var channelClass = factory.getClientChannelClass();

        assertThat(channelClass).isNotNull();
    }

    @Test
    void constructor_withEpoll_fallsBackToNioIfNotAvailable() {
        TransportFactory factory = new TransportFactory(TransportImpl.EPOLL);

        TransportImpl resolved = factory.getResolvedTransport();

        // On macOS, EPOLL is not available, so it should fall back to NIO
        // On Linux, EPOLL should be used
        assertThat(resolved).isIn(TransportImpl.EPOLL, TransportImpl.NIO);
    }

    @Test
    void constructor_withKqueue_fallsBackToNioIfNotAvailable() {
        TransportFactory factory = new TransportFactory(TransportImpl.KQUEUE);

        TransportImpl resolved = factory.getResolvedTransport();

        // On Linux, KQUEUE is not available, so it should fall back to NIO
        // On macOS, KQUEUE should be used
        assertThat(resolved).isIn(TransportImpl.KQUEUE, TransportImpl.NIO);
    }

    @Test
    void createWorkerGroup_withZeroThreads_usesDefaultThreadCount() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        EventLoopGroup group = factory.createWorkerGroup(0);

        assertThat(group).isNotNull();
        group.shutdownGracefully();
    }

    // ==================== KQUEUE-specific tests (macOS/BSD) ====================

    @Test
    void constructor_withKqueue_whenAvailable_usesKqueue() {
        assumeTrue(KQueue.isAvailable(), "KQueue not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.KQUEUE);

        assertThat(factory.getResolvedTransport()).isEqualTo(TransportImpl.KQUEUE);
    }

    @Test
    void createBossGroup_withKqueue_createsKQueueEventLoopGroup() {
        assumeTrue(KQueue.isAvailable(), "KQueue not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.KQUEUE);
        EventLoopGroup group = factory.createBossGroup(1);

        assertThat(group).isInstanceOf(KQueueEventLoopGroup.class);
        group.shutdownGracefully();
    }

    @Test
    void createWorkerGroup_withKqueue_createsKQueueEventLoopGroup() {
        assumeTrue(KQueue.isAvailable(), "KQueue not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.KQUEUE);
        EventLoopGroup group = factory.createWorkerGroup(2);

        assertThat(group).isInstanceOf(KQueueEventLoopGroup.class);
        group.shutdownGracefully();
    }

    @Test
    void getServerChannelClass_withKqueue_returnsKQueueServerSocketChannel() {
        assumeTrue(KQueue.isAvailable(), "KQueue not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.KQUEUE);

        assertThat(factory.getServerChannelClass()).isEqualTo(KQueueServerSocketChannel.class);
    }

    @Test
    void getClientChannelClass_withKqueue_returnsKQueueSocketChannel() {
        assumeTrue(KQueue.isAvailable(), "KQueue not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.KQUEUE);

        assertThat(factory.getClientChannelClass()).isEqualTo(KQueueSocketChannel.class);
    }

    @Test
    void getDatagramChannelClass_withKqueue_returnsKQueueDatagramChannel() {
        assumeTrue(KQueue.isAvailable(), "KQueue not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.KQUEUE);

        assertThat(factory.getDatagramChannelClass()).isEqualTo(KQueueDatagramChannel.class);
    }

    // ==================== EPOLL-specific tests (Linux) ====================

    @Test
    void constructor_withEpoll_whenAvailable_usesEpoll() {
        assumeTrue(Epoll.isAvailable(), "Epoll not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.EPOLL);

        assertThat(factory.getResolvedTransport()).isEqualTo(TransportImpl.EPOLL);
    }

    @Test
    void createBossGroup_withEpoll_createsEpollEventLoopGroup() {
        assumeTrue(Epoll.isAvailable(), "Epoll not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.EPOLL);
        EventLoopGroup group = factory.createBossGroup(1);

        assertThat(group).isInstanceOf(EpollEventLoopGroup.class);
        group.shutdownGracefully();
    }

    @Test
    void createWorkerGroup_withEpoll_createsEpollEventLoopGroup() {
        assumeTrue(Epoll.isAvailable(), "Epoll not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.EPOLL);
        EventLoopGroup group = factory.createWorkerGroup(2);

        assertThat(group).isInstanceOf(EpollEventLoopGroup.class);
        group.shutdownGracefully();
    }

    @Test
    void getServerChannelClass_withEpoll_returnsEpollServerSocketChannel() {
        assumeTrue(Epoll.isAvailable(), "Epoll not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.EPOLL);

        assertThat(factory.getServerChannelClass()).isEqualTo(EpollServerSocketChannel.class);
    }

    @Test
    void getClientChannelClass_withEpoll_returnsEpollSocketChannel() {
        assumeTrue(Epoll.isAvailable(), "Epoll not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.EPOLL);

        assertThat(factory.getClientChannelClass()).isEqualTo(EpollSocketChannel.class);
    }

    @Test
    void getDatagramChannelClass_withEpoll_returnsEpollDatagramChannel() {
        assumeTrue(Epoll.isAvailable(), "Epoll not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.EPOLL);

        assertThat(factory.getDatagramChannelClass()).isEqualTo(EpollDatagramChannel.class);
    }

    // ==================== NIO-specific tests ====================

    @Test
    void createBossGroup_withNio_createsNioEventLoopGroup() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);
        EventLoopGroup group = factory.createBossGroup(1);

        assertThat(group).isInstanceOf(NioEventLoopGroup.class);
        group.shutdownGracefully();
    }

    @Test
    void createWorkerGroup_withNio_createsNioEventLoopGroup() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);
        EventLoopGroup group = factory.createWorkerGroup(2);

        assertThat(group).isInstanceOf(NioEventLoopGroup.class);
        group.shutdownGracefully();
    }

    @Test
    void getServerChannelClass_withNio_returnsNioServerSocketChannel() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        assertThat(factory.getServerChannelClass()).isEqualTo(NioServerSocketChannel.class);
    }

    @Test
    void getClientChannelClass_withNio_returnsNioSocketChannel() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        assertThat(factory.getClientChannelClass()).isEqualTo(NioSocketChannel.class);
    }

    @Test
    void getDatagramChannelClass_withNio_returnsNioDatagramChannel() {
        TransportFactory factory = new TransportFactory(TransportImpl.NIO);

        assertThat(factory.getDatagramChannelClass()).isEqualTo(NioDatagramChannel.class);
    }

    // ==================== AUTO resolution tests ====================

    @Test
    void constructor_withAuto_onKqueuePlatform_resolvesToKqueue() {
        assumeTrue(KQueue.isAvailable(), "KQueue not available on this platform");
        assumeTrue(!Epoll.isAvailable(), "Epoll available, test is for KQueue-only platforms");

        TransportFactory factory = new TransportFactory(TransportImpl.AUTO);

        assertThat(factory.getResolvedTransport()).isEqualTo(TransportImpl.KQUEUE);
    }

    @Test
    void constructor_withAuto_onEpollPlatform_resolvesToEpoll() {
        assumeTrue(Epoll.isAvailable(), "Epoll not available on this platform");

        TransportFactory factory = new TransportFactory(TransportImpl.AUTO);

        // EPOLL takes priority over KQUEUE in AUTO mode
        assertThat(factory.getResolvedTransport()).isEqualTo(TransportImpl.EPOLL);
    }

    @Test
    void constructor_withAuto_createsCorrectEventLoopGroup() {
        TransportFactory factory = new TransportFactory(TransportImpl.AUTO);
        EventLoopGroup group = factory.createBossGroup(1);

        TransportImpl resolved = factory.getResolvedTransport();
        if (resolved == TransportImpl.EPOLL) {
            assertThat(group).isInstanceOf(EpollEventLoopGroup.class);
        } else if (resolved == TransportImpl.KQUEUE) {
            assertThat(group).isInstanceOf(KQueueEventLoopGroup.class);
        } else {
            assertThat(group).isInstanceOf(NioEventLoopGroup.class);
        }
        group.shutdownGracefully();
    }
}
