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

package se.svt.helios.serviceregistration.consul.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import java.util.List;

/*
    {
        "ID": "redis1",
        "Name": "redis",
        "Tags": [
            "master",
            "v1"
        ],
        "Port": 8000,
        "Check": {
            "Script": "/usr/local/bin/check_redis.py",
            "Interval": "10s"
        }
    }
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"ID", "Name", "Tags", "Port", "Check"})
public class Service {
    private final String id;
    private final String name;
    private final List<String> tags;
    private final Integer port;
    private final ServiceCheck check;

    public Service(@JsonProperty("ID") String id,
                    @JsonProperty("Name") String name,
                    @JsonProperty("Tags") List<String> tags,
                    @JsonProperty("Port") Integer port,
                    @JsonProperty("Check") ServiceCheck check) {
        this.id = id;
        this.name = Preconditions.checkNotNull(name);
        this.tags = tags;
        this.port = port;
        this.check = check;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getTags() {
        return tags;
    }

    public Integer getPort() {
        return port;
    }

    public ServiceCheck getCheck() {
        return check;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id = null;
        private String name = null;
        private List<String> tags = null;
        private Integer port = null;
        private ServiceCheck check = null;

        public Builder() {
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setTags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder setCheck(String script, String interval) {
            this.check = new ServiceCheck(script, interval);
            return this;
        }

        public Service build() {
            return new Service(id, name, tags, port, check);
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("tags", tags)
                .add("port", port)
                .add("check", check)
                .toString();
    }
}
