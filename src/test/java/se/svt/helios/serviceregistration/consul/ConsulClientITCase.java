package se.svt.helios.serviceregistration.consul;

import com.spotify.helios.serviceregistration.ServiceRegistration;
import com.spotify.helios.serviceregistration.ServiceRegistrationHandle;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ConsulClientITCase {
	private static final String TAG = ConsulServiceRegistrar.HELIOS_DEPLOYED_TAG;

	private ConsulServiceRegistrar newRegistrar() {
		ConsulServiceRegistrarFactory serviceRegistrarFactory = new ConsulServiceRegistrarFactory();
		return (ConsulServiceRegistrar) serviceRegistrarFactory.create("http://127.0.0.1:8500,/bin/echo,10s");
	}

	private ServiceRegistration newSimpleServiceRegistration(String serviceId) {
		return new ServiceRegistration(Arrays.asList(new ServiceRegistration.Endpoint(serviceId, "tcp", 8080, "", "localhost")));
	}

	@Test
	public void test() throws Exception {
		final String testServiceId = "test-service-v39";

		ConsulServiceRegistrar registrar = newRegistrar();
		registrar.register(newSimpleServiceRegistration(testServiceId));

		Thread.sleep(2000);
		List<String> services = registrar.getConsulClient().getAgentServicesWithTag(TAG);
		assertEquals(1, services.size());
		assertEquals(testServiceId, services.get(0));
		registrar.close();

		Thread.sleep(1000);
		registrar = newRegistrar();
		services = registrar.getConsulClient().getAgentServicesWithTag(TAG);
		assertEquals(1, services.size());
		assertEquals(testServiceId, services.get(0));


		registrar.syncState();
		services = registrar.getConsulClient().getAgentServicesWithTag(TAG);
		assertEquals(0, services.size());
	}

	@Test
	public void test2() throws Exception {
		final String testServiceId = "test-2-service-v12";

		ConsulServiceRegistrar registrar = newRegistrar();
		ServiceRegistrationHandle handle = registrar.register(newSimpleServiceRegistration(testServiceId));

		Thread.sleep(2000);
		List<String> services = registrar.getConsulClient().getAgentServicesWithTag(TAG);
		assertEquals(1, services.size());
		assertEquals(testServiceId, services.get(0));

		// Consul "forgets" the service
		registrar.getConsulClient().deregister(testServiceId);
		Thread.sleep(1000);
		assertEquals(0, registrar.getConsulClient().getAgentServicesWithTag(TAG).size());

		registrar.syncState();
		Thread.sleep(2000);
		List<String> services2 = registrar.getConsulClient().getAgentServicesWithTag(TAG);
		assertEquals(1, services2.size());
		assertEquals(testServiceId, services2.get(0));

		// Cleanup
		registrar.unregister(handle);
		Thread.sleep(1000);
		assertEquals(0, registrar.getConsulClient().getAgentServicesWithTag(TAG).size());
	}
}