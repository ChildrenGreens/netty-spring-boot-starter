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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ConnectionManager}.
 */
class ConnectionManagerTest {

    private ConnectionManager connectionManager;
    private Channel channel1;
    private Channel channel2;
    private ChannelFuture closeFuture1;
    private ChannelFuture closeFuture2;

    @BeforeEach
    void setUp() {
        connectionManager = new ConnectionManager();

        // Set up mock channels
        channel1 = mock(Channel.class);
        channel2 = mock(Channel.class);
        closeFuture1 = mock(ChannelFuture.class);
        closeFuture2 = mock(ChannelFuture.class);

        when(channel1.isActive()).thenReturn(true);
        when(channel2.isActive()).thenReturn(true);
        when(channel1.closeFuture()).thenReturn(closeFuture1);
        when(channel2.closeFuture()).thenReturn(closeFuture2);
        when(closeFuture1.addListener(any())).thenReturn(closeFuture1);
        when(closeFuture2.addListener(any())).thenReturn(closeFuture2);
    }

    @Test
    void onAuthenticated_allowsFirstConnection() {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setAllowMultiple(false);
        policy.setStrategy(ConnectionStrategy.REJECT_NEW);

        boolean allowed = connectionManager.onAuthenticated(channel1, "user1", policy, null);

        assertThat(allowed).isTrue();
        assertThat(connectionManager.getConnectionCount("user1")).isEqualTo(1);
    }

    @Test
    void onAuthenticated_withAllowMultiple_allowsMultipleConnections() {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setAllowMultiple(true);
        policy.setMaxConnectionsPerUser(5);

        connectionManager.onAuthenticated(channel1, "user1", policy, null);
        boolean allowed = connectionManager.onAuthenticated(channel2, "user1", policy, null);

        assertThat(allowed).isTrue();
        assertThat(connectionManager.getConnectionCount("user1")).isEqualTo(2);
    }

    @Test
    void onAuthenticated_withMaxConnections_rejectsExcess() {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setAllowMultiple(true);
        policy.setMaxConnectionsPerUser(1);

        connectionManager.onAuthenticated(channel1, "user1", policy, null);
        boolean allowed = connectionManager.onAuthenticated(channel2, "user1", policy, null);

        assertThat(allowed).isFalse();
        assertThat(connectionManager.getConnectionCount("user1")).isEqualTo(1);
    }

    @Test
    void onAuthenticated_rejectNew_rejectsSecondConnection() {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setAllowMultiple(false);
        policy.setStrategy(ConnectionStrategy.REJECT_NEW);

        connectionManager.onAuthenticated(channel1, "user1", policy, null);
        boolean allowed = connectionManager.onAuthenticated(channel2, "user1", policy, null);

        assertThat(allowed).isFalse();
    }

    @Test
    void onAuthenticated_kickOld_closesOldConnection() {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setAllowMultiple(false);
        policy.setStrategy(ConnectionStrategy.KICK_OLD);

        // Mock writeAndFlush for kick message
        ChannelFuture writeFuture = mock(ChannelFuture.class);
        when(channel1.writeAndFlush(any())).thenReturn(writeFuture);
        when(writeFuture.addListener(any())).thenAnswer(invocation -> {
            // Simulate the listener being called
            return writeFuture;
        });

        connectionManager.onAuthenticated(channel1, "user1", policy, null);
        boolean allowed = connectionManager.onAuthenticated(channel2, "user1", policy, null);

        assertThat(allowed).isTrue();
        verify(channel1).writeAndFlush(any()); // Kick message sent
    }

    @Test
    void onAuthenticated_kickOld_incrementsMetrics() {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setAllowMultiple(false);
        policy.setStrategy(ConnectionStrategy.KICK_OLD);
        AuthMetrics metrics = new AuthMetrics("test");

        ChannelFuture writeFuture = mock(ChannelFuture.class);
        when(channel1.writeAndFlush(any())).thenReturn(writeFuture);
        when(writeFuture.addListener(any())).thenReturn(writeFuture);

        connectionManager.onAuthenticated(channel1, "user1", policy, metrics);
        connectionManager.onAuthenticated(channel2, "user1", policy, metrics);

        assertThat(metrics.getKickedCount()).isEqualTo(1);
    }

    @Test
    void getConnections_returnsEmptySetForUnknownUser() {
        Set<Channel> connections = connectionManager.getConnections("unknown");

        assertThat(connections).isEmpty();
    }

    @Test
    void getConnections_filtersClosedChannels() {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setAllowMultiple(true);
        policy.setMaxConnectionsPerUser(5);

        connectionManager.onAuthenticated(channel1, "user1", policy, null);
        connectionManager.onAuthenticated(channel2, "user1", policy, null);

        // Simulate channel1 becoming inactive
        when(channel1.isActive()).thenReturn(false);

        Set<Channel> connections = connectionManager.getConnections("user1");

        assertThat(connections).containsExactly(channel2);
    }

    @Test
    void kickUser_closesAllConnections() {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setAllowMultiple(true);
        policy.setMaxConnectionsPerUser(5);

        ChannelFuture writeFuture1 = mock(ChannelFuture.class);
        ChannelFuture writeFuture2 = mock(ChannelFuture.class);
        when(channel1.writeAndFlush(any())).thenReturn(writeFuture1);
        when(channel2.writeAndFlush(any())).thenReturn(writeFuture2);
        when(writeFuture1.addListener(any())).thenReturn(writeFuture1);
        when(writeFuture2.addListener(any())).thenReturn(writeFuture2);

        connectionManager.onAuthenticated(channel1, "user1", policy, null);
        connectionManager.onAuthenticated(channel2, "user1", policy, null);

        connectionManager.kickUser("user1", "Admin action");

        verify(channel1).writeAndFlush(any());
        verify(channel2).writeAndFlush(any());
    }

    @Test
    void getConnectedUserCount_returnsCorrectCount() {
        ConnectionPolicy policy = new ConnectionPolicy();

        connectionManager.onAuthenticated(channel1, "user1", policy, null);
        connectionManager.onAuthenticated(channel2, "user2", policy, null);

        assertThat(connectionManager.getConnectedUserCount()).isEqualTo(2);
    }

    @Test
    void onAuthenticated_withNullPolicy_allowsConnection() {
        boolean allowed = connectionManager.onAuthenticated(channel1, "user1", null, null);

        assertThat(allowed).isTrue();
    }
}
