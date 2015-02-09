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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.Objects;

@JsonPropertyOrder({"Script", "Interval"})
public class ServiceCheck {
    private String script;
    private String interval;

    public ServiceCheck(@JsonProperty("Script") String script,
                        @JsonProperty("Interval") String interval) {
        this.script = script;
        this.interval = interval;
    }

    public String getScript() {
        return script;
    }

    public String getInterval() {
        return interval;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServiceCheck that = (ServiceCheck) o;

        if (interval != null ? !interval.equals(that.interval) : that.interval != null) {
            return false;
        }
        if (script != null ? !script.equals(that.script) : that.script != null) {
            return false;  
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = script != null ? script.hashCode() : 0;
        result = 31 * result + (interval != null ? interval.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("script", script)
                .add("interval", interval)
                .toString();
    }
}
