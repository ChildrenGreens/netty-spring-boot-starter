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

import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import com.childrengreens.netty.spring.boot.context.properties.TransportImpl;
import com.childrengreens.netty.spring.boot.context.server.ServerRuntime;
import com.childrengreens.netty.spring.boot.context.server.ServerState;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.DatagramSocket;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UdpTransportStarter}.
 */
class UdpTransportStarterTest {

    private TransportFactory transportFactory;
    private UdpTransportStarter udpTransportStarter;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerRuntime serverRuntime;

    @BeforeEach
    void setUp() {
        transportFactory = new TransportFactory(TransportImpl.NIO);
        udpTransportStarter = new UdpTransportStarter(transportFactory);
        bossGroup = transportFactory.createBossGroup(1);
        workerGroup = transportFactory.createWorkerGroup(2);
    }

    @AfterEach
    void tearDown() {
        if (serverRuntime != null) {
            serverRuntime.getBindChannel().close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    @Test
    void start_withValidSpec_createsRuntime() throws Exception {
        int port = findAvailablePort();
        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-udp-server");
        serverSpec.setHost("127.0.0.1");
        serverSpec.setPort(port);

        ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                // Empty initializer for test
            }
        };

        serverRuntime = udpTransportStarter.start(serverSpec, bossGroup, workerGroup, initializer);

        assertThat(serverRuntime).isNotNull();
        assertThat(serverRuntime.getState()).isEqualTo(ServerState.RUNNING);
        assertThat(serverRuntime.getBindChannel()).isNotNull();
        assertThat(serverRuntime.getBindChannel().isOpen()).isTrue();
        // UDP doesn't use boss group
        assertThat(serverRuntime.getBossGroup()).isNull();
    }

    @Test
    void start_withoutHost_bindsToAllInterfaces() throws Exception {
        int port = findAvailablePort();
        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-udp-server-no-host");
        serverSpec.setHost("");
        serverSpec.setPort(port);

        ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                // Empty initializer for test
            }
        };

        serverRuntime = udpTransportStarter.start(serverSpec, bossGroup, workerGroup, initializer);

        assertThat(serverRuntime).isNotNull();
        assertThat(serverRuntime.getState()).isEqualTo(ServerState.RUNNING);
    }

    @Test
    void start_withDefaultHost_bindsToAllInterfaces() throws Exception {
        int port = findAvailablePort();
        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-udp-server-default");
        serverSpec.setHost("0.0.0.0");
        serverSpec.setPort(port);

        ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                // Empty initializer for test
            }
        };

        serverRuntime = udpTransportStarter.start(serverSpec, bossGroup, workerGroup, initializer);

        assertThat(serverRuntime).isNotNull();
        assertThat(serverRuntime.getState()).isEqualTo(ServerState.RUNNING);
    }

    @Test
    void start_withNullHost_bindsToAllInterfaces() throws Exception {
        int port = findAvailablePort();
        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-udp-server-null-host");
        serverSpec.setHost(null);
        serverSpec.setPort(port);

        ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                // Empty initializer for test
            }
        };

        serverRuntime = udpTransportStarter.start(serverSpec, bossGroup, workerGroup, initializer);

        assertThat(serverRuntime).isNotNull();
        assertThat(serverRuntime.getState()).isEqualTo(ServerState.RUNNING);
    }

    @Test
    void constructor_setsTransportFactory() {
        UdpTransportStarter starter = new UdpTransportStarter(transportFactory);
        assertThat(starter).isNotNull();
    }

    private int findAvailablePort() throws Exception {
        try (DatagramSocket socket = new DatagramSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
