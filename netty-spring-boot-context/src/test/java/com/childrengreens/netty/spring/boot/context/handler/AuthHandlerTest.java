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

package com.childrengreens.netty.spring.boot.context.handler;

import com.childrengreens.netty.spring.boot.context.auth.*;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link AuthHandler}.
 */
class AuthHandlerTest {

    private AuthSpec spec;
    private Authenticator authenticator;
    private ConnectionManager connectionManager;
    private AuthMetrics metrics;
    private List<Object> receivedMessages;

    @BeforeEach
    void setUp() {
        spec = new AuthSpec();
        spec.setEnabled(true);
        spec.setMode(AuthMode.CREDENTIAL);
        spec.setAuthRoute("/auth");
        spec.setAuthTimeout(10000);
        spec.setCloseOnFailure(true);

        authenticator = mock(Authenticator.class);
        connectionManager = mock(ConnectionManager.class);
        metrics = new AuthMetrics("test-server");
        receivedMessages = new ArrayList<>();

        // Default mock behavior - allow connections
        when(connectionManager.onAuthenticated(any(), anyString(), any(), any())).thenReturn(true);
    }

    private EmbeddedChannel createChannel(AuthHandler handler) {
        return new EmbeddedChannel(handler, new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                receivedMessages.add(msg);
            }
        });
    }

    @Test
    void credentialAuth_validCredentials_authenticatesChannel() {
        when(authenticator.authenticateCredential("admin", "password"))
                .thenReturn(AuthResult.success("user123").withUsername("admin"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Send auth message
        Map<String, Object> authMessage = createAuthMessage("admin", "password");
        channel.writeInbound(authMessage);

        // Verify authenticated
        AuthPrincipal principal = channel.attr(NettyContext.AUTH_PRINCIPAL_KEY).get();
        assertThat(principal).isNotNull();
        assertThat(principal.getUserId()).isEqualTo("user123");
        assertThat(metrics.getSuccessCount()).isEqualTo(1);

        // Auth message should not be passed to next handler
        assertThat(receivedMessages).isEmpty();

        // Verify response was sent
        Object response = channel.readOutbound();
        assertThat(response).isNotNull();

        channel.close();
    }

    @Test
    void credentialAuth_invalidCredentials_rejectsAndCloses() {
        when(authenticator.authenticateCredential("admin", "wrong"))
                .thenReturn(AuthResult.failure("INVALID_CREDENTIALS", "Wrong password"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        Map<String, Object> authMessage = createAuthMessage("admin", "wrong");
        channel.writeInbound(authMessage);

        // Verify not authenticated
        AuthPrincipal principal = channel.attr(NettyContext.AUTH_PRINCIPAL_KEY).get();
        assertThat(principal).isNull();
        assertThat(metrics.getFailureCount()).isEqualTo(1);

        // Channel should be closed
        assertThat(channel.isActive()).isFalse();

        channel.close();
    }

    @Test
    void credentialAuth_alreadyAuthenticated_passesThrough() {
        when(authenticator.authenticateCredential("admin", "password"))
                .thenReturn(AuthResult.success("user123"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // First authenticate
        channel.writeInbound(createAuthMessage("admin", "password"));

        // Then send business message
        Map<String, Object> businessMessage = new HashMap<>();
        businessMessage.put("type", "user.query");
        businessMessage.put("payload", Map.of("userId", "123"));
        channel.writeInbound(businessMessage);

        // Business message should be passed through
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.get(0)).isEqualTo(businessMessage);

        channel.close();
    }

    @Test
    void credentialAuth_nonAuthFirstMessage_rejects() {
        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        // Send non-auth message first
        Map<String, Object> businessMessage = new HashMap<>();
        businessMessage.put("type", "user.query");
        channel.writeInbound(businessMessage);

        // Should be rejected and closed
        assertThat(channel.isActive()).isFalse();
        assertThat(receivedMessages).isEmpty();

        channel.close();
    }

    @Test
    void credentialAuth_missingUsername_rejects() {
        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        Map<String, Object> authMessage = new HashMap<>();
        authMessage.put("type", "/auth");
        authMessage.put("payload", Map.of("password", "123"));
        channel.writeInbound(authMessage);

        assertThat(channel.isActive()).isFalse();

        channel.close();
    }

    @Test
    void credentialAuth_connectionRejected_closesChannel() {
        when(authenticator.authenticateCredential("admin", "password"))
                .thenReturn(AuthResult.success("user123"));
        when(connectionManager.onAuthenticated(any(), anyString(), any(), any()))
                .thenReturn(false);

        spec.setConnectionPolicy(new ConnectionPolicy());
        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        channel.writeInbound(createAuthMessage("admin", "password"));

        // Should be closed due to connection policy
        assertThat(channel.isActive()).isFalse();
        assertThat(metrics.getFailureCount()).isEqualTo(1);

        channel.close();
    }

    @Test
    void credentialAuth_closeOnFailureFalse_keepsChannelOpen() {
        spec.setCloseOnFailure(false);
        when(authenticator.authenticateCredential("admin", "wrong"))
                .thenReturn(AuthResult.failure("INVALID_CREDENTIALS", "Wrong password"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, metrics);
        EmbeddedChannel channel = createChannel(handler);

        channel.writeInbound(createAuthMessage("admin", "wrong"));

        // Channel should still be active
        assertThat(channel.isActive()).isTrue();

        channel.close();
    }

    @Test
    void credentialAuth_nullMetrics_handlesGracefully() {
        when(authenticator.authenticateCredential("admin", "password"))
                .thenReturn(AuthResult.success("user123"));

        AuthHandler handler = new AuthHandler(spec, authenticator, connectionManager, null);
        EmbeddedChannel channel = createChannel(handler);

        // Should not throw
        channel.writeInbound(createAuthMessage("admin", "password"));

        AuthPrincipal principal = channel.attr(NettyContext.AUTH_PRINCIPAL_KEY).get();
        assertThat(principal).isNotNull();

        channel.close();
    }

    @Test
    void credentialAuth_nullConnectionManager_stillWorks() {
        when(authenticator.authenticateCredential("admin", "password"))
                .thenReturn(AuthResult.success("user123"));

        AuthHandler handler = new AuthHandler(spec, authenticator, null, metrics);
        EmbeddedChannel channel = createChannel(handler);

        channel.writeInbound(createAuthMessage("admin", "password"));

        AuthPrincipal principal = channel.attr(NettyContext.AUTH_PRINCIPAL_KEY).get();
        assertThat(principal).isNotNull();

        channel.close();
    }

    private Map<String, Object> createAuthMessage(String username, String password) {
        Map<String, Object> authMessage = new HashMap<>();
        authMessage.put("type", "/auth");
        authMessage.put("payload", Map.of("username", username, "password", password));
        return authMessage;
    }
}
