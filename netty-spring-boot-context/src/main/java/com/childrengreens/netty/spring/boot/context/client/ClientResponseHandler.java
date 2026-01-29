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

package com.childrengreens.netty.spring.boot.context.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Netty handler for processing server responses.
 *
 * <p>This handler receives decoded response messages and routes them
 * to the appropriate pending request using correlation IDs.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see RequestInvoker
 */
public class ClientResponseHandler extends SimpleChannelInboundHandler<Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(ClientResponseHandler.class);

    private final RequestInvoker requestInvoker;
    private final String clientName;

    /**
     * Create a new ClientResponseHandler.
     * @param requestInvoker the request invoker for completing requests
     * @param clientName the client name for logging
     */
    public ClientResponseHandler(RequestInvoker requestInvoker, String clientName) {
        this.requestInvoker = requestInvoker;
        this.clientName = clientName;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Map<String, Object> response) throws Exception {
        String correlationId = (String) response.get(RequestInvoker.CORRELATION_ID_HEADER);

        if (correlationId != null) {
            boolean completed = requestInvoker.completeRequest(correlationId, response);
            if (!completed) {
                logger.debug("Received response without pending request: correlationId={}", correlationId);
            }
        } else {
            // Response without correlation ID - may be a push message
            logger.debug("Received message without correlationId from server");
            handlePushMessage(ctx, response);
        }
    }

    /**
     * Handle push messages (messages without correlation ID).
     * @param ctx the channel context
     * @param message the message
     */
    protected void handlePushMessage(ChannelHandlerContext ctx, Map<String, Object> message) {
        // Default implementation logs the message
        // Subclasses can override to handle push messages
        String messageType = (String) message.get(RequestInvoker.MESSAGE_TYPE_HEADER);
        logger.debug("Push message received: type={}, client={}", messageType, clientName);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client [{}] connected to server: {}", clientName, ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client [{}] disconnected from server: {}", clientName, ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Client [{}] error: {}", clientName, cause.getMessage(), cause);
        ctx.close();
    }

}
