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

import com.childrengreens.netty.spring.boot.context.backpressure.BackpressureMetrics;
import com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics;
import com.childrengreens.netty.spring.boot.context.pipeline.PipelineAssembler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

/**
 * Transport starter for UDP-based servers.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class UdpTransportStarter implements TransportStarter {

    private static final Logger logger = LoggerFactory.getLogger(UdpTransportStarter.class);

    private final TransportFactory transportFactory;
    private final PipelineAssembler pipelineAssembler;

    /**
     * Create a new UdpTransportStarter.
     * @param transportFactory the transport factory
     * @param pipelineAssembler the pipeline assembler
     */
    public UdpTransportStarter(TransportFactory transportFactory, PipelineAssembler pipelineAssembler) {
        this.transportFactory = transportFactory;
        this.pipelineAssembler = pipelineAssembler;
    }

    @Override
    public ServerRuntime start(ServerSpec serverSpec, EventLoopGroup bossGroup,
                                EventLoopGroup workerGroup,
                                ChannelInitializer<SocketChannel> initializer,
                                ServerMetrics serverMetrics,
                                @Nullable BackpressureMetrics backpressureMetrics) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(transportFactory.getDatagramChannelClass())
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) throws Exception {
                        pipelineAssembler.assemble(ch.pipeline(), serverSpec, serverMetrics, backpressureMetrics);
                    }
                });

        String host = serverSpec.getHost();
        int port = serverSpec.getPort();

        Channel channel = bind(bootstrap, serverSpec);

        logger.info("UDP Server [{}] started on {}:{}", serverSpec.getName(), host, port);

        return new ServerRuntime(
                serverSpec,
                null, // UDP doesn't use boss group
                workerGroup,
                channel,
                ServerState.RUNNING,
                serverMetrics,
                backpressureMetrics
        );
    }

}
