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
import se.svt.helios.serviceregistration.consul.model.AgentService;
import se.svt.helios.serviceregistration.consul.model.RegistrarConfig;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConsulServiceRegistrar implements ServiceRegistrar {
    private static final Logger log = LoggerFactory.getLogger(ConsulServiceRegistrar.class);

    private final Map<ServiceRegistrationHandle, ServiceRegistration> handles;
    private final Set<String> endpoints;

    private final ScheduledExecutorService executor;
    private final ConsulClient consulClient;
    private final ConsulServiceUtil serviceUtil;
    private final RegistrarConfig config;

    public ConsulServiceRegistrar(final ConsulClient consulClient, final RegistrarConfig config) {
        this.consulClient = consulClient;
        this.config = config;
        this.handles = Maps.newConcurrentMap();
        this.endpoints = Sets.newConcurrentHashSet();

        this.serviceUtil = new ConsulServiceUtil(config.getHealthCheckInterval(),
                config.getDeployTag());

        this.executor = MoreExecutors.getExitingScheduledExecutorService(
                          (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1,
                          new ThreadFactoryBuilder().setNameFormat("consul-registrar-%d").build()),
                          0, TimeUnit.SECONDS);

        // If the Consul agent is restarted, all services will be forgotten. Therefore we sync the
        // state between services known by this plugin and services registered in Consul.
        Runnable registrationRunnable = new Runnable() {
            @Override
            public void run() {
                syncState();
            }
        };
        this.executor.scheduleAtFixedRate(registrationRunnable,
                config.getSyncInterval(), config.getSyncInterval(), TimeUnit.SECONDS);
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
            consulClient.register(serviceUtil.createService(endpoint));
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

    void syncState() {
        // 1. List all my services with tag HELIOS_DEPLOYED_TAG
        Map<String, AgentService> registeredServices = null;
        try {
            registeredServices = consulClient.getAgentServicesWithTag(config.getDeployTag());
        } catch (Exception e) {
            log.warn("Failure during lookup of Consul services", e);
        }

        if (registeredServices == null) {
            return;
        }

        // 2. De-register all services not known by Helios
        for (String service : registeredServices.keySet()) {
            if (!endpoints.contains(service)) {
                log.info("Service '{}' not known by Helios. Sending deregistration.", service);
                consulClient.deregister(service);
            }
        }

        // 3. Register all services not known by Consul
        for (ServiceRegistration handle : handles.values()) {
            for (ServiceRegistration.Endpoint endpoint : handle.getEndpoints()) {
                if (!registeredServices.containsKey(endpoint.getName())) {
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

    ConsulClient getConsulClient() {
        return consulClient;
    }
}
