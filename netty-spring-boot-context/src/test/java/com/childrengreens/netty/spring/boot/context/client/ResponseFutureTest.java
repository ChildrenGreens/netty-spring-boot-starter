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

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ResponseFuture}.
 */
class ResponseFutureTest {

    @Test
    void constructor_setsPropertiesCorrectly() {
        ResponseFuture<String> future = new ResponseFuture<>("test-id", 5000);

        assertThat(future.getCorrelationId()).isEqualTo("test-id");
        assertThat(future.getTimeoutMs()).isEqualTo(5000);
        assertThat(future.getCreateTime()).isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(future.isDone()).isFalse();
    }

    @Test
    void complete_withValue_completesSuccessfully() throws Exception {
        ResponseFuture<String> future = new ResponseFuture<>("test-id", 5000);

        boolean completed = future.complete("result");

        assertThat(completed).isTrue();
        assertThat(future.isDone()).isTrue();
        assertThat(future.get()).isEqualTo("result");
    }

    @Test
    void complete_whenAlreadyCompleted_returnsFalse() {
        ResponseFuture<String> future = new ResponseFuture<>("test-id", 5000);

        future.complete("first");
        boolean secondComplete = future.complete("second");

        assertThat(secondComplete).isFalse();
    }

    @Test
    void completeExceptionally_withError_completesWithException() {
        ResponseFuture<String> future = new ResponseFuture<>("test-id", 5000);

        boolean completed = future.completeExceptionally(new RuntimeException("test error"));

        assertThat(completed).isTrue();
        assertThat(future.isDone()).isTrue();
        assertThatThrownBy(future::get)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("test error");
    }

    @Test
    void get_withTimeout_returnsValue() throws Exception {
        ResponseFuture<String> future = new ResponseFuture<>("test-id", 5000);
        future.complete("result");

        String result = future.get(1, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("result");
    }

    @Test
    void isExpired_whenNotExpired_returnsFalse() {
        ResponseFuture<String> future = new ResponseFuture<>("test-id", 60000);

        assertThat(future.isExpired()).isFalse();
    }

    @Test
    void isExpired_whenExpired_returnsTrue() throws InterruptedException {
        ResponseFuture<String> future = new ResponseFuture<>("test-id", 1);

        Thread.sleep(10);

        assertThat(future.isExpired()).isTrue();
    }

    @Test
    void cancelIfExpired_whenExpired_completesExceptionally() throws InterruptedException {
        ResponseFuture<String> future = new ResponseFuture<>("test-id", 1);

        Thread.sleep(10);

        boolean cancelled = future.cancelIfExpired();

        assertThat(cancelled).isTrue();
        assertThat(future.isDone()).isTrue();
        assertThatThrownBy(future::get)
                .hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void cancelIfExpired_whenNotExpired_returnsFalse() {
        ResponseFuture<String> future = new ResponseFuture<>("test-id", 60000);

        boolean cancelled = future.cancelIfExpired();

        assertThat(cancelled).isFalse();
        assertThat(future.isDone()).isFalse();
    }

    @Test
    void cancelIfExpired_whenAlreadyCompleted_returnsFalse() throws InterruptedException {
        ResponseFuture<String> future = new ResponseFuture<>("test-id", 1);
        future.complete("result");

        Thread.sleep(10);

        boolean cancelled = future.cancelIfExpired();

        assertThat(cancelled).isFalse();
    }

    @Test
    void toCompletableFuture_returnsFuture() {
        ResponseFuture<String> future = new ResponseFuture<>("test-id", 5000);

        CompletableFuture<String> cf = future.toCompletableFuture();

        assertThat(cf).isNotNull();
        assertThat(cf.isDone()).isFalse();

        future.complete("result");

        assertThat(cf.isDone()).isTrue();
    }

}
