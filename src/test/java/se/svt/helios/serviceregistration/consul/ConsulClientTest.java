package se.svt.helios.serviceregistration.consul;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.svt.helios.serviceregistration.consul.model.Service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ConsulClientTest {

    @Mock
    CloseableHttpAsyncClient httpClient;

    String baseUri = "localhost:8500";

    @Test
    public void testRegister() throws Exception {
        ConsulClient client = new ConsulClient(baseUri, httpClient);
        client.register(Service.builder().setName("redis").build());

        verify(httpClient).execute(any(HttpPut.class), Matchers.<FutureCallback<HttpResponse>>any());
    }

    @Test
    public void testDeregister() throws Exception {
        ConsulClient client = new ConsulClient(baseUri, httpClient);
        client.deregister("redis");

        verify(httpClient).execute(any(HttpGet.class), Matchers.<FutureCallback<HttpResponse>>any());
    }

    @Test
    public void testClose() throws Exception {
        ConsulClient client = new ConsulClient(baseUri, httpClient);
        client.close();
        verify(httpClient).close();
    }
}
