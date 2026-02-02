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

import com.childrengreens.netty.spring.boot.context.auth.*;
import com.childrengreens.netty.spring.boot.context.context.NettyContext;
import com.childrengreens.netty.spring.boot.context.handler.AuthHandler;
import com.childrengreens.netty.spring.boot.context.properties.ServerSpec;
import io.netty.channel.ChannelPipeline;

/**
 * Feature provider for authentication.
 *
 * <p>Adds {@link AuthHandler} to the pipeline for authentication handling.
 * This feature should be placed after codec handlers so it can inspect
 * decoded messages for credential authentication.
 *
 * @author ChildrenGreens
 * @since 0.0.2
 */
public class AuthFeatureProvider implements FeatureProvider {

    /**
     * Feature name constant.
     */
    public static final String NAME = "auth";

    /**
     * Order for this feature - after codec, before dispatcher.
     */
    public static final int ORDER = 180;

    private final Authenticator authenticator;
    private final ConnectionManager connectionManager;

    /**
     * Create a new AuthFeatureProvider.
     *
     * @param authenticator the authenticator to use
     */
    public AuthFeatureProvider(Authenticator authenticator) {
        this(authenticator, new ConnectionManager());
    }

    /**
     * Create a new AuthFeatureProvider with a custom connection manager.
     *
     * @param authenticator the authenticator to use
     * @param connectionManager the connection manager to use
     */
    public AuthFeatureProvider(Authenticator authenticator, ConnectionManager connectionManager) {
        this.authenticator = authenticator;
        this.connectionManager = connectionManager;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void configure(ChannelPipeline pipeline, ServerSpec serverSpec) {
        AuthSpec spec = serverSpec.getFeatures().getAuth();
        if (spec == null || !spec.isEnabled()) {
            return;
        }

        // Get or create auth metrics
        AuthMetrics authMetrics = null;
        if (spec.isMetrics()) {
            authMetrics = pipeline.channel().attr(NettyContext.AUTH_METRICS_KEY).get();
            if (authMetrics == null) {
                authMetrics = new AuthMetrics(serverSpec.getName());
                pipeline.channel().attr(NettyContext.AUTH_METRICS_KEY).set(authMetrics);
            }
        }

        // Add handler
        pipeline.addLast("authHandler", new AuthHandler(spec, authenticator, connectionManager, authMetrics));
    }

    @Override
    public boolean isEnabled(ServerSpec serverSpec) {
        AuthSpec spec = serverSpec.getFeatures().getAuth();
        return spec != null && spec.isEnabled();
    }

    /**
     * Get the connection manager.
     *
     * @return the connection manager
     */
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

}
