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

import com.childrengreens.netty.spring.boot.context.properties.RateLimitSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Feature provider for request rate limiting.
 *
 * <p>Implements a token bucket algorithm for limiting the number of
 * requests per second on each channel.
 *
 * @author Netty Spring Boot
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
                    rateLimit.getBurstSize()
            ));
            logger.debug("Configured rate limit: {} req/s, burst: {}",
                    rateLimit.getRequestsPerSecond(), rateLimit.getBurstSize());
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
     */
    private static class RateLimitHandler extends ChannelInboundHandlerAdapter {

        private final double requestsPerSecond;
        private final int burstSize;
        private final AtomicLong tokens;
        private final AtomicLong lastRefillTime;

        RateLimitHandler(double requestsPerSecond, int burstSize) {
            this.requestsPerSecond = requestsPerSecond;
            this.burstSize = burstSize > 0 ? burstSize : (int) Math.max(1, requestsPerSecond);
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

            // No tokens available
            logger.warn("Rate limit exceeded for channel: {}", ctx.channel().remoteAddress());
            ctx.close();
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
    }

}
