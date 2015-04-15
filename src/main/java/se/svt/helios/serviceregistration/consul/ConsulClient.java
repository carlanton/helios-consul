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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.svt.helios.serviceregistration.consul.model.AgentService;
import se.svt.helios.serviceregistration.consul.model.Service;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Future;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;

public class ConsulClient implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ConsulClient.class);
    private static final int CONNECT_TIMEOUT = 5000; // ms
    private static final int CONNECTION_REQUEST_TIMEOUT = 5000; // ms
    private static final int SOCKET_TIMEOUT = 5000; // ms

    private static final String AGENT_SERVICES_ENDPOINT = "/v1/agent/services";
    private static final String REGISTER_ENDPOINT = "/v1/agent/service/register";
    private static final String DEREGISTER_ENDPOINT = "/v1/agent/service/deregister/%s";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String baseUri;
    private final CloseableHttpAsyncClient httpClient;

    public ConsulClient(String baseUri, CloseableHttpAsyncClient httpClient) {
        this.httpClient = httpClient;
        this.httpClient.start();
        this.baseUri = baseUri;
    }

    public ConsulClient(String baseUri) {
        this(baseUri,
             HttpAsyncClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT).build()).build());
    }

    public Future<HttpResponse> register(final Service record) throws JsonProcessingException {
        final String value = OBJECT_MAPPER.writeValueAsString(record);

        log.info("Registering consul service Json: {}", value);

        final URI uri = URI.create(baseUri + REGISTER_ENDPOINT);
        final HttpPut request = new HttpPut(uri);
        request.setEntity(new StringEntity(value, "UTF-8"));

        return httpClient.execute(request,
                new RequestCallback("register service " + record.getId()));
    }

    public Future<HttpResponse> deregister(final String serviceId) {
        final URI uri = URI.create(baseUri + String.format(DEREGISTER_ENDPOINT, serviceId));
        return httpClient.execute(new HttpGet(uri),
                new RequestCallback("deregister service " + serviceId));
    }

    public Future<HttpResponse> agentServices() {
        final URI uri = URI.create(baseUri + AGENT_SERVICES_ENDPOINT);
        return httpClient.execute(new HttpGet(uri),
                new RequestCallback(AGENT_SERVICES_ENDPOINT));
    }

    @Override
    public void close() throws Exception {
        httpClient.close();
    }

    public Map<String, AgentService> getAgentServicesWithTag(final String tag) throws Exception {
        HttpResponse response = agentServices().get();
        Map<String, AgentService> myServices = OBJECT_MAPPER.readValue(
                response.getEntity().getContent(),
                new TypeReference<Map<String, AgentService>>() {
                });

        return ImmutableMap.copyOf(Maps.filterValues(
                myServices, new Predicate<AgentService>() {
                    @Override
                    public boolean apply(AgentService service) {
                        return service.getTags() != null && service.getTags().contains(tag);
                    }
                }));
    }

    private static class RequestCallback implements FutureCallback<HttpResponse> {
        private final String description;

        private RequestCallback(String endpoint) {
            this.description = endpoint;
        }

        @Override
        public void completed(HttpResponse result) {
            int statusCode = result.getStatusLine().getStatusCode();
            log.debug("Request '{}' completed. (status code: {})",
                    description, statusCode);
        }

        @Override
        public void failed(Exception ex) {
            log.warn("Request '{}' failed.", description, ex);
        }

        @Override
        public void cancelled() {
            log.warn("Request '{}' cancelled.", description);
        }
    }

    String getBaseUri() {
        return baseUri;
    }
}
