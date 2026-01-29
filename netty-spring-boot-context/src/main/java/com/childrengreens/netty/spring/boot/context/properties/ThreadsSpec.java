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

package com.childrengreens.netty.spring.boot.context.properties;

/**
 * Thread pool configuration for Netty event loop groups.
 *
 * @author ChildrenGreens
 * @since 0.0.1
 */
public class ThreadsSpec {

    /**
     * Number of boss threads for accepting connections.
     * <p>Default is 1, which is sufficient for most use cases.
     */
    private int boss = 1;

    /**
     * Number of worker threads for handling I/O operations.
     * <p>Default is 0, which means Netty will use {@code CPU cores * 2}.
     */
    private int worker = 0;

    /**
     * Return the number of boss threads.
     * @return the boss thread count
     */
    public int getBoss() {
        return this.boss;
    }

    /**
     * Set the number of boss threads.
     * @param boss the boss thread count
     */
    public void setBoss(int boss) {
        this.boss = boss;
    }

    /**
     * Return the number of worker threads.
     * @return the worker thread count, 0 for default
     */
    public int getWorker() {
        return this.worker;
    }

    /**
     * Set the number of worker threads.
     * @param worker the worker thread count, 0 for default
     */
    public void setWorker(int worker) {
        this.worker = worker;
    }

}
