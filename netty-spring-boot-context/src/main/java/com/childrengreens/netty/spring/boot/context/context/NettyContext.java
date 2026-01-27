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

package com.childrengreens.netty.spring.boot.context.context;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context object providing access to Netty channel information and attributes.
 *
 * <p>This context is available as a method parameter in handler methods and
 * provides convenient access to channel attributes, remote address, and
 * trace information.
 *
 * @author Netty Spring Boot
 * @since 1.0.0
 */
public class NettyContext {

    /**
     * Attribute key for storing the server name in the channel.
     */
    public static final AttributeKey<String> SERVER_NAME_KEY = AttributeKey.valueOf("netty.server.name");

    /**
     * Attribute key for storing the WebSocket path in the channel.
     */
    public static final AttributeKey<String> WS_PATH_KEY = AttributeKey.valueOf("netty.ws.path");

    /**
     * Attribute key for storing trace ID.
     */
    public static final AttributeKey<String> TRACE_ID_KEY = AttributeKey.valueOf("netty.trace.id");

    private final Channel channel;

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * Create a new NettyContext for the specified channel.
     * @param channel the Netty channel
     */
    public NettyContext(Channel channel) {
        this.channel = channel;
    }

    /**
     * Return the underlying Netty channel.
     * @return the channel
     */
    public Channel getChannel() {
        return this.channel;
    }

    /**
     * Return the channel ID as a string.
     * @return the channel ID
     */
    public String getChannelId() {
        return this.channel.id().asShortText();
    }

    /**
     * Return the remote address of the connection.
     * @return the remote socket address, or {@code null} if not available
     */
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) this.channel.remoteAddress();
    }

    /**
     * Return the remote IP address as a string.
     * @return the remote IP, or {@code null} if not available
     */
    public String getRemoteIp() {
        InetSocketAddress address = getRemoteAddress();
        return address != null ? address.getAddress().getHostAddress() : null;
    }

    /**
     * Return the server name this channel is connected to.
     * @return the server name
     */
    public String getServerName() {
        return this.channel.attr(SERVER_NAME_KEY).get();
    }

    /**
     * Return the WebSocket path for this connection.
     * @return the WebSocket path, or {@code null} if not a WebSocket connection
     */
    public String getWsPath() {
        return this.channel.attr(WS_PATH_KEY).get();
    }

    /**
     * Return the trace ID for this request.
     * @return the trace ID, or {@code null} if not set
     */
    public String getTraceId() {
        return this.channel.attr(TRACE_ID_KEY).get();
    }

    /**
     * Set the trace ID for this request.
     * @param traceId the trace ID
     */
    public void setTraceId(String traceId) {
        this.channel.attr(TRACE_ID_KEY).set(traceId);
    }

    /**
     * Get a custom attribute from this context.
     * @param key the attribute key
     * @param <T> the attribute type
     * @return the attribute value, or {@code null} if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) this.attributes.get(key);
    }

    /**
     * Set a custom attribute in this context.
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    /**
     * Remove a custom attribute from this context.
     * @param key the attribute key
     * @return the removed value, or {@code null} if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T removeAttribute(String key) {
        return (T) this.attributes.remove(key);
    }

    /**
     * Return whether the channel is active.
     * @return {@code true} if the channel is active
     */
    public boolean isActive() {
        return this.channel.isActive();
    }

    /**
     * Close the channel.
     */
    public void close() {
        this.channel.close();
    }

}
