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
import com.childrengreens.netty.spring.boot.context.properties.FeaturesSpec;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ConnectionLimitFeatureProvider}.
 */
class ConnectionLimitFeatureProviderTest {

    private ConnectionLimitFeatureProvider provider;
    private ChannelPipeline pipeline;
    private ServerSpec serverSpec;
    private FeaturesSpec featuresSpec;
    private ConnectionLimitSpec connectionLimitSpec;

    @BeforeEach
    void setUp() {
        provider = new ConnectionLimitFeatureProvider();
        pipeline = mock(ChannelPipeline.class);
        serverSpec = mock(ServerSpec.class);
        featuresSpec = mock(FeaturesSpec.class);
        connectionLimitSpec = new ConnectionLimitSpec();

        when(serverSpec.getFeatures()).thenReturn(featuresSpec);
        when(featuresSpec.getConnectionLimit()).thenReturn(connectionLimitSpec);
        when(pipeline.addLast(any(String.class), any())).thenReturn(pipeline);
    }

    @Test
    void getName_returnsConnectionLimit() {
        assertThat(provider.getName()).isEqualTo("connectionLimit");
    }

    @Test
    void getOrder_returns10() {
        assertThat(provider.getOrder()).isEqualTo(10);
    }

    @Test
    void isEnabled_whenConnectionLimitEnabled_returnsTrue() {
        connectionLimitSpec.setEnabled(true);
        assertThat(provider.isEnabled(serverSpec)).isTrue();
    }

