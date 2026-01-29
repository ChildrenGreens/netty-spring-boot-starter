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

package com.childrengreens.netty.spring.boot.context.annotation;

import com.childrengreens.netty.spring.boot.context.client.NettyClientsRegistrarImportSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables scanning for Netty client interfaces.
 *
 * <p>When this annotation is present on a configuration class, Spring will
 * scan for interfaces annotated with {@link NettyClient} and create proxies
 * for them.
 *
 * <p>Example:
 * <pre>{@code
 * @SpringBootApplication
 * @EnableNettyClients(basePackages = "com.example.clients")
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * }</pre>
 *
 * @author ChildrenGreens
 * @since 0.0.1
 * @see NettyClient
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(NettyClientsRegistrarImportSelector.class)
public @interface EnableNettyClients {

    /**
     * Alias for {@link #basePackages()}.
     * @return the base packages
     */
    String[] value() default {};

    /**
     * Base packages to scan for client interfaces.
     * <p>If not specified, scanning will start from the package of the
     * configuration class that declares this annotation.
     * @return the base packages
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()}.
     * <p>The package of each specified class will be scanned.
     * @return the base package classes
     */
    Class<?>[] basePackageClasses() default {};

}
