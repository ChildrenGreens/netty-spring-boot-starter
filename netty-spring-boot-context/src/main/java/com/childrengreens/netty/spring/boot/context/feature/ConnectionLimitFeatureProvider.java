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

package com.childrengreens.netty.spring.boot.context.feature;

import com.childrengreens.netty.spring.boot.context.properties.ConnectionLimitSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Feature provider for connection count limiting.
 *
 * <p>Limits the maximum number of concurrent connections to prevent
 * resource exhaustion.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class ConnectionLimitFeatureProvider implements FeatureProvider {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionLimitFeatureProvider.class);

    /**
     * Feature name constant.
     */
    public static final String NAME = "connectionLimit";

    /**
     * Order for this feature - very early in the pipeline.
     */
    public static final int ORDER = 10;

    /**
     * Shared handler instance per server.
     */
    private ConnectionLimitHandler sharedHandler;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        ConnectionLimitSpec connLimit = serverSpec.getFeatures().getConnectionLimit();
        if (connLimit != null && connLimit.isEnabled()) {
            if (sharedHandler == null) {
                sharedHandler = new ConnectionLimitHandler(connLimit.getMaxConnections());
            }
            pipeline.addLast("connectionLimitHandler", sharedHandler);
            logger.debug("Configured connection limit: {}", connLimit.getMaxConnections());
        }
    }

    @Override
    public boolean isEnabled(ServerSpec serverSpec) {
        ConnectionLimitSpec connLimit = serverSpec.getFeatures().getConnectionLimit();
        return connLimit != null && connLimit.isEnabled();
    }

    /**
     * Sharable channel handler that tracks and limits concurrent connections.
     */
    @Sharable
    static class ConnectionLimitHandler extends ChannelInboundHandlerAdapter {

        /**
         * Attribute key to mark channels that were rejected due to connection limit.
         * These channels should not decrement the counter in channelInactive.
         */
        private static final AttributeKey<Boolean> REJECTED_KEY =
                AttributeKey.valueOf("connectionLimitRejected");

        private final int maxConnections;
        private final AtomicInteger currentConnections = new AtomicInteger(0);

        ConnectionLimitHandler(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            int current = currentConnections.incrementAndGet();

            if (current > maxConnections) {
                currentConnections.decrementAndGet();
                // Mark this channel as rejected so channelInactive won't decrement again
                ctx.channel().attr(REJECTED_KEY).set(Boolean.TRUE);
                logger.warn("Connection limit reached ({}/{}), rejecting: {}",
                        current - 1, maxConnections, ctx.channel().remoteAddress());
                ctx.close();
                return;
            }

            logger.debug("Connection accepted ({}/{}): {}",
                    current, maxConnections, ctx.channel().remoteAddress());
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            // Only decrement if this channel was not rejected
            Boolean rejected = ctx.channel().attr(REJECTED_KEY).get();
            if (rejected == null || !rejected) {
                int current = currentConnections.decrementAndGet();
                logger.debug("Connection closed ({}/{}): {}",
                        current, maxConnections, ctx.channel().remoteAddress());
            }
            super.channelInactive(ctx);
        }

        /**
         * Return the current connection count.
         * @return the current number of connections
         */
        public int getCurrentConnections() {
            return currentConnections.get();
        }

        /**
         * Return the maximum connection limit.
         * @return the maximum connections
         */
        public int getMaxConnections() {
            return maxConnections;
        }
    }

}
