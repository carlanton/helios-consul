package se.svt.helios.serviceregistration.consul;

import com.spotify.helios.serviceregistration.ServiceRegistration.Endpoint;
import com.spotify.helios.serviceregistration.ServiceRegistration.EndpointHealthCheck;

import java.util.Arrays;
import java.util.List;

public class Utils {
    public static EndpointBuilder newEndpointBuilder() {
        return new EndpointBuilder();
    }

    public static class EndpointBuilder {
        private String name = "service";
        private String protocol = "http";
        private int port = 8080;
        private String domain = "example.com";
        private String host = "localhost";
        private List<String> tags = null;
        private EndpointHealthCheck healthCheck = null;

        public EndpointBuilder name(String name) {
            this.name = name;
            return this;
        }

        public EndpointBuilder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public EndpointBuilder port(int port) {
            this.port = port;
            return this;
        }

        public EndpointBuilder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public EndpointBuilder host(String host) {
            this.host = host;
            return this;
        }

        public EndpointBuilder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public EndpointBuilder tag(String tag) {
            this.tags = Arrays.asList(tag);
            return this;
        }

        public EndpointBuilder healthCheck(EndpointHealthCheck healthCheck) {
            this.healthCheck = healthCheck;
            return this;
        }

        public EndpointBuilder httpHealthCheck(String path) {
            return healthCheck(new EndpointHealthCheck(EndpointHealthCheck.HTTP, path));
        }

        public Endpoint build() {
            return new Endpoint(name, protocol, port, domain, host, tags, healthCheck);
        }
    }
}
