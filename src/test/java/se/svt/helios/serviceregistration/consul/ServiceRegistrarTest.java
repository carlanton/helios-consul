package se.svt.helios.serviceregistration.consul;

import com.spotify.helios.serviceregistration.ServiceRegistrar;
import com.spotify.helios.serviceregistration.ServiceRegistration;
import com.spotify.helios.serviceregistration.ServiceRegistrationHandle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.svt.helios.serviceregistration.consul.model.RegistrarConfig;
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
    private final static ServiceRegistration.Endpoint ENDPOINT_WITHOUT_TAGS =
            new ServiceRegistration.Endpoint("redis", "http", 9000, "local", "example.com", null, null);

    private final static ServiceRegistration.Endpoint ENDPOINT_CHECK_FROM_TAG =
            new ServiceRegistration.Endpoint("redis", "http", 9000, "local", "example.com",
                    newArrayList("healthCheckEndpoint::/health"), null);

    @Mock
    ConsulClient consulClient;

    @Test
    public void testRegister() throws Exception {
        ArgumentCaptor<Service> serviceCaptor = ArgumentCaptor.forClass(Service.class);

        RegistrarConfig config = ConsulServiceRegistrarFactory.createConfig();
        ConsulServiceRegistrar registrar = new ConsulServiceRegistrar(consulClient, config);
        registrar.register(new ServiceRegistration(Arrays.asList(ENDPOINT_CHECK_FROM_TAG)));
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
        assertEquals("http://example.com:9000/health", service.getCheck().getHttp());
        assertEquals("10s", service.getCheck().getInterval());
    }

    @Test
    public void testSetInterval() throws Exception {
        ArgumentCaptor<Service> serviceCaptor = ArgumentCaptor.forClass(Service.class);

        RegistrarConfig config = new RegistrarConfig(10, 42, "helios-deploy");
        ConsulServiceRegistrar registrar = new ConsulServiceRegistrar(consulClient, config);
        registrar.register(new ServiceRegistration(Arrays.asList(ENDPOINT_CHECK_FROM_TAG)));
        registrar.close();

        verify(consulClient).register(serviceCaptor.capture());
        verify(consulClient).close();

        Service service = serviceCaptor.getValue();
        assertEquals("42s", service.getCheck().getInterval());
    }

    @Test
    public void testUnregister() throws Exception {
        RegistrarConfig config = ConsulServiceRegistrarFactory.createConfig();
        ServiceRegistrar registrar = new ConsulServiceRegistrar(consulClient, config);
        ServiceRegistrationHandle handle = registrar.register(new ServiceRegistration(Arrays.asList(ENDPOINT_WITHOUT_TAGS)));
        registrar.unregister(handle);
        registrar.close();

        verify(consulClient).register((Service) anyObject());
        verify(consulClient).deregister(ENDPOINT_WITHOUT_TAGS.getName());
        verify(consulClient).close();
    }

    @Test
    public void testRegisterWithoutCheck() throws Exception {
        ArgumentCaptor<Service> serviceCaptor = ArgumentCaptor.forClass(Service.class);

        RegistrarConfig config = ConsulServiceRegistrarFactory.createConfig();
        ConsulServiceRegistrar registrar = new ConsulServiceRegistrar(consulClient, config);
        registrar.register(new ServiceRegistration(Arrays.asList(ENDPOINT_WITHOUT_TAGS)));
        registrar.close();

        verify(consulClient).register(serviceCaptor.capture());
        verify(consulClient).close();

        Service service = serviceCaptor.getValue();
        assertEquals("redis", service.getName());
        assertEquals(9000, service.getPort().intValue());
        assertTrue(service.getTags().contains("protocol-http"));
        assertNull(service.getCheck());
    }
}
