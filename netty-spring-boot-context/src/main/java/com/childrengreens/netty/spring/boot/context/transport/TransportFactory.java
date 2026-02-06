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
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating transport-specific components.
 *
 * <p>This factory creates the appropriate event loop groups and channel
 * classes based on the platform and configuration.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class TransportFactory {

    private static final Logger logger = LoggerFactory.getLogger(TransportFactory.class);

    private final TransportImpl preferredTransport;
    private final TransportImpl resolvedTransport;

    /**
     * Create a new TransportFactory with the specified preference.
     * @param preferredTransport the preferred transport implementation
     */
    public TransportFactory(TransportImpl preferredTransport) {
        this.preferredTransport = preferredTransport;
        this.resolvedTransport = resolveTransport(preferredTransport);
        logger.info("Transport resolved: {} (preferred: {})", resolvedTransport, preferredTransport);
    }

    /**
     * Resolve the actual transport to use based on preference and availability.
     */
    private TransportImpl resolveTransport(TransportImpl preferred) {
        if (preferred == TransportImpl.AUTO) {
            if (Epoll.isAvailable()) {
                return TransportImpl.EPOLL;
            }
            if (KQueue.isAvailable()) {
                return TransportImpl.KQUEUE;
            }
            return TransportImpl.NIO;
        }

        // Verify requested transport is available
        if (preferred == TransportImpl.EPOLL && !Epoll.isAvailable()) {
            logger.warn("EPOLL not available, falling back to NIO");
            return TransportImpl.NIO;
        }
        if (preferred == TransportImpl.KQUEUE && !KQueue.isAvailable()) {
            logger.warn("KQUEUE not available, falling back to NIO");
            return TransportImpl.NIO;
        }

        return preferred;
    }

    /**
     * Create an event loop group for boss threads.
     * @param nThreads the number of threads
     * @return the event loop group
     */
    public EventLoopGroup createBossGroup(int nThreads) {
        return createEventLoopGroup(nThreads);
    }

    /**
     * Create an event loop group for worker threads.
     * @param nThreads the number of threads (0 for default)
     * @return the event loop group
     */
    public EventLoopGroup createWorkerGroup(int nThreads) {
        return createEventLoopGroup(nThreads);
    }

    /**
     * Create an event loop group with the resolved transport.
     */
    private EventLoopGroup createEventLoopGroup(int nThreads) {
        return switch (resolvedTransport) {
            case EPOLL -> new EpollEventLoopGroup(nThreads);
            case KQUEUE -> new KQueueEventLoopGroup(nThreads);
            default -> new NioEventLoopGroup(nThreads);
        };
    }

    /**
     * Get the server channel class for TCP.
     * @return the server channel class
     */
    public Class<? extends ServerChannel> getServerChannelClass() {
        return switch (resolvedTransport) {
            case EPOLL -> EpollServerSocketChannel.class;
            case KQUEUE -> KQueueServerSocketChannel.class;
            default -> NioServerSocketChannel.class;
        };
    }

    /**
     * Get the client channel class for TCP.
     * @return the client channel class
     */
    public Class<? extends SocketChannel> getClientChannelClass() {
        return switch (resolvedTransport) {
            case EPOLL -> EpollSocketChannel.class;
            case KQUEUE -> KQueueSocketChannel.class;
            default -> NioSocketChannel.class;
        };
    }

    /**
     * Get the datagram channel class for UDP.
     * @return the datagram channel class
     */
    public Class<? extends DatagramChannel> getDatagramChannelClass() {
        return switch (resolvedTransport) {
            case EPOLL -> EpollDatagramChannel.class;
            case KQUEUE -> KQueueDatagramChannel.class;
            default -> NioDatagramChannel.class;
        };
    }

    /**
     * Get a transport starter for the specified transport type.
     * @param transportType the transport type
     * @return the transport starter
     */
    public TransportStarter getTransportStarter(TransportType transportType) {
        return switch (transportType) {
            case TCP, HTTP -> new TcpTransportStarter(this);
            case UDP -> new UdpTransportStarter(this);
        };
    }

    /**
     * Return the resolved transport implementation.
     * @return the resolved transport
     */
    public TransportImpl getResolvedTransport() {
        return resolvedTransport;
    }

}
