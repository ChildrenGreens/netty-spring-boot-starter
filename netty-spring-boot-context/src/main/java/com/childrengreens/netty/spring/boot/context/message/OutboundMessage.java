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

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract representation of an outbound message for protocol-agnostic response handling.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see InboundMessage
 */
public class OutboundMessage {

    /**
     * Response headers or metadata.
     */
    private final Map<String, Object> headers;

    /**
     * The response payload.
     */
    private final Object payload;

    /**
     * HTTP status code (for HTTP responses).
     */
    private final int statusCode;

    /**
     * Create a new OutboundMessage with payload only.
     * @param payload the response payload
     */
    public OutboundMessage(Object payload) {
        this(new HashMap<>(), payload, 200);
    }

    /**
     * Create a new OutboundMessage with headers and payload.
     * @param headers the response headers
     * @param payload the response payload
     */
    public OutboundMessage(Map<String, Object> headers, Object payload) {
        this(headers, payload, 200);
    }

    /**
     * Create a new OutboundMessage with all parameters.
     * @param headers the response headers
     * @param payload the response payload
     * @param statusCode the HTTP status code
     */
    public OutboundMessage(Map<String, Object> headers, Object payload, int statusCode) {
        this.headers = headers != null ? headers : new HashMap<>();
        this.payload = payload;
        this.statusCode = statusCode;
    }

    /**
     * Return the response headers.
     * @return the headers map
     */
    public Map<String, Object> getHeaders() {
        return this.headers;
    }

    /**
     * Return the response payload.
     * @return the payload
     */
    public Object getPayload() {
        return this.payload;
    }

    /**
     * Return the HTTP status code.
     * @return the status code
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * Create an OutboundMessage with OK status.
     * @param payload the payload
     * @return the outbound message
     */
    public static OutboundMessage ok(Object payload) {
        return new OutboundMessage(payload);
    }

    /**
     * Create an OutboundMessage with the specified status code.
     * @param statusCode the status code
     * @param payload the payload
     * @return the outbound message
     */
    public static OutboundMessage status(int statusCode, Object payload) {
        return new OutboundMessage(null, payload, statusCode);
    }

    /**
     * Create an error OutboundMessage.
     * @param statusCode the status code
     * @param message the error message
     * @return the outbound message
     */
    public static OutboundMessage error(int statusCode, String message) {
        Map<String, Object> errorPayload = new HashMap<>();
        errorPayload.put("error", message);
        errorPayload.put("status", statusCode);
        return new OutboundMessage(null, errorPayload, statusCode);
    }

    /**
     * Create a builder for constructing OutboundMessage instances.
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link OutboundMessage}.
     */
    public static class Builder {

        private Map<String, Object> headers = new HashMap<>();
        private Object payload;
        private int statusCode = 200;

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
         * Set the status code.
         * @param statusCode the status code
         * @return this builder
         */
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        /**
         * Build the OutboundMessage.
         * @return the constructed message
         */
        public OutboundMessage build() {
            return new OutboundMessage(headers, payload, statusCode);
        }

    }

}
