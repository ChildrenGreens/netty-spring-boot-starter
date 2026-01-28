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

import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NettyPipelineConfigurer} interface default methods.
 */
class NettyPipelineConfigurerTest {

    @Test
    void supports_returnsTrue_byDefault() {
        NettyPipelineConfigurer configurer = (pipeline, spec) -> {};
        ServerSpec serverSpec = new ServerSpec();

        assertThat(configurer.supports(serverSpec)).isTrue();
    }

    @Test
    void getOrder_returnsZero_byDefault() {
        NettyPipelineConfigurer configurer = (pipeline, spec) -> {};

        assertThat(configurer.getOrder()).isEqualTo(0);
    }

    @Test
    void configure_isCalled() {
        boolean[] called = {false};
        NettyPipelineConfigurer configurer = (pipeline, spec) -> called[0] = true;
        ChannelPipeline pipeline = mock(ChannelPipeline.class);
        ServerSpec serverSpec = new ServerSpec();

        configurer.configure(pipeline, serverSpec);

        assertThat(called[0]).isTrue();
    }

    @Test
    void supports_withCustomImplementation_canReturnFalse() {
        NettyPipelineConfigurer configurer = new NettyPipelineConfigurer() {
            @Override
            public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
                // No-op
            }

            @Override
            public boolean supports(ServerSpec serverSpec) {
                return "specific-server".equals(serverSpec.getName());
            }
        };

        ServerSpec matchingSpec = new ServerSpec();
        matchingSpec.setName("specific-server");

        ServerSpec nonMatchingSpec = new ServerSpec();
        nonMatchingSpec.setName("other-server");

        assertThat(configurer.supports(matchingSpec)).isTrue();
        assertThat(configurer.supports(nonMatchingSpec)).isFalse();
    }

    @Test
    void getOrder_withCustomImplementation_canReturnNonZero() {
        NettyPipelineConfigurer configurer = new NettyPipelineConfigurer() {
            @Override
            public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
                // No-op
            }

            @Override
            public int getOrder() {
                return 100;
            }
        };

        assertThat(configurer.getOrder()).isEqualTo(100);
    }
}
