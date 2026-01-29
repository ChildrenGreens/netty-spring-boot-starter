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

package com.childrengreens.netty.spring.boot.context.message;

import com.childrengreens.netty.spring.boot.context.properties.TransportType;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract representation of an inbound message for protocol-agnostic dispatching.
 *
 * <p>This abstraction allows the router and dispatcher to work uniformly
 * across different protocols (TCP, UDP, HTTP, WebSocket).
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see OutboundMessage
 */
public class InboundMessage {

    /**
     * The transport protocol type.
     */
    private final TransportType transport;

    /**
     * The route key used for handler matching.
     * <p>For HTTP: path + method; for TCP/UDP: message type; for WS: path + frame type
     */
    private final String routeKey;

    /**
     * Message headers or metadata.
     */
    private final Map<String, Object> headers;

    /**
     * The decoded message payload.
     */
    private final Object payload;

    /**
     * The raw payload bytes for lazy decoding.
     */
    private final byte[] rawPayload;

    /**
     * Create a new InboundMessage.
     * @param transport the transport type
     * @param routeKey the route key for matching
     * @param headers message headers
     * @param payload the decoded payload
     */
    public InboundMessage(TransportType transport, String routeKey,
                          Map<String, Object> headers, Object payload) {
        this(transport, routeKey, headers, payload, null);
    }

    /**
     * Create a new InboundMessage with raw payload.
     * @param transport the transport type
     * @param routeKey the route key for matching
     * @param headers message headers
     * @param payload the decoded payload
     * @param rawPayload the raw payload bytes
     */
    public InboundMessage(TransportType transport, String routeKey,
                          Map<String, Object> headers, Object payload, byte[] rawPayload) {
        this.transport = transport;
        this.routeKey = routeKey;
        this.headers = headers != null ? headers : new HashMap<>();
        this.payload = payload;
        this.rawPayload = rawPayload;
    }

    /**
     * Return the transport type.
     * @return the transport type
     */
    public TransportType getTransport() {
        return this.transport;
    }

    /**
     * Return the route key.
     * @return the route key
     */
    public String getRouteKey() {
        return this.routeKey;
    }

    /**
     * Return the message headers.
     * @return the headers map
     */
    public Map<String, Object> getHeaders() {
        return this.headers;
    }

    /**
     * Get a header value by name.
     * @param name the header name
     * @param <T> the expected type
     * @return the header value, or {@code null} if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getHeader(String name) {
        return (T) this.headers.get(name);
    }

    /**
     * Return the decoded payload.
     * @return the payload object
     */
    public Object getPayload() {
        return this.payload;
    }

    /**
     * Return the payload cast to the specified type.
     * @param type the expected type
     * @param <T> the type parameter
     * @return the typed payload
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayload(Class<T> type) {
        return (T) this.payload;
    }

    /**
     * Return the raw payload bytes.
     * @return the raw bytes, or {@code null} if not available
     */
    public byte[] getRawPayload() {
        return this.rawPayload;
    }

    /**
     * Create a builder for constructing InboundMessage instances.
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InboundMessage}.
     */
    public static class Builder {

        private TransportType transport;
        private String routeKey;
        private Map<String, Object> headers = new HashMap<>();
        private Object payload;
        private byte[] rawPayload;

        /**
         * Set the transport type.
         * @param transport the transport type
         * @return this builder
         */
        public Builder transport(TransportType transport) {
            this.transport = transport;
            return this;
        }

        /**
         * Set the route key.
         * @param routeKey the route key
         * @return this builder
         */
        public Builder routeKey(String routeKey) {
            this.routeKey = routeKey;
            return this;
        }

        /**
         * Add a header.
         * @param name the header name
         * @param value the header value
         * @return this builder
         */
        public Builder header(String name, Object value) {
            this.headers.put(name, value);
            return this;
        }

        /**
         * Set all headers.
         * @param headers the headers map
         * @return this builder
         */
        public Builder headers(Map<String, Object> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Set the payload.
         * @param payload the payload
         * @return this builder
         */
        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        /**
         * Set the raw payload.
         * @param rawPayload the raw bytes
         * @return this builder
         */
        public Builder rawPayload(byte[] rawPayload) {
            this.rawPayload = rawPayload;
            return this;
        }

        /**
         * Build the InboundMessage.
         * @return the constructed message
         */
        public InboundMessage build() {
            return new InboundMessage(transport, routeKey, headers, payload, rawPayload);
        }

    }

}
