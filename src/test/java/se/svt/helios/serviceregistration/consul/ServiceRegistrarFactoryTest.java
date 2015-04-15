package se.svt.helios.serviceregistration.consul;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class ServiceRegistrarFactoryTest {
    @Test
    public void testCreate() throws Exception {
        String connectString = "localhost:8500";

        ConsulServiceRegistrarFactory factory = new ConsulServiceRegistrarFactory();
        ConsulServiceRegistrar registrar = (ConsulServiceRegistrar) factory.create(connectString);

        assertEquals(connectString, registrar.getConsulClient().getBaseUri());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateEmptyConnectString() throws Exception {
        ConsulServiceRegistrarFactory factory = new ConsulServiceRegistrarFactory();
        factory.create("");
    }
}