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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.Objects;

import java.net.URL;

@JsonPropertyOrder({"http", "interval"})
public class ServiceCheck {
    private final URL http;
    private final String interval;
    private final String name;
    private final String notes;


    private final String id;

    public ServiceCheck(@JsonProperty("id") String id,
                        @JsonProperty("name") String name,
                        @JsonProperty("http") URL http,
                        @JsonProperty("interval") String interval,
                        @JsonProperty("notes") String notes) {
        this.http = http;
        this.interval = interval;
        this.name = name;
        this.notes = notes;
        this.id = id;
    }

    public URL getHttp() {
        return http;
    }

    public String getInterval() {
        return interval;
    }

    public String getName() {
        return name;
    }

    public String getNotes() {
        return notes;
    }

    public String getId() {
        return id;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id = null;
        private String name = null;
        private URL http = null;
        private String interval = null;
        private String notes = null;

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

        public Builder setHttp(URL http) {
            this.http = http;
            return this;
        }

        public Builder setInterval(String interval) {
            this.interval = interval;
            return this;
        }

        public Builder setNotes(String notes) {
            this.notes = notes;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Builder builder = (Builder) o;

            if (http != null ? !http.equals(builder.http) : builder.http != null) {
                return false;
            }
            if (id != null ? !id.equals(builder.id) : builder.id != null) {
                return false;
            }
            if (interval != null ? !interval.equals(builder.interval) : builder.interval != null) {
                return false;
            }
            if (name != null ? !name.equals(builder.name) : builder.name != null) {
                return false;
            }
            if (notes != null ? !notes.equals(builder.notes) : builder.notes != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (http != null ? http.hashCode() : 0);
            result = 31 * result + (interval != null ? interval.hashCode() : 0);
            result = 31 * result + (notes != null ? notes.hashCode() : 0);
            return result;
        }

        public ServiceCheck build() {
            return new ServiceCheck(id, name, http, interval, notes);
        }
    }


    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("http", http)
                .add("interval", interval)
                .add("notes", notes)
                .toString();
    }
}
