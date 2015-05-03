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

import com.spotify.helios.serviceregistration.ServiceRegistration.Endpoint;
import com.spotify.helios.serviceregistration.ServiceRegistration.EndpointHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.svt.helios.serviceregistration.consul.model.Service;
import se.svt.helios.serviceregistration.consul.model.ServiceCheck;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsulServiceUtil {
    private static final Logger log = LoggerFactory.getLogger(ConsulServiceUtil.class);

    private static final String HEALTH_CHECK_ENDPOINT_TAGKEY = "healthCheckEndpoint";
    private static final String KVTAG_SEPARATOR = "::";
    private static final Pattern ENDPOINT_NAME_PATTERN =
            Pattern.compile("(?<name>.+)-(?<tag>v\\d+)$");

    private final int healthCheckInterval;
    private final String deployTag;

    public ConsulServiceUtil(int healthCheckInterval, String deployTag) {
        this.healthCheckInterval = healthCheckInterval;
        this.deployTag = deployTag;
    }

    public Service createService(final Endpoint endpoint) {
        final String id = endpoint.getName();
        final int port = endpoint.getPort();
        final String name = serviceName(endpoint);
        final List<String> tags = tags(endpoint);
        final ServiceCheck check = serviceCheck(endpoint);

        return new Service(id, name, tags, port, check);
    }

    public ServiceCheck serviceCheck(final Endpoint endpoint) {
        Map<String, String> kvTags = kvTags(endpoint);

        String path = kvTags.get(HEALTH_CHECK_ENDPOINT_TAGKEY);
        if (path == null && endpoint.getHealthCheck() != null &&
                EndpointHealthCheck.HTTP.equals(endpoint.getHealthCheck().getType())) {
            path = endpoint.getHealthCheck().getPath();
        }

        if (path == null) {
            return null;
        }

        URL url;
        try {
            url = new URL(endpoint.getProtocol(), endpoint.getHost(), endpoint.getPort(), path);
        } catch (MalformedURLException e) {
            log.warn("Could not create health check for endpoint {}", endpoint.toString(), e);
            return null;
        }

        String id = String.format("%s-%s-%s",
                endpoint.getName(), endpoint.getProtocol(), endpoint.getPort());

        String name = String.format("HTTP health check for %s",
                url.toString());

        String interval = String.format("%ds", healthCheckInterval);
        String notes = String.format("HTTP health check requesting %s every %s",
                url.toString(), interval);

        return new ServiceCheck(id, name, url.toString(), interval, notes);
    }

    public List<String> tags(final Endpoint endpoint) {
        List<String> tags = new ArrayList<>();

        // Indicate that this service is deployed by Helios
        tags.add(deployTag);

        // Add the protocol as a tag
        tags.add(String.format("protocol-%s", endpoint.getProtocol()));

        // Add version as a tag
        String versionTag = versionTag(endpoint);
        if (versionTag != null) {
            tags.add(versionTag);
        }

        // Filter out KV-tags
        if (endpoint.getTags() != null) {
            for (String tag : endpoint.getTags()) {
                if (!tag.contains(KVTAG_SEPARATOR)) {
                    tags.add(tag);
                }
            }
        }

        return tags;
    }

    public Map<String, String> kvTags(final Endpoint endpoint) {
        Map<String, String> kvTags = new HashMap<>();

        if (endpoint.getTags() != null) {
            for (String tag : endpoint.getTags()) {
                if (tag.contains(KVTAG_SEPARATOR)) {
                    String[] kv = tag.split(KVTAG_SEPARATOR, 2);
                    kvTags.put(kv[0], kv[1]);
                }
            }
        }

        return kvTags;
    }

    public String versionTag(final Endpoint endpoint) {
        Matcher matcher = ENDPOINT_NAME_PATTERN.matcher(endpoint.getName());
        if (matcher.matches()) {
            return matcher.group("tag");
        } else {
            return null;
        }
    }

    public String serviceName(final Endpoint endpoint) {
        Matcher matcher = ENDPOINT_NAME_PATTERN.matcher(endpoint.getName());
        if (matcher.matches()) {
            return matcher.group("name");
        } else {
            return endpoint.getName();
        }
    }
}
