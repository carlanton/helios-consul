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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.spotify.helios.serviceregistration.ServiceRegistrar;
import com.spotify.helios.serviceregistration.ServiceRegistration;
import com.spotify.helios.serviceregistration.ServiceRegistrationHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.svt.helios.serviceregistration.consul.model.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsulServiceRegistrar implements ServiceRegistrar {
    private static final Logger log = LoggerFactory.getLogger(ConsulServiceRegistrar.class);
    public static final String HELIOS_DEPLOYED_TAG = "helios-deployed";
    private static final Pattern ENDPOINT_NAME_PATTERN =
        Pattern.compile("(?<name>.+)-(?<tag>v\\d+)$");
    private static final int CONSUL_UPDATE_INTERVAL = 30; // seconds

    private final Map<ServiceRegistrationHandle, ServiceRegistration> handles;
    private final Set<String> endpoints;

    private final ScheduledExecutorService executor;
    private final ConsulClient consulClient;
    private final String serviceCheckScript;
    private final String serviceCheckInterval;

    public ConsulServiceRegistrar(final ConsulClient consulClient, final String serviceCheckScript,
                                  final String serviceCheckInterval) {
        this.consulClient = consulClient;
        this.serviceCheckScript = serviceCheckScript;
        this.serviceCheckInterval = serviceCheckInterval;

        this.handles = Maps.newConcurrentMap();
        this.endpoints = Sets.newConcurrentHashSet();

        this.executor = MoreExecutors.getExitingScheduledExecutorService(
                          (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1,
                          new ThreadFactoryBuilder().setNameFormat("consul-registrar-%d").build()),
                          0, TimeUnit.SECONDS);

        // If the Consul agent is restarted, all services will forgotten. Therefore we sync the
        // state between services known by this plugin and services registered in Consul.
        Runnable registrationRunnable = new Runnable() {
            @Override
            public void run() {
                syncState();
            }
        };
        this.executor.scheduleAtFixedRate(registrationRunnable,
                CONSUL_UPDATE_INTERVAL, CONSUL_UPDATE_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public ServiceRegistrationHandle register(final ServiceRegistration registration) {
        final ServiceRegistrationHandle newHandle = new ServiceRegistrationHandle() {};
        handles.put(newHandle, registration);

        for (final ServiceRegistration.Endpoint endpoint : registration.getEndpoints()) {
            if (!endpoints.add(endpoint.getName())) {
                log.error("Endpoint names must be unique since they map to a Consul Service ID. " +
                          "'{}' already present.", endpoint.getName());
            }
        }

        try {
            sendRegistration(registration);
        } catch (Exception e) {
            log.warn("Error performing registration", e);
        }
        return newHandle;
    }

    @Override
    public void unregister(ServiceRegistrationHandle handle) {
        if (!handles.containsKey(handle)) {
            return;
        }
        try {
            sendDeRegistration(handle);
        } catch (Exception e) {
            log.warn("Error removing registration handle {}", handle, e);
        }
        handles.remove(handle);
    }

    @Override
    public void close() {
        try {
            executor.shutdownNow();
        } catch (Exception e) {
            log.error("Error shutting down executor service", e);
        }
        try {
            consulClient.close();
        } catch (Exception e) {
            log.error("Error shutting down http client to Consul", e);
        }
    }

    private void sendRegistration(ServiceRegistration registration)
            throws JsonProcessingException {

        for (final ServiceRegistration.Endpoint endpoint : registration.getEndpoints()) {
            List<String> tags = new ArrayList<>();

            // Add version as a tag
            String versionTag = getVersionTag(endpoint.getName());
            if (versionTag != null) {
                tags.add(versionTag);
            }

            // Indicate that this service is deployed by Helios
            tags.add(HELIOS_DEPLOYED_TAG);

            // Add the protocol as a tag
            tags.add(String.format("protocol-%s", endpoint.getProtocol()));

            String name = getServiceName(endpoint.getName());

            Service.Builder recordBuilder = Service.builder()
                    .setId(endpoint.getName())
                    .setName(name)
                    .setPort(endpoint.getPort())
                    .setTags(tags);

            if (serviceCheckScript != null) {
                String script = String.format("%s %s %s %s", serviceCheckScript,
                        endpoint.getHost(), endpoint.getPort(), endpoint.getProtocol());

                recordBuilder.setCheck(script, serviceCheckInterval);
            }
            consulClient.register(recordBuilder.build());
        }
    }

    private void sendDeRegistration(ServiceRegistrationHandle handle) {
        final ServiceRegistration registration = handles.get(handle);
        if (registration == null) {
            return;
        }
        for (ServiceRegistration.Endpoint endpoint : registration.getEndpoints()) {
            consulClient.deregister(endpoint.getName());
            endpoints.remove(endpoint.getName());
        }
    }

    static String getVersionTag(String endpointName) {
        Matcher matcher = ENDPOINT_NAME_PATTERN.matcher(endpointName);
        if (matcher.matches()) {
            return matcher.group("tag");
        } else {
            return null;
        }
    }

    static String getServiceName(String endpointName) {
        Matcher matcher = ENDPOINT_NAME_PATTERN.matcher(endpointName);
        if (matcher.matches()) {
            return matcher.group("name");
        } else {
            return endpointName;
        }
    }

    void syncState() {
        // 1. List all my services with tag HELIOS_DEPLOYED_TAG
        List<String> registeredServices = null;
        try {
            registeredServices = consulClient.getAgentServicesWithTag(HELIOS_DEPLOYED_TAG);
        } catch (Exception e) {
            log.warn("Failure during lookup of Consul services", e);
        }

        if (registeredServices == null) {
            return;
        }

        // 2. De-register all services not known by Helios
        for (String service : registeredServices) {
            if (!endpoints.contains(service)) {
                log.info("Service '{}' not known by Helios. Sending deregistration.", service);
                consulClient.deregister(service);
            }
        }

        // 3. Register all services not known by Consul
        for (ServiceRegistration handle : handles.values()) {
            for (ServiceRegistration.Endpoint endpoint : handle.getEndpoints()) {
                if (!registeredServices.contains(endpoint.getName())) {
                    log.info("Service '{}' not known by Consul. Re-registering Helios job.",
                             endpoint.getName());
                    try {
                        sendRegistration(handle);
                    } catch (Exception e) {
                        log.warn("Error performing registration", e);
                    }
                    break;
                }
            }
        }
    }

    String getServiceCheckInterval() {
        return serviceCheckInterval;
    }

    String getServiceCheckScript() {
        return serviceCheckScript;
    }

    ConsulClient getConsulClient() {
        return consulClient;
    }
}