    @Test
    void isEnabled_whenConnectionLimitDisabled_returnsFalse() {
        connectionLimitSpec.setEnabled(false);
        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void isEnabled_whenConnectionLimitSpecNull_returnsFalse() {
        when(featuresSpec.getConnectionLimit()).thenReturn(null);
        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void configure_whenEnabled_addsConnectionLimitHandler() {
        connectionLimitSpec.setEnabled(true);
        connectionLimitSpec.setMaxConnections(10000);

        provider.configure(pipeline, serverSpec);

        verify(pipeline).addLast(eq("connectionLimitHandler"), any());
    }

    @Test
    void configure_whenDisabled_doesNotAddHandler() {
        connectionLimitSpec.setEnabled(false);

        provider.configure(pipeline, serverSpec);

        verify(pipeline, never()).addLast(any(String.class), any());
    }

    @Test
    void configure_whenConnectionLimitSpecNull_doesNotAddHandler() {
        when(featuresSpec.getConnectionLimit()).thenReturn(null);

        provider.configure(pipeline, serverSpec);

        verify(pipeline, never()).addLast(any(String.class), any());
    }

    @Test
    void configure_calledTwice_reusesSameHandler() {
        connectionLimitSpec.setEnabled(true);
        connectionLimitSpec.setMaxConnections(1000);

        provider.configure(pipeline, serverSpec);
        provider.configure(pipeline, serverSpec);

        // Should add handler twice but reusing the same instance
        verify(pipeline, times(2)).addLast(eq("connectionLimitHandler"), any());
    }

    @Test
    void getName_returnsConstantValue() {
        assertThat(provider.getName()).isEqualTo(ConnectionLimitFeatureProvider.NAME);
    }

    @Test
    void getOrder_returnsConstantValue() {
        assertThat(provider.getOrder()).isEqualTo(ConnectionLimitFeatureProvider.ORDER);
    }

    @Test
    void isEnabled_withFeaturesSpecNull_returnsFalse() {
        when(serverSpec.getFeatures()).thenReturn(featuresSpec);
        when(featuresSpec.getConnectionLimit()).thenReturn(null);

        assertThat(provider.isEnabled(serverSpec)).isFalse();
    }

    @Test
    void configure_withDifferentMaxConnections_createsHandler() {
        connectionLimitSpec.setEnabled(true);
        connectionLimitSpec.setMaxConnections(500);

        provider.configure(pipeline, serverSpec);

        verify(pipeline).addLast(eq("connectionLimitHandler"), any());
    }

    @Test
    void connectionLimitHandler_allowsConnectionsWithinLimit() {
        ServerSpec realServerSpec = new ServerSpec();
        realServerSpec.setName("test-server");
        FeaturesSpec features = new FeaturesSpec();
        ConnectionLimitSpec connLimit = new ConnectionLimitSpec();
        connLimit.setEnabled(true);
        connLimit.setMaxConnections(10);
        features.setConnectionLimit(connLimit);
        realServerSpec.setFeatures(features);

        EmbeddedChannel channel = new EmbeddedChannel();

        provider.configure(channel.pipeline(), realServerSpec);

        assertThat(channel.pipeline().get("connectionLimitHandler")).isNotNull();

        // Simulate channel active event
        channel.pipeline().fireChannelActive();

        // Channel should still be open
        assertThat(channel.isActive()).isTrue();

        channel.close();
    }

    @Test
    void connectionLimitHandler_rejectsConnectionsOverLimit() {
        ServerSpec realServerSpec = new ServerSpec();
        realServerSpec.setName("test-server");
        FeaturesSpec features = new FeaturesSpec();
        ConnectionLimitSpec connLimit = new ConnectionLimitSpec();
        connLimit.setEnabled(true);
        connLimit.setMaxConnections(2);
        features.setConnectionLimit(connLimit);
        realServerSpec.setFeatures(features);

        // Create multiple channels using the same provider
        EmbeddedChannel channel1 = new EmbeddedChannel();
        EmbeddedChannel channel2 = new EmbeddedChannel();
        EmbeddedChannel channel3 = new EmbeddedChannel();

        provider.configure(channel1.pipeline(), realServerSpec);
        provider.configure(channel2.pipeline(), realServerSpec);
        provider.configure(channel3.pipeline(), realServerSpec);

        // Activate first two channels
        channel1.pipeline().fireChannelActive();
        channel2.pipeline().fireChannelActive();

        // Third channel should be rejected
        channel3.pipeline().fireChannelActive();

        // Channel 3 should be closed
        assertThat(channel3.isOpen()).isFalse();

        channel1.close();
        channel2.close();
    }

    @Test
    void connectionLimitHandler_decrementsOnChannelInactive() {
        ServerSpec realServerSpec = new ServerSpec();
        realServerSpec.setName("test-server");
        FeaturesSpec features = new FeaturesSpec();
        ConnectionLimitSpec connLimit = new ConnectionLimitSpec();
        connLimit.setEnabled(true);
        connLimit.setMaxConnections(2);
        features.setConnectionLimit(connLimit);
        realServerSpec.setFeatures(features);

        EmbeddedChannel channel1 = new EmbeddedChannel();
        EmbeddedChannel channel2 = new EmbeddedChannel();
        EmbeddedChannel channel3 = new EmbeddedChannel();

        provider.configure(channel1.pipeline(), realServerSpec);
        provider.configure(channel2.pipeline(), realServerSpec);

        channel1.pipeline().fireChannelActive();
        channel2.pipeline().fireChannelActive();

        // Close channel1
        channel1.pipeline().fireChannelInactive();

        // Now channel3 should be able to connect
        provider.configure(channel3.pipeline(), realServerSpec);
        channel3.pipeline().fireChannelActive();

        assertThat(channel3.isActive()).isTrue();

        channel1.close();
        channel2.close();
        channel3.close();
    }

    @Test
    void connectionLimitHandler_rejectedConnectionDoesNotDoubleDecrement() {
        // This test verifies the fix for double-decrement bug:
        // When a connection is rejected due to limit, channelInactive should NOT decrement again
        ConnectionLimitFeatureProvider.ConnectionLimitHandler handler =
                new ConnectionLimitFeatureProvider.ConnectionLimitHandler(2);

        // Create and activate two channels to fill the limit
        EmbeddedChannel channel1 = new EmbeddedChannel(handler);
        EmbeddedChannel channel2 = new EmbeddedChannel(handler);

        assertThat(handler.getCurrentConnections()).isEqualTo(2);

        // Create a third channel that will be rejected
        EmbeddedChannel channel3 = new EmbeddedChannel(handler);

        // Channel3 should be closed due to limit exceeded
        assertThat(channel3.isOpen()).isFalse();

        // Connection count should still be 2, not negative
        // Before fix: count would be 1 (double decrement: once in channelActive, once in channelInactive)
        // After fix: count should be 2
        assertThat(handler.getCurrentConnections()).isEqualTo(2);

        // Close channel1 and channel2
        channel1.close();
        channel2.close();

        // Count should be 0, not negative
        assertThat(handler.getCurrentConnections()).isEqualTo(0);
    }

    @Test
    void connectionLimitHandler_multipleRejectedConnectionsDoNotCauseNegativeCount() {
        // Test multiple rejected connections to ensure count never goes negative
        ConnectionLimitFeatureProvider.ConnectionLimitHandler handler =
                new ConnectionLimitFeatureProvider.ConnectionLimitHandler(1);

        // Fill the limit with one connection
        EmbeddedChannel channel1 = new EmbeddedChannel(handler);
        assertThat(handler.getCurrentConnections()).isEqualTo(1);

        // Try to connect 10 more channels, all should be rejected
        for (int i = 0; i < 10; i++) {
            EmbeddedChannel rejectedChannel = new EmbeddedChannel(handler);
            assertThat(rejectedChannel.isOpen()).isFalse();
        }

        // Connection count should still be 1, not negative
        assertThat(handler.getCurrentConnections()).isEqualTo(1);

        // Close the original channel
        channel1.close();

        assertThat(handler.getCurrentConnections()).isEqualTo(0);
    }

    @Test
    void connectionLimitHandler_newConnectionsAllowedAfterRejectionAndClose() {
        // Verify that after rejecting connections and then closing active ones,
        // new connections can be accepted again
        ConnectionLimitFeatureProvider.ConnectionLimitHandler handler =
                new ConnectionLimitFeatureProvider.ConnectionLimitHandler(2);

        EmbeddedChannel channel1 = new EmbeddedChannel(handler);
        EmbeddedChannel channel2 = new EmbeddedChannel(handler);

        // Reject one
        EmbeddedChannel rejected = new EmbeddedChannel(handler);
        assertThat(rejected.isOpen()).isFalse();

        // Count should be 2
        assertThat(handler.getCurrentConnections()).isEqualTo(2);

        // Close one active connection
        channel1.close();

        // Now a new connection should be accepted
        EmbeddedChannel channel3 = new EmbeddedChannel(handler);
        assertThat(channel3.isOpen()).isTrue();
        assertThat(handler.getCurrentConnections()).isEqualTo(2);

        channel2.close();
        channel3.close();
    }
}
