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

package com.childrengreens.netty.spring.boot.context.auth;

/**
 * Configuration specification for token-based authentication (HTTP).
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class TokenSpec {

    /**
     * Token type: JWT or API_KEY.
     */
    private TokenType type = TokenType.JWT;

    /**
     * HTTP header name to extract the token from.
     * Default is "Authorization".
     */
    private String headerName = "Authorization";

    /**
     * Secret key for JWT signing/verification.
     */
    private String secret;

    /**
     * Token expiration time in seconds.
     * Default is 3600 (1 hour).
     */
    private long expireSeconds = 3600;

    /**
     * JWT issuer claim.
     */
    private String issuer;

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

}
