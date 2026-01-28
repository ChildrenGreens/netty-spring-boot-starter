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

import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.codec.JsonNettyCodec;
import com.childrengreens.netty.spring.boot.context.properties.NettyProperties;
import com.childrengreens.netty.spring.boot.context.properties.TransportImpl;
import com.childrengreens.netty.spring.boot.context.transport.TransportFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NettyClientOrchestrator}.
 */
class NettyClientOrchestratorTest {

    private NettyProperties properties;
    private TransportFactory transportFactory;
    private ClientPipelineAssembler pipelineAssembler;
    private CodecRegistry codecRegistry;
    private NettyClientOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        properties = new NettyProperties();
        transportFactory = new TransportFactory(TransportImpl.NIO);

        ClientProfileRegistry profileRegistry = new ClientProfileRegistry();
        profileRegistry.register(new TcpLengthFieldJsonClientProfile());

        codecRegistry = new CodecRegistry();
        codecRegistry.register(new JsonNettyCodec());

        pipelineAssembler = new ClientPipelineAssembler(profileRegistry, codecRegistry);

        orchestrator = new NettyClientOrchestrator(properties, transportFactory, pipelineAssembler, codecRegistry);
    }

    @Test
    void start_whenDisabled_doesNotStart() {
        properties.setEnabled(false);

        orchestrator.start();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void start_whenNoClients_doesNotStart() {
        properties.setEnabled(true);
        properties.setClients(Collections.emptyList());

        orchestrator.start();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void getRuntime_whenNotExists_returnsNull() {
        ClientRuntime runtime = orchestrator.getRuntime("non-existent");

        assertThat(runtime).isNull();
    }

    @Test
    void getAllRuntimes_initiallyEmpty() {
        Map<String, ClientRuntime> runtimes = orchestrator.getAllRuntimes();

        assertThat(runtimes).isEmpty();
    }

    @Test
    void setFailFast_setsValue() {
        orchestrator.setFailFast(false);
        // No exception thrown, value is set internally
    }

    @Test
    void stop_whenNoClients_doesNotThrow() {
        orchestrator.stop();
        // No exception thrown
    }

}
