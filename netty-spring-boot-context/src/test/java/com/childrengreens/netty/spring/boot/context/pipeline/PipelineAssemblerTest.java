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

package com.childrengreens.netty.spring.boot.context.pipeline;

import com.childrengreens.netty.spring.boot.context.codec.CodecRegistry;
import com.childrengreens.netty.spring.boot.context.codec.JsonNettyCodec;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.dispatch.Dispatcher;
import com.childrengreens.netty.spring.boot.context.feature.FeatureProvider;
import com.childrengreens.netty.spring.boot.context.feature.FeatureRegistry;
import com.childrengreens.netty.spring.boot.context.metrics.ServerMetrics;
import com.childrengreens.netty.spring.boot.context.profile.ProfileRegistry;
import com.childrengreens.netty.spring.boot.context.profile.TcpLengthFieldJsonProfile;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import com.childrengreens.netty.spring.boot.context.routing.Router;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PipelineAssembler}.
 */
class PipelineAssemblerTest {

    private ProfileRegistry profileRegistry;
    private FeatureRegistry featureRegistry;
    private Dispatcher dispatcher;
    private CodecRegistry codecRegistry;
    private PipelineAssembler assembler;

    @BeforeEach
    void setUp() {
        profileRegistry = new ProfileRegistry();
        profileRegistry.register(new TcpLengthFieldJsonProfile());

        featureRegistry = new FeatureRegistry();

        Router router = new Router();
        codecRegistry = new CodecRegistry();
        codecRegistry.register(new JsonNettyCodec());

        dispatcher = new Dispatcher(router, codecRegistry);

        assembler = new PipelineAssembler(
                profileRegistry,
                featureRegistry,
                dispatcher,
                codecRegistry,
                Collections.emptyList()
        );
    }

    @Test
    void assemble_withValidProfile_configuresPipeline() {
        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        EmbeddedChannel channel = new EmbeddedChannel();

        assembler.assemble(channel.pipeline(), serverSpec);

        assertThat(channel.pipeline().names()).contains("dispatcherHandler", "exceptionHandler");

        channel.close();
    }

    @Test
    void assemble_withConfigurers_appliesConfigurers() {
        NettyPipelineConfigurer configurer = new NettyPipelineConfigurer() {
            @Override
            public void configure(io.netty.channel.ChannelPipeline pipeline, ServerSpec serverSpec) {
                pipeline.addLast("customHandler", new io.netty.channel.ChannelInboundHandlerAdapter());
            }

            @Override
            public boolean supports(ServerSpec serverSpec) {
                return NettyPipelineConfigurer.super.supports(serverSpec);
            }
        };

        assembler = new PipelineAssembler(
                profileRegistry,
                featureRegistry,
                dispatcher,
                codecRegistry,
                Collections.singletonList(configurer)
        );

        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        EmbeddedChannel channel = new EmbeddedChannel();

        assembler.assemble(channel.pipeline(), serverSpec);

        assertThat(channel.pipeline().names()).contains("customHandler");

        channel.close();
    }

    @Test
    void assemble_withConfigurerThatDoesNotSupport_skipsConfigurer() {
        NettyPipelineConfigurer configurer = new NettyPipelineConfigurer() {
            @Override
            public void configure(io.netty.channel.ChannelPipeline pipeline, ServerSpec serverSpec) {
                pipeline.addLast("customHandler", new io.netty.channel.ChannelInboundHandlerAdapter());
            }

            @Override
            public boolean supports(ServerSpec serverSpec) {
                return false;
            }
        };

        assembler = new PipelineAssembler(
                profileRegistry,
                featureRegistry,
                dispatcher,
                codecRegistry,
                Collections.singletonList(configurer)
        );

        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        EmbeddedChannel channel = new EmbeddedChannel();

        assembler.assemble(channel.pipeline(), serverSpec);

        assertThat(channel.pipeline().names()).doesNotContain("customHandler");

        channel.close();
    }

    // ==================== Tests for applyFeatures (lines 136-145) ====================

    @Test
    void assemble_withEnabledFeature_appliesFeature() {
        // Create a test feature with order < 200 (applied before profile)
        AtomicBoolean featureApplied = new AtomicBoolean(false);
        FeatureProvider testFeature = new FeatureProvider() {
            @Override
            public String getName() {
                return "test-feature";
            }

            @Override
            public int getOrder() {
                return 50; // Low order, applied before profile
            }

            @Override
            public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
                featureApplied.set(true);
                pipeline.addLast("testFeatureHandler", new io.netty.channel.ChannelInboundHandlerAdapter());
            }

            @Override
            public boolean isEnabled(ServerSpec serverSpec) {
                return true;
            }
        };

        featureRegistry.register(testFeature);

        assembler = new PipelineAssembler(
                profileRegistry,
                featureRegistry,
                dispatcher,
                codecRegistry,
                Collections.emptyList()
        );

        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        EmbeddedChannel channel = new EmbeddedChannel();

        assembler.assemble(channel.pipeline(), serverSpec);

        assertThat(featureApplied.get()).isTrue();
        assertThat(channel.pipeline().names()).contains("testFeatureHandler");

