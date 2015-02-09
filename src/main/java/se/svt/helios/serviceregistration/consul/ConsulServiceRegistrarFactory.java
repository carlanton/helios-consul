/*
 * Copyright (c) 2015 SVT AB.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsulServiceRegistrarFactory implements ServiceRegistrarFactory {
    private static final Logger log =
        LoggerFactory.getLogger(ConsulServiceRegistrarFactory.class);

    private static final Pattern CONNECT_STRING_PATTERN = Pattern.compile("^(.+),(.+),(.+)$");

    @Override
    public ServiceRegistrar create(final String connectString) {

        String baseUri;
        String serviceCheckScript = null;
        String serviceCheckInterval = null;

        if (connectString == null || connectString.isEmpty()) {
            throw new RuntimeException("Empty connect string!");
        }

        Matcher matcher = CONNECT_STRING_PATTERN.matcher(connectString);

        if (matcher.matches()) {
            baseUri = matcher.group(1);
            serviceCheckScript = matcher.group(2);
            serviceCheckInterval = matcher.group(3);
        } else {
            baseUri = connectString;
        }

        final URI uri;

        try {
            uri = new URI(baseUri);
        } catch (URISyntaxException e) {
            throw new RuntimeException("baseUri is not a proper url", e);
        }

        log.info("Creating new ConsulServiceRegistrar: " +
                 "baseUri={}, checkScript={}, checkInterval={}",
                 uri.toString(), serviceCheckScript, serviceCheckInterval);

        final ConsulClient consulClient = new ConsulClient(uri.toString());

        return new ConsulServiceRegistrar(consulClient, serviceCheckScript,
                                          serviceCheckInterval);
    }

    @Override
    public ServiceRegistrar createForDomain(String s) {
        throw new UnsupportedOperationException("Not implemented!");
    }
}
