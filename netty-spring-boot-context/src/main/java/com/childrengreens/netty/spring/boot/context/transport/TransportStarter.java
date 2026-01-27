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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;

/**
 * Strategy interface for transport-specific server bootstrapping.
 *
 * <p>Implementations handle the creation and configuration of server
 * bootstraps for different transport types (TCP, UDP, HTTP).
 *
 * @author Netty Spring Boot
 * @since 0.0.1
 * @see TransportFactory
 */
public interface TransportStarter {

    /**
     * Start the server and return the runtime.
     * @param serverSpec the server specification
     * @param bossGroup the boss event loop group
     * @param workerGroup the worker event loop group
     * @param initializer the channel initializer
     * @return the server runtime
     * @throws Exception if startup fails
     */
    ServerRuntime start(ServerSpec serverSpec, EventLoopGroup bossGroup,
                        EventLoopGroup workerGroup, ChannelInitializer<SocketChannel> initializer)
            throws Exception;

}
