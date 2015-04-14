package se.svt.helios.serviceregistration.consul;

import com.spotify.helios.serviceregistration.ServiceRegistrar;
import com.spotify.helios.serviceregistration.ServiceRegistration;
import com.spotify.helios.serviceregistration.ServiceRegistrationHandle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.svt.helios.serviceregistration.consul.model.Service;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static com.google.common.collect.Lists.newArrayList;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRegistrarTest {
    private final static ServiceRegistration.Endpoint ENDPOINT =
            new ServiceRegistration.Endpoint("redis", "http", 9000, "local", "example.com", null);

    @Mock
    ConsulClient consulClient;

    @Test
    public void testRegister() throws Exception {
        ArgumentCaptor<Service> serviceCaptor = ArgumentCaptor.forClass(Service.class);

        String healthCheckTag = "healthCheckEndpoint::/health";

        ServiceRegistration.Endpoint endpoint =
                new ServiceRegistration.Endpoint("redis", "http", 9000, "local", "example.com", newArrayList(healthCheckTag));

        ConsulServiceRegistrar registrar = new ConsulServiceRegistrar(consulClient);
        registrar.register(new ServiceRegistration(Arrays.asList(endpoint)));
        registrar.close();

        verify(consulClient).register(serviceCaptor.capture());
        verify(consulClient).close();

        Service service = serviceCaptor.getValue();
        assertEquals("redis", service.getName());
        assertEquals(9000, service.getPort().intValue());
        assertTrue(service.getTags().contains("protocol-http"));
        assertEquals("redis-http-9000", service.getCheck().getId());
        assertEquals("HTTP health check for http://example.com:9000/health", service.getCheck().getName());
        assertEquals("HTTP health check requesting http://example.com:9000/health every 10s",
                service.getCheck().getNotes());
        assertEquals("http://example.com:9000/health", service.getCheck().getHttp().toString());
        assertEquals("10s", service.getCheck().getInterval());
    }

    @Test
    public void testSetInterval() throws Exception {
        ArgumentCaptor<Service> serviceCaptor = ArgumentCaptor.forClass(Service.class);

        String healthCheckTag = "healthCheckEndpoint::/health";

        ServiceRegistration.Endpoint endpoint =
                new ServiceRegistration.Endpoint("redis", "http", 9000, "local", "example.com", newArrayList(healthCheckTag));


        System.setProperty(ConsulServiceRegistrar.HEALTH_CHECK_SYSTEM_PROPERTY, "42s");
        ConsulServiceRegistrar registrar = new ConsulServiceRegistrar(consulClient);
        registrar.register(new ServiceRegistration(Arrays.asList(endpoint)));
        registrar.close();

        verify(consulClient).register(serviceCaptor.capture());
        verify(consulClient).close();

        Service service = serviceCaptor.getValue();
        assertEquals("42s", service.getCheck().getInterval());
    }

    @Test
    public void testUnregister() throws Exception {
        ServiceRegistrar registrar = new ConsulServiceRegistrar(consulClient);
        ServiceRegistrationHandle handle = registrar.register(new ServiceRegistration(Arrays.asList(ENDPOINT)));
        registrar.unregister(handle);
        registrar.close();

        verify(consulClient).register((Service) anyObject());
        verify(consulClient).deregister(ENDPOINT.getName());
        verify(consulClient).close();
    }

    @Test
    public void testRegisterWithoutCheck() throws Exception {
        ArgumentCaptor<Service> serviceCaptor = ArgumentCaptor.forClass(Service.class);

        ConsulServiceRegistrar registrar = new ConsulServiceRegistrar(consulClient);
        registrar.register(new ServiceRegistration(Arrays.asList(ENDPOINT)));
        registrar.close();

        verify(consulClient).register(serviceCaptor.capture());
        verify(consulClient).close();

        Service service = serviceCaptor.getValue();
        assertEquals("redis", service.getName());
        assertEquals(9000, service.getPort().intValue());
        assertTrue(service.getTags().contains("protocol-http"));
        assertNull(service.getCheck());
    }

	@Test
	public void testGetServiceName() throws Exception {
		assertEquals("x", ConsulServiceRegistrar.getServiceName("x-v1"));
		assertEquals("some-random-service", ConsulServiceRegistrar.getServiceName("some-random-service-v491"));
		assertEquals("a-service-without-version", ConsulServiceRegistrar.getServiceName("a-service-without-version"));
	}

	@Test
	public void testGetVersionTag() throws Exception {
		assertEquals("v1", ConsulServiceRegistrar.getVersionTag("x-v1"));
		assertEquals("v491", ConsulServiceRegistrar.getVersionTag("some-random-service-v491"));
		assertNull(ConsulServiceRegistrar.getVersionTag("a-service-without-version"));
	}
}
