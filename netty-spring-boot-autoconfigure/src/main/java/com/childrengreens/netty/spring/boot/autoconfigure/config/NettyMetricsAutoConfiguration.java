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

package com.childrengreens.netty.spring.boot.autoconfigure.config;

import com.childrengreens.netty.spring.boot.actuator.metrics.NettyMetricsBinder;
import com.childrengreens.netty.spring.boot.context.client.NettyClientOrchestrator;
import com.childrengreens.netty.spring.boot.context.server.NettyServerOrchestrator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Netty metrics with Micrometer.
 *
 * <p>This configuration provides the {@link NettyMetricsBinder} bean which registers
 * Netty server and client metrics with the Micrometer registry.
 *
 * <p>This configuration is activated when:
 * <ul>
 * <li>Micrometer is on the classpath</li>
 * <li>At least one orchestrator (server or client) is available</li>
 * </ul>
 *
 * @author ChildrenGreens
 * @since 0.0.2
 * @see NettyMetricsBinder
 */
@AutoConfiguration(after = {NettyServerAutoConfiguration.class, NettyClientAutoConfiguration.class})
@ConditionalOnClass({MeterRegistry.class, NettyMetricsBinder.class})
public class NettyMetricsAutoConfiguration {

    /**
     * Create the Netty metrics binder.
     * @param serverOrchestrator the server orchestrator (optional)
     * @param clientOrchestrator the client orchestrator (optional)
     * @return the metrics binder
     */
    @Bean
    @ConditionalOnMissingBean
    public NettyMetricsBinder nettyMetricsBinder(
            ObjectProvider<NettyServerOrchestrator> serverOrchestrator,
            ObjectProvider<NettyClientOrchestrator> clientOrchestrator) {
        NettyServerOrchestrator server = serverOrchestrator.getIfAvailable();
        NettyClientOrchestrator client = clientOrchestrator.getIfAvailable();
        if (server == null && client == null) {
            return null;
        }
        return new NettyMetricsBinder(server, client);
    }

}
