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

package com.childrengreens.netty.spring.boot.context.server;

import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import com.childrengreens.netty.spring.boot.context.properties.ShutdownSpec;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ServerRuntime}.
 */
class ServerRuntimeTest {

    private ServerSpec spec;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel bindChannel;
    private ShutdownSpec shutdownSpec;

    @BeforeEach
    void setUp() {
        spec = mock(ServerSpec.class);
        bossGroup = mock(EventLoopGroup.class);
        workerGroup = mock(EventLoopGroup.class);
        bindChannel = mock(Channel.class);
        shutdownSpec = new ShutdownSpec();

        when(spec.getName()).thenReturn("testServer");
    }

    @Test
    void constructor_initializesFields() {
        ServerRuntime runtime = new ServerRuntime(spec, bossGroup, workerGroup, bindChannel, ServerState.RUNNING);

        assertThat(runtime.getSpec()).isSameAs(spec);
        assertThat(runtime.getBossGroup()).isSameAs(bossGroup);
        assertThat(runtime.getWorkerGroup()).isSameAs(workerGroup);
        assertThat(runtime.getBindChannel()).isSameAs(bindChannel);
        assertThat(runtime.getState()).isEqualTo(ServerState.RUNNING);
    }

    @Test
    void isRunning_whenRunning_returnsTrue() {
        ServerRuntime runtime = new ServerRuntime(spec, bossGroup, workerGroup, bindChannel, ServerState.RUNNING);

        assertThat(runtime.isRunning()).isTrue();
    }

    @Test
    void isRunning_whenNotRunning_returnsFalse() {
        ServerRuntime runtime = new ServerRuntime(spec, bossGroup, workerGroup, bindChannel, ServerState.STOPPED);

        assertThat(runtime.isRunning()).isFalse();
    }

    @Test
    void setState_changesState() {
        ServerRuntime runtime = new ServerRuntime(spec, bossGroup, workerGroup, bindChannel, ServerState.RUNNING);

        runtime.setState(ServerState.STOPPING);

        assertThat(runtime.getState()).isEqualTo(ServerState.STOPPING);
    }

    @Test
    void stop_whenAlreadyStopped_doesNothing() throws Exception {
        ServerRuntime runtime = new ServerRuntime(spec, bossGroup, workerGroup, bindChannel, ServerState.STOPPED);

        runtime.stop(shutdownSpec);

        verify(bindChannel, never()).close();
        assertThat(runtime.getState()).isEqualTo(ServerState.STOPPED);
    }

    @Test
    void stop_whenStopping_doesNothing() throws Exception {
        ServerRuntime runtime = new ServerRuntime(spec, bossGroup, workerGroup, bindChannel, ServerState.STOPPING);

        runtime.stop(shutdownSpec);

        verify(bindChannel, never()).close();
        assertThat(runtime.getState()).isEqualTo(ServerState.STOPPING);
    }

    @Test
    void stop_closesChannelAndShutdownGroups() throws Exception {
        ChannelFuture channelFuture = mock(ChannelFuture.class);
        @SuppressWarnings("rawtypes")
        Future workerFuture = mock(Future.class);
        @SuppressWarnings("rawtypes")
        Future bossFuture = mock(Future.class);
        when(bindChannel.isOpen()).thenReturn(true);
        when(bindChannel.close()).thenReturn(channelFuture);
        when(channelFuture.sync()).thenReturn(channelFuture);
        when(workerGroup.isShutdown()).thenReturn(false);
        when(bossGroup.isShutdown()).thenReturn(false);
        Mockito.<Future<?>>when(workerGroup.shutdownGracefully(anyLong(), anyLong(), any()))
                .thenReturn((Future<?>) workerFuture);
        Mockito.<Future<?>>when(bossGroup.shutdownGracefully(anyLong(), anyLong(), any()))
                .thenReturn((Future<?>) bossFuture);
        when(workerFuture.sync()).thenReturn(workerFuture);
        when(bossFuture.sync()).thenReturn(bossFuture);

        ServerRuntime runtime = new ServerRuntime(spec, bossGroup, workerGroup, bindChannel, ServerState.RUNNING);

        runtime.stop(shutdownSpec);

        verify(bindChannel).close();
        verify(workerGroup).shutdownGracefully(anyLong(), anyLong(), any());
        verify(bossGroup).shutdownGracefully(anyLong(), anyLong(), any());
        verify(workerFuture).sync();
        verify(bossFuture).sync();
        assertThat(runtime.getState()).isEqualTo(ServerState.STOPPED);
    }

