package se.svt.helios.serviceregistration.consul;

import org.junit.Test;
import se.svt.helios.serviceregistration.consul.model.RegistrarConfig;

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

    @Test
    public void testCreateConfig() throws Exception {
        try {
            System.setProperty(ConsulServiceRegistrarFactory.PROP_DEPLOY_TAG, "tag");
            System.setProperty(ConsulServiceRegistrarFactory.PROP_SYNC_INTERVAL, "123");
            System.setProperty(ConsulServiceRegistrarFactory.PROP_HEALTH_CHECK_INTERVAL, "456");

            RegistrarConfig config = ConsulServiceRegistrarFactory.createConfig();
            assertEquals("tag", config.getDeployTag());
            assertEquals(123, config.getSyncInterval());
            assertEquals(456, config.getHealthCheckInterval());
        } finally {
            System.clearProperty(ConsulServiceRegistrarFactory.PROP_DEPLOY_TAG);
            System.clearProperty(ConsulServiceRegistrarFactory.PROP_SYNC_INTERVAL);
            System.clearProperty(ConsulServiceRegistrarFactory.PROP_HEALTH_CHECK_INTERVAL);
        }
    }
}