        channel.close();
    }

    @Test
    void assemble_withDisabledFeature_skipsFeature() {
        // Create a test feature that is disabled
        AtomicBoolean featureApplied = new AtomicBoolean(false);
        FeatureProvider testFeature = new FeatureProvider() {
            @Override
            public String getName() {
                return "disabled-feature";
            }

            @Override
            public int getOrder() {
                return 50;
            }

            @Override
            public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
                featureApplied.set(true);
                pipeline.addLast("disabledFeatureHandler", new io.netty.channel.ChannelInboundHandlerAdapter());
            }

            @Override
            public boolean isEnabled(ServerSpec serverSpec) {
                return false; // Disabled
            }
        };

        featureRegistry.register(testFeature);

        assembler = new PipelineAssembler(
                profileRegistry,
                featureRegistry,
                dispatcher,
                codecRegistry,
                Collections.emptyList()
        );

        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        EmbeddedChannel channel = new EmbeddedChannel();

        assembler.assemble(channel.pipeline(), serverSpec);

        assertThat(featureApplied.get()).isFalse();
        assertThat(channel.pipeline().names()).doesNotContain("disabledFeatureHandler");

        channel.close();
    }

    @Test
    void assemble_withHighOrderFeature_appliesAfterProfile() {
        // Create a feature with order >= 200 (applied after profile)
        AtomicBoolean featureApplied = new AtomicBoolean(false);
        FeatureProvider highOrderFeature = new FeatureProvider() {
            @Override
            public String getName() {
                return "high-order-feature";
            }

            @Override
            public int getOrder() {
                return 300; // High order, applied after profile
            }

            @Override
            public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
                featureApplied.set(true);
                pipeline.addLast("highOrderHandler", new io.netty.channel.ChannelInboundHandlerAdapter());
            }

            @Override
            public boolean isEnabled(ServerSpec serverSpec) {
                return true;
            }
        };

        featureRegistry.register(highOrderFeature);

        assembler = new PipelineAssembler(
                profileRegistry,
                featureRegistry,
                dispatcher,
                codecRegistry,
                Collections.emptyList()
        );

        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        EmbeddedChannel channel = new EmbeddedChannel();

        assembler.assemble(channel.pipeline(), serverSpec);

        assertThat(featureApplied.get()).isTrue();
        assertThat(channel.pipeline().names()).contains("highOrderHandler");

        channel.close();
    }

    @Test
    void assemble_withMultipleFeatures_appliesInOrderRange() {
        // Feature with order 50 (low, before profile)
        AtomicBoolean lowOrderApplied = new AtomicBoolean(false);
        FeatureProvider lowOrderFeature = new FeatureProvider() {
            @Override
            public String getName() {
                return "low-order";
            }

            @Override
            public int getOrder() {
                return 50;
            }

            @Override
            public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
                lowOrderApplied.set(true);
            }

            @Override
            public boolean isEnabled(ServerSpec serverSpec) {
                return true;
            }
        };

        // Feature with order 250 (high, after profile)
        AtomicBoolean highOrderApplied = new AtomicBoolean(false);
        FeatureProvider highOrderFeature = new FeatureProvider() {
            @Override
            public String getName() {
                return "high-order";
            }

            @Override
            public int getOrder() {
                return 250;
            }

            @Override
            public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
                highOrderApplied.set(true);
            }

            @Override
            public boolean isEnabled(ServerSpec serverSpec) {
                return true;
            }
        };

        featureRegistry.register(lowOrderFeature);
        featureRegistry.register(highOrderFeature);

        assembler = new PipelineAssembler(
                profileRegistry,
                featureRegistry,
                dispatcher,
                codecRegistry,
                Collections.emptyList()
        );

        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        EmbeddedChannel channel = new EmbeddedChannel();

        assembler.assemble(channel.pipeline(), serverSpec);

        // Both features should be applied (low before profile, high after)
        assertThat(lowOrderApplied.get()).isTrue();
        assertThat(highOrderApplied.get()).isTrue();

        channel.close();
    }

    // ==================== Tests for ServerMetrics support ====================

    @Test
    void assemble_withServerMetrics_setsMetricsAttribute() {
        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        ServerMetrics serverMetrics = new ServerMetrics("test-server");
        EmbeddedChannel channel = new EmbeddedChannel();

        assembler.assemble(channel.pipeline(), serverSpec, serverMetrics);

        // Verify metrics attribute is set
        ServerMetrics storedMetrics = channel.attr(NettyContext.SERVER_METRICS_KEY).get();
        assertThat(storedMetrics).isSameAs(serverMetrics);

        channel.close();
    }

    @Test
    void assemble_withNullServerMetrics_doesNotSetMetricsAttribute() {
        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        EmbeddedChannel channel = new EmbeddedChannel();

        assembler.assemble(channel.pipeline(), serverSpec, null);

        // Verify metrics attribute is not set
        ServerMetrics storedMetrics = channel.attr(NettyContext.SERVER_METRICS_KEY).get();
        assertThat(storedMetrics).isNull();

        channel.close();
    }

    @Test
    void assemble_withoutServerMetrics_callsOverloadedMethod() {
        ServerSpec serverSpec = new ServerSpec();
        serverSpec.setName("test-server");
        serverSpec.setProfile("tcp-lengthfield-json");

        EmbeddedChannel channel = new EmbeddedChannel();

        // Call without serverMetrics argument
        assembler.assemble(channel.pipeline(), serverSpec);

        // Pipeline should still be configured
        assertThat(channel.pipeline().names()).contains("dispatcherHandler", "exceptionHandler");

        channel.close();
    }

}