    @Test
    void stop_withNullGroups_handlesGracefully() throws Exception {
        ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(bindChannel.isOpen()).thenReturn(true);
        when(bindChannel.close()).thenReturn(channelFuture);
        when(channelFuture.sync()).thenReturn(channelFuture);

        ServerRuntime runtime = new ServerRuntime(spec, null, null, bindChannel, ServerState.RUNNING);

        runtime.stop(shutdownSpec);

        assertThat(runtime.getState()).isEqualTo(ServerState.STOPPED);
    }

    @Test
    void stop_withClosedChannel_doesNotTryToCloseAgain() throws Exception {
        when(bindChannel.isOpen()).thenReturn(false);
        when(workerGroup.isShutdown()).thenReturn(false);
        when(bossGroup.isShutdown()).thenReturn(false);
        @SuppressWarnings("rawtypes")
        Future workerFuture = mock(Future.class);
        @SuppressWarnings("rawtypes")
        Future bossFuture = mock(Future.class);
        Mockito.<Future<?>>when(workerGroup.shutdownGracefully(anyLong(), anyLong(), any()))
                .thenReturn((Future<?>) workerFuture);
        Mockito.<Future<?>>when(bossGroup.shutdownGracefully(anyLong(), anyLong(), any()))
                .thenReturn((Future<?>) bossFuture);
        when(workerFuture.sync()).thenReturn(workerFuture);
        when(bossFuture.sync()).thenReturn(bossFuture);

        ServerRuntime runtime = new ServerRuntime(spec, bossGroup, workerGroup, bindChannel, ServerState.RUNNING);

        runtime.stop(shutdownSpec);

        verify(bindChannel, never()).close();
        verify(workerFuture).sync();
        verify(bossFuture).sync();
        assertThat(runtime.getState()).isEqualTo(ServerState.STOPPED);
    }

    @Test
    void stop_withGracefulDisabled_usesZeroQuietPeriod() throws Exception {
        ChannelFuture channelFuture = mock(ChannelFuture.class);
        @SuppressWarnings("rawtypes")
        Future workerFuture = mock(Future.class);
        @SuppressWarnings("rawtypes")
        Future bossFuture = mock(Future.class);
        when(bindChannel.isOpen()).thenReturn(true);
        when(bindChannel.close()).thenReturn(channelFuture);
        when(channelFuture.sync()).thenReturn(channelFuture);
        when(workerGroup.isShutdown()).thenReturn(false);
        when(bossGroup.isShutdown()).thenReturn(false);
        Mockito.<Future<?>>when(workerGroup.shutdownGracefully(anyLong(), anyLong(), any()))
                .thenReturn((Future<?>) workerFuture);
        Mockito.<Future<?>>when(bossGroup.shutdownGracefully(anyLong(), anyLong(), any()))
                .thenReturn((Future<?>) bossFuture);
        when(workerFuture.sync()).thenReturn(workerFuture);
        when(bossFuture.sync()).thenReturn(bossFuture);

        shutdownSpec.setGraceful(false);

        ServerRuntime runtime = new ServerRuntime(spec, bossGroup, workerGroup, bindChannel, ServerState.RUNNING);

        runtime.stop(shutdownSpec);

        verify(workerGroup).shutdownGracefully(eq(0L), anyLong(), any());
        verify(bossGroup).shutdownGracefully(eq(0L), anyLong(), any());
        verify(workerFuture).sync();
        verify(bossFuture).sync();
    }

    @Test
    void stop_whenInterrupted_setsFailedAndRestoresInterruptFlag() throws Exception {
        ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(bindChannel.isOpen()).thenReturn(true);
        when(bindChannel.close()).thenReturn(channelFuture);
        when(channelFuture.sync()).thenThrow(new InterruptedException("interrupted"));

        ServerRuntime runtime = new ServerRuntime(spec, bossGroup, workerGroup, bindChannel, ServerState.RUNNING);

        runtime.stop(shutdownSpec);

        assertThat(runtime.getState()).isEqualTo(ServerState.FAILED);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
    }
}
