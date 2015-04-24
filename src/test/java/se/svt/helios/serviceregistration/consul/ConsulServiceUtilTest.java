package se.svt.helios.serviceregistration.consul;

import org.junit.Test;
import se.svt.helios.serviceregistration.consul.model.ServiceCheck;

import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static se.svt.helios.serviceregistration.consul.Utils.newEndpointBuilder;

public class ConsulServiceUtilTest {

    private static ConsulServiceUtil serviceUtil =
            new ConsulServiceUtil(15, "helios-deployed");

    @Test
    public void testServiceCheckFromTag() throws Exception {
        ServiceCheck check = serviceUtil.serviceCheck(
                newEndpointBuilder()
                        .name("redis")
                        .host("localhost")
                        .port(9000)
                        .protocol("http")
                        .tag("healthCheckEndpoint::/health")
                        .build());

        assertEquals("http://localhost:9000/health", check.getHttp());
        assertEquals("redis-http-9000", check.getId());
        assertEquals("15s", check.getInterval());
    }

    @Test
    public void testServiceCheckFromHelios() throws Exception {
        ServiceCheck check = serviceUtil.serviceCheck(
                newEndpointBuilder()
                        .name("redis")
                        .host("localhost")
                        .port(9000)
                        .protocol("http")
                        .httpHealthCheck("/status")
                        .build());

        assertEquals("http://localhost:9000/status", check.getHttp());
        assertEquals("redis-http-9000", check.getId());
        assertEquals("15s", check.getInterval());
    }

    @Test
    public void testServiceCheckPrecedence() throws Exception {
        ServiceCheck check = serviceUtil.serviceCheck(
                newEndpointBuilder()
                        .name("redis")
                        .host("localhost")
                        .port(9000)
                        .protocol("http")
                        .httpHealthCheck("/helios")
                        .tag("healthCheckEndpoint::/tag")
                        .build());

        assertEquals("http://localhost:9000/tag", check.getHttp());
    }

    @Test
    public void testNoServiceCheck() throws Exception {
        ServiceCheck check = serviceUtil.serviceCheck(
                newEndpointBuilder()
                        .name("redis")
                        .host("localhost")
                        .port(9000)
                        .protocol("http")
                        .build());

        assertNull(check);
    }

    @Test
    public void testTags() throws Exception {
        List<String> tags = Arrays.asList("tag1", "key::value", "tag2");

        List<String> expected = Arrays.asList("tag1", "tag2", "helios-deployed", "protocol-http");
        List<String> actual = serviceUtil.tags(newEndpointBuilder().tags(tags).protocol("http").build());

        Collections.sort(expected);
        Collections.sort(actual);

        assertThat(actual, is(expected));
    }

    @Test
    public void testKvTags() throws Exception {
        List<String> tags = Arrays.asList("tag1", "key::value", "tag2", "key2::value2::2");

        Map<String, String> expected = new HashMap<>();
        expected.put("key", "value");
        expected.put("key2", "value2::2");
        Map<String, String> actual = serviceUtil.kvTags(newEndpointBuilder().tags(tags).protocol("http").build());

        assertThat(actual, is(expected));
    }

    @Test
    public void testVersionTag() throws Exception {
        assertEquals("v1", serviceUtil.versionTag(newEndpointBuilder().name("x-v1").build()));
        assertEquals("v491", serviceUtil.versionTag(newEndpointBuilder().name("some-random-service-v491").build()));
        assertNull(serviceUtil.versionTag(newEndpointBuilder().name("a-service-without-version").build()));
    }

    @Test
    public void testServiceName() throws Exception {
        assertEquals("x", serviceUtil.serviceName(newEndpointBuilder().name("x-v1").build()));
        assertEquals("some-random-service", serviceUtil.serviceName(newEndpointBuilder().name("some-random-service-v491").build()));
        assertEquals("a-service-without-version", serviceUtil.serviceName(newEndpointBuilder().name("a-service-without-version").build()));
    }
}