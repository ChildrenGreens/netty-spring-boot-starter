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

import com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import com.childrengreens.netty.spring.boot.context.server.ServerRuntime;
import com.childrengreens.netty.spring.boot.context.server.ServerState;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transport starter for UDP-based servers.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class UdpTransportStarter implements TransportStarter {

    private static final Logger logger = LoggerFactory.getLogger(UdpTransportStarter.class);

    private final TransportFactory transportFactory;

    /**
     * Create a new UdpTransportStarter.
     * @param transportFactory the transport factory
     */
    public UdpTransportStarter(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    @Override
    public ServerRuntime start(ServerSpec serverSpec, EventLoopGroup bossGroup,
                                EventLoopGroup workerGroup,
                                ChannelInitializer<SocketChannel> initializer,
                                ServerMetrics serverMetrics) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) throws Exception {
                        // UDP-specific initialization
                        // The actual pipeline will be configured separately
                    }
                });

        String host = serverSpec.getHost();
        int port = serverSpec.getPort();

        Channel channel;
        if (host != null && !host.isEmpty() && !"0.0.0.0".equals(host)) {
            channel = bootstrap.bind(host, port).sync().channel();
        } else {
            channel = bootstrap.bind(port).sync().channel();
        }

        logger.info("UDP Server [{}] started on {}:{}", serverSpec.getName(), host, port);

        return new ServerRuntime(
                serverSpec,
                null, // UDP doesn't use boss group
                workerGroup,
                channel,
                ServerState.RUNNING,
                serverMetrics
        );
    }

}
