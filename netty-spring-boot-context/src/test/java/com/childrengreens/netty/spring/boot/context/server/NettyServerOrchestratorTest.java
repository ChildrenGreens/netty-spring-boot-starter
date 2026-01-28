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

import com.childrengreens.netty.spring.boot.context.pipeline.PipelineAssembler;
import com.childrengreens.netty.spring.boot.context.properties.NettyProperties;
import com.childrengreens.netty.spring.boot.context.properties.TransportImpl;
import com.childrengreens.netty.spring.boot.context.transport.TransportFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NettyServerOrchestrator}.
 */
class NettyServerOrchestratorTest {

    private NettyProperties properties;
    private TransportFactory transportFactory;
    private PipelineAssembler pipelineAssembler;
    private NettyServerOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        properties = new NettyProperties();
        transportFactory = new TransportFactory(TransportImpl.NIO);
        pipelineAssembler = mock(PipelineAssembler.class);

        orchestrator = new NettyServerOrchestrator(properties, transportFactory, pipelineAssembler);
    }

    @Test
    void start_whenDisabled_doesNotStart() {
        properties.setEnabled(false);

        orchestrator.start();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void start_whenNoServers_doesNotStart() {
        properties.setEnabled(true);
        properties.setServers(Collections.emptyList());

        orchestrator.start();

        assertThat(orchestrator.getAllRuntimes()).isEmpty();
    }

    @Test
    void getRuntime_whenNotExists_returnsNull() {
        ServerRuntime runtime = orchestrator.getRuntime("non-existent");

        assertThat(runtime).isNull();
    }

    @Test
    void getAllRuntimes_initiallyEmpty() {
        Map<String, ServerRuntime> runtimes = orchestrator.getAllRuntimes();

        assertThat(runtimes).isEmpty();
    }

    @Test
    void setFailFast_setsValue() {
        orchestrator.setFailFast(false);
        // No exception thrown, value is set internally
    }

    @Test
    void stop_whenNoServers_doesNotThrow() {
        orchestrator.stop();
        // No exception thrown
    }

}
