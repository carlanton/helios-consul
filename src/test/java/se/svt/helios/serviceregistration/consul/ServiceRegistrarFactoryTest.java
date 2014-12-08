package se.svt.helios.serviceregistration.consul;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class ServiceRegistrarFactoryTest {
    @Test
    public void testCreate() throws Exception {
        String consulAddr = "localhost:8500";
        String checkScript = "/usr/bin/service-check";
        String checkInterval = "7s";
        String connectString = String.format("%s,%s,%s", consulAddr, checkScript, checkInterval);

        ConsulServiceRegistrarFactory factory = new ConsulServiceRegistrarFactory();
        ConsulServiceRegistrar registrar = (ConsulServiceRegistrar) factory.create(connectString);

        assertEquals(checkScript, registrar.getServiceCheckScript());
        assertEquals(checkInterval, registrar.getServiceCheckInterval());
        assertEquals(consulAddr, registrar.getConsulClient().getBaseUri());
    }

    @Test
    public void testCreateNoCheck() throws Exception {
        String connectString = "localhost:8500";

        ConsulServiceRegistrarFactory factory = new ConsulServiceRegistrarFactory();
        ConsulServiceRegistrar registrar = (ConsulServiceRegistrar) factory.create(connectString);

        assertNull(registrar.getServiceCheckScript());
        assertNull(registrar.getServiceCheckInterval());
        assertEquals(connectString, registrar.getConsulClient().getBaseUri());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateEmptyConnectString() throws Exception {
        ConsulServiceRegistrarFactory factory = new ConsulServiceRegistrarFactory();
        factory.create("");
    }
}