/*
 * Copyright (c) 2014 SVT AB.
 *
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

package se.svt.helios.serviceregistration.consul;

import com.spotify.helios.serviceregistration.ServiceRegistrar;
import com.spotify.helios.serviceregistration.ServiceRegistrarFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.svt.helios.serviceregistration.consul.model.RegistrarConfig;

import java.net.URI;
import java.net.URISyntaxException;

public class ConsulServiceRegistrarFactory implements ServiceRegistrarFactory {
    private static final Logger log =
        LoggerFactory.getLogger(ConsulServiceRegistrarFactory.class);

    static final String PROP_HEALTH_CHECK_INTERVAL = "helios-consul.healthCheckInterval";
    static final String PROP_DEPLOY_TAG = "helios-consul.deployTag";
    static final String PROP_SYNC_INTERVAL = "helios-consul.syncInterval";

    @Override
    public ServiceRegistrar create(final String consulUri) {

        if (consulUri == null || consulUri.isEmpty()) {
            throw new RuntimeException("Empty connect string!");
        }

        final URI uri;

        try {
            uri = new URI(consulUri);
        } catch (URISyntaxException e) {
            throw new RuntimeException("consulUri is not a proper url", e);
        }

        log.info("Creating new ConsulServiceRegistrar: consulUri={}", uri.toString());

        final ConsulClient consulClient = new ConsulClient(uri.toString());
        final RegistrarConfig config = createConfig();

        return new ConsulServiceRegistrar(consulClient, config);
    }

    @Override
    public ServiceRegistrar createForDomain(String s) {
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static RegistrarConfig createConfig() {
        // Default values
        final int healthCheckInterval;
        final int syncInterval;
        final String deployTag;

        try {
            healthCheckInterval = Integer.parseInt(
                    System.getProperty(PROP_HEALTH_CHECK_INTERVAL, "10")
            );
            syncInterval = Integer.parseInt(
                    System.getProperty(PROP_SYNC_INTERVAL, "30")
            );
        } catch (NumberFormatException e) {
            throw new RuntimeException("Could not parse config", e);
        }

        deployTag = System.getProperty(PROP_DEPLOY_TAG, "helios-deployed");
        if (deployTag.isEmpty()) {
            throw new RuntimeException("Could not parse config: deployTag can not be empty!");
        }

        return new RegistrarConfig(syncInterval, healthCheckInterval, deployTag);
    }
}
