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

import java.net.URI;
import java.net.URISyntaxException;

public class ConsulServiceRegistrarFactory implements ServiceRegistrarFactory {
    private static final Logger log =
        LoggerFactory.getLogger(ConsulServiceRegistrarFactory.class);


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

        return new ConsulServiceRegistrar(consulClient);
    }

    @Override
    public ServiceRegistrar createForDomain(String s) {
        throw new UnsupportedOperationException("Not implemented!");
    }
}
