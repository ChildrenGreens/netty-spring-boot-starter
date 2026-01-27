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
import com.childrengreens.netty.spring.boot.context.server.ServerRuntime;
import com.childrengreens.netty.spring.boot.context.server.ServerState;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transport starter for TCP-based servers (TCP, HTTP, WebSocket).
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 */
public class TcpTransportStarter implements TransportStarter {

    private static final Logger logger = LoggerFactory.getLogger(TcpTransportStarter.class);

    private final TransportFactory transportFactory;

    /**
     * Create a new TcpTransportStarter.
     * @param transportFactory the transport factory
     */
    public TcpTransportStarter(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    @Override
    public ServerRuntime start(ServerSpec serverSpec, EventLoopGroup bossGroup,
                                EventLoopGroup workerGroup,
                                ChannelInitializer<SocketChannel> initializer) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(transportFactory.getServerChannelClass())
                .childHandler(initializer)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        String host = serverSpec.getHost();
        int port = serverSpec.getPort();

        Channel channel;
        if (host != null && !host.isEmpty() && !"0.0.0.0".equals(host)) {
            channel = bootstrap.bind(host, port).sync().channel();
        } else {
            channel = bootstrap.bind(port).sync().channel();
        }

        logger.info("Server [{}] started on {}:{}", serverSpec.getName(), host, port);

        return new ServerRuntime(
                serverSpec,
                bossGroup,
                workerGroup,
                channel,
                ServerState.RUNNING
        );
    }

}
