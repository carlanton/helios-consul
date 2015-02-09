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

package se.svt.helios.serviceregistration.consul.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.List;

// Response from http://localhost:8500/v1/agent/services
public class AgentService {
    private final String id;
    private final String service;
    private final Integer port;
    private final List<String> tags;

    public AgentService(@JsonProperty("ID") final String id,
                        @JsonProperty("Service") final String service,
                        @JsonProperty("Tags") final List<String> tags,
                        @JsonProperty("Port") final Integer port) {
        this.id = id;
        this.service = service;
        this.tags = tags;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public String getService() {
        return service;
    }

    public List<String> getTags() {
        return tags;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AgentService that = (AgentService) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (port != null ? !port.equals(that.port) : that.port != null) {
            return false;
        }
        if (service != null ? !service.equals(that.service) : that.service != null) {
            return false;
        }
        if (tags != null ? !tags.equals(that.tags) : that.tags != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (service != null ? service.hashCode() : 0);
        result = 31 * result + (port != null ? port.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("service", service)
                .add("port", port)
                .add("tags", tags)
                .toString();
    }
}
