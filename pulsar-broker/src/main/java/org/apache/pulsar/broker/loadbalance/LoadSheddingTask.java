/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.broker.loadbalance;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pulsar.broker.ServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoadManager load shedding task.
 */
public class LoadSheddingTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(LoadSheddingTask.class);
    private final AtomicReference<LoadManager> loadManager;
    private final ScheduledExecutorService loadManagerExecutor;

    private final ServiceConfiguration config;

    private volatile boolean isCancel = false;

    public LoadSheddingTask(AtomicReference<LoadManager> loadManager,
                            ScheduledExecutorService loadManagerExecutor,
                            ServiceConfiguration config) {
        this.loadManager = loadManager;
        this.loadManagerExecutor = loadManagerExecutor;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            loadManager.get().doLoadShedding();
        } catch (Exception e) {
            LOG.warn("Error during the load shedding", e);
        } finally {
            if (!isCancel && loadManagerExecutor != null && config != null) {
                loadManagerExecutor.schedule(
                        new LoadSheddingTask(loadManager, loadManagerExecutor, config),
                        config.getLoadBalancerSheddingIntervalMinutes(),
                        TimeUnit.MINUTES);
            }
        }
    }

    public void start() {
        if (loadManagerExecutor != null && config != null) {
            loadManagerExecutor.schedule(
                    new LoadSheddingTask(loadManager, loadManagerExecutor, config),
                    config.getLoadBalancerSheddingIntervalMinutes(),
                    TimeUnit.MINUTES);
        }
    }

    public void cancel() {
        isCancel = true;
    }
}
