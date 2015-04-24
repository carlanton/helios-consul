package se.svt.helios.serviceregistration.consul;

import com.google.common.collect.ImmutableSet;
import com.spotify.helios.serviceregistration.ServiceRegistration;
import com.spotify.helios.serviceregistration.ServiceRegistrationHandle;
import org.junit.Test;
import se.svt.helios.serviceregistration.consul.model.AgentService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConsulClientITCase {
	private static final String TAG = ConsulServiceRegistrarFactory.createConfig().getDeployTag();

	private ConsulServiceRegistrar newRegistrar() {
		ConsulServiceRegistrarFactory serviceRegistrarFactory = new ConsulServiceRegistrarFactory();
		return (ConsulServiceRegistrar) serviceRegistrarFactory.create("http://127.0.0.1:8500");
	}

	private ServiceRegistration newService(String serviceId) {
		return newService(serviceId, null);
	}

	private ServiceRegistration newService(String serviceId, List<String> tags) {
		return ServiceRegistration.newBuilder()
			.endpoint(serviceId, "tcp", 8080, "", "localhost", tags)
			.build();
	}

	@Test
	public void testRegistrarCleanup() throws Exception {
		final String serviceId = "test-service-v39";

		// Register a service
		final ConsulServiceRegistrar registrarA = newRegistrar();
		registrarA.register(newService(serviceId));

		Thread.sleep(2000);

		// Check that the service actually is registered
		assertTrue(registrarA.getConsulClient().getAgentServicesWithTag(TAG).containsKey(serviceId));

		registrarA.close();
		Thread.sleep(1000);

		// A new service registrar appears with no knowledge about the service
		final ConsulServiceRegistrar registrarB = newRegistrar();

		// The cleanup job have not been executed yet, so we should still find the service in Consul
		assertTrue(registrarB.getConsulClient().getAgentServicesWithTag(TAG).containsKey(serviceId));

		// Force the cleanup job to run
		registrarB.syncState();

		// And the registration is gone!
		assertTrue(registrarB.getConsulClient().getAgentServicesWithTag(TAG).isEmpty());
	}

	@Test
	public void testReRegister() throws Exception {
		final String serviceId = "test-2-service-v12";

		// Register a service
		final ConsulServiceRegistrar registrar = newRegistrar();
		final ServiceRegistrationHandle handle = registrar.register(newService(serviceId));
		final ConsulClient client = registrar.getConsulClient();

		Thread.sleep(1000);

		// Check that the service actually is registered
		assertTrue(client.getAgentServicesWithTag(TAG).containsKey(serviceId));

		// Consul "forgets" about the service (i.e. manually de-register it)
		client.deregister(serviceId).get();
		assertTrue(client.getAgentServicesWithTag(TAG).isEmpty());

		// Force re-registration
		registrar.syncState();

		// Check that the service is re-registered
		assertTrue(client.getAgentServicesWithTag(TAG).containsKey(serviceId));

		// Cleanup
		registrar.unregister(handle);
		Thread.sleep(1000);
		assertTrue(registrar.getConsulClient().getAgentServicesWithTag(TAG).isEmpty());
	}

	@Test
	public void testEndpointTags() throws Exception {
		final String serviceId = "test-service-with-tags-v2";

		// Register a service
		final ConsulServiceRegistrar registrar = newRegistrar();
		final ServiceRegistrationHandle handle = registrar.register(
			newService(serviceId, Arrays.asList("tag-1", "tag-2")));
		final ConsulClient client = registrar.getConsulClient();

		Thread.sleep(1000);

		AgentService service = client.getAgentServicesWithTag(TAG).get(serviceId);

		Set<String> expectedTags = ImmutableSet.of(TAG, "protocol-tcp", "v2", "tag-1", "tag-2");
		Set<String> actualTags = ImmutableSet.copyOf(service.getTags());

		assertEquals(expectedTags, actualTags);

		// Cleanup
		registrar.unregister(handle);
		Thread.sleep(1000);
		assertTrue(registrar.getConsulClient().getAgentServicesWithTag(TAG).isEmpty());
	}
}