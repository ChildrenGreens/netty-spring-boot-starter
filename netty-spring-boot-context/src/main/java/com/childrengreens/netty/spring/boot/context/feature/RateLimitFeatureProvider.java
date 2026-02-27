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

import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.properties.RateLimitAction;
import com.childrengreens.netty.spring.boot.context.properties.RateLimitSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Feature provider for request rate limiting.
 *
 * <p>Implements a token bucket algorithm for limiting the number of
 * requests per second on each channel.
 *
 * <p>When rate limit is exceeded, the behavior depends on the protocol type
 * and the configured {@link RateLimitAction}:
 * <ul>
 * <li>HTTP - Always returns 429 Too Many Requests</li>
 * <li>WebSocket/TCP/UDP - Configurable via {@link RateLimitAction}</li>
 * </ul>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class RateLimitFeatureProvider implements FeatureProvider {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFeatureProvider.class);

    /**
     * Feature name constant.
     */
    public static final String NAME = "rateLimit";

    /**
     * Order for this feature - early in the pipeline after connection limit.
     */
    public static final int ORDER = 50;

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
        RateLimitSpec rateLimit = serverSpec.getFeatures().getRateLimit();
        if (rateLimit != null && rateLimit.isEnabled()) {
            pipeline.addLast("rateLimitHandler", new RateLimitHandler(
                    rateLimit.getRequestsPerSecond(),
                    rateLimit.getBurstSize(),
                    rateLimit.getAction()
            ));
            logger.debug("Configured rate limit: {} req/s, burst: {}, action: {}",
                    rateLimit.getRequestsPerSecond(), rateLimit.getBurstSize(), rateLimit.getAction());
        }
    }

    @Override
    public boolean isEnabled(ServerSpec serverSpec) {
        RateLimitSpec rateLimit = serverSpec.getFeatures().getRateLimit();
        return rateLimit != null && rateLimit.isEnabled();
    }

    /**
     * Channel handler that implements token bucket rate limiting.
     *
     * <p>This implementation is thread-safe using CAS (Compare-And-Swap) operations
     * to handle concurrent access to the token bucket.
     *
     * <p>When rate limit is exceeded, the response depends on protocol type:
     * <ul>
     * <li>HTTP: Returns 429 Too Many Requests with Retry-After header</li>
     * <li>WebSocket: Behavior based on configured action (DROP/CLOSE/REJECT)</li>
     * <li>TCP/UDP: Behavior based on configured action (DROP/CLOSE/REJECT)</li>
     * </ul>
     */
    static class RateLimitHandler extends ChannelInboundHandlerAdapter {

        private static final Logger handlerLogger = LoggerFactory.getLogger(RateLimitHandler.class);

        private static final String RATE_LIMIT_ERROR_JSON =
                "{\"error\":\"rate_limit_exceeded\",\"message\":\"Too many requests\"}";

        private final double requestsPerSecond;
        private final int burstSize;
        private final RateLimitAction action;
        private final AtomicLong tokens;
        private final AtomicLong lastRefillTime;

        RateLimitHandler(double requestsPerSecond, int burstSize, RateLimitAction action) {
            this.requestsPerSecond = requestsPerSecond;
            this.burstSize = burstSize > 0 ? burstSize : (int) Math.max(1, requestsPerSecond);
            this.action = action != null ? action : RateLimitAction.DROP;
            this.tokens = new AtomicLong(this.burstSize);
            this.lastRefillTime = new AtomicLong(System.nanoTime());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            refillTokens();

            // Use CAS to atomically check and consume token
            long currentTokens = tokens.get();
            while (currentTokens > 0) {
                if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                    // Successfully consumed a token
                    super.channelRead(ctx, msg);
                    return;
                }
                // CAS failed, retry with updated value
                currentTokens = tokens.get();
            }

            // No tokens available - release the message to prevent memory leak
            handlerLogger.warn("Rate limit exceeded for channel: {}", ctx.channel().remoteAddress());
            ReferenceCountUtil.release(msg);

            // Send appropriate response based on protocol type
            handleRateLimitExceeded(ctx);
        }

        /**
         * Handle rate limit exceeded based on protocol type and configured action.
         */
        private void handleRateLimitExceeded(ChannelHandlerContext ctx) {
            String protocolType = getProtocolType(ctx);

            switch (protocolType) {
                case NettyContext.PROTOCOL_HTTP -> handleHttpRateLimit(ctx);
                case NettyContext.PROTOCOL_WEBSOCKET -> handleWebSocketRateLimit(ctx);
                case NettyContext.PROTOCOL_TCP, NettyContext.PROTOCOL_UDP -> handleTcpUdpRateLimit(ctx);
                default -> {
                    // Unknown protocol, apply configured action
                    if (action == RateLimitAction.CLOSE) {
                        ctx.close();
                    }
                }
            }
        }

        /**
         * Get the protocol type from channel attributes.
         */
        private String getProtocolType(ChannelHandlerContext ctx) {
            if (ctx.channel().hasAttr(NettyContext.PROTOCOL_TYPE_KEY)) {
                String type = ctx.channel().attr(NettyContext.PROTOCOL_TYPE_KEY).get();
                if (type != null) {
                    return type;
                }
            }
            return NettyContext.PROTOCOL_TCP; // Default to TCP
        }

        /**
         * Handle HTTP rate limit - always returns 429 Too Many Requests.
         */
        private void handleHttpRateLimit(ChannelHandlerContext ctx) {
            String errorJson = "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}";
            byte[] content = errorJson.getBytes(StandardCharsets.UTF_8);

            ByteBuf buffer = ctx.alloc().buffer(content.length);
            buffer.writeBytes(content);

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.TOO_MANY_REQUESTS,
                    buffer
            );
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
            response.headers().set("Retry-After", "1");

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        /**
         * Handle WebSocket rate limit based on configured action.
         */
        private void handleWebSocketRateLimit(ChannelHandlerContext ctx) {
            switch (action) {
                case DROP -> {
                    // Just drop the message, keep connection open
                    handlerLogger.debug("Dropped WebSocket message due to rate limit");
                }
                case CLOSE -> {
                    // Send close frame with policy violation code (1008)
                    ctx.writeAndFlush(new CloseWebSocketFrame(1008, "Rate limit exceeded"))
                            .addListener(ChannelFutureListener.CLOSE);
                }
                case REJECT -> {
                    // Send error message as text frame, keep connection open
                    ctx.writeAndFlush(new TextWebSocketFrame(RATE_LIMIT_ERROR_JSON));
                }
            }
        }

        /**
         * Handle TCP/UDP rate limit based on configured action.
         */
        private void handleTcpUdpRateLimit(ChannelHandlerContext ctx) {
            switch (action) {
                case DROP -> {
                    // Just drop the message, keep connection open
                    handlerLogger.debug("Dropped TCP/UDP message due to rate limit");
                }
                case CLOSE -> {
                    // Close the connection
                    ctx.close();
                }
                case REJECT -> {
                    // Send error message, keep connection open
                    byte[] content = RATE_LIMIT_ERROR_JSON.getBytes(StandardCharsets.UTF_8);
                    ByteBuf buffer = ctx.alloc().buffer(content.length);
                    buffer.writeBytes(content);
                    ctx.writeAndFlush(buffer);
                }
            }
        }

        /**
         * Refill tokens based on elapsed time using CAS for thread safety.
         */
        private void refillTokens() {
            long now = System.nanoTime();
            long currentTime;
            long tokensToAdd;

            // CAS loop for updating lastRefillTime
            do {
                currentTime = lastRefillTime.get();
                long elapsed = now - currentTime;
                tokensToAdd = (long) (elapsed * requestsPerSecond / 1_000_000_000L);

                if (tokensToAdd <= 0) {
                    return; // No tokens to add
                }
            } while (!lastRefillTime.compareAndSet(currentTime, now));

            // Atomically update tokens, capped at burstSize
            final long addedTokens = tokensToAdd;
            tokens.updateAndGet(current -> Math.min(burstSize, current + addedTokens));
        }

        /**
         * Return the current token count.
         * @return the current tokens
         */
        long getTokens() {
            return tokens.get();
        }

        /**
         * Return the burst size.
         * @return the burst size
         */
        int getBurstSize() {
            return burstSize;
        }

        /**
         * Return the configured action.
         * @return the rate limit action
         */
        RateLimitAction getAction() {
            return action;
        }
    }

}
