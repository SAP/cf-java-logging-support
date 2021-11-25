package com.sap.hcp.cf.logging.jersey.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import com.sap.hcp.cf.logging.common.Defaults;
import com.sap.hcp.cf.logging.common.Fields;
import com.sap.hcp.cf.logging.common.LogContext;
import com.sap.hcp.cf.logging.common.request.HttpHeaders;
import com.sap.hcp.cf.logging.common.request.RequestRecord.Direction;

/**
 * Test Class for Jersey Performance Logs
 *
 * @author d048888
 *
 */
public class RequestMetricsClientFilterTest extends AbstractFilterTest {

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        /* -- make sure client side logging is registered -- */
        RequestMetricsFilterRegistry.registerClientFilters(clientConfig);
    }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig();
        config.register(TestResource.class);
        config.register(TestChainedResource.class);
        return config;

    }

    @Test
    public void ChainedResourcePerformanceLogTest() {
        final Response response = ClientRequestUtils.propagate(target("testchainedresource").request(), null).get();
        if (response.hasEntity()) {
            String res = response.readEntity(String.class);
            res.length();
        }
        /*
         * There should be at least two messages, but there might be more if
         * LogContext reports correlation id generation
         * 
         */
        assertThat(getLogSize(), greaterThanOrEqualTo(2));
        assertThat(response.getStatus(), is(200));
        /*
         * -- correlation id should have been propagated
         */
        Set<String> correlationIds = new HashSet<String>();
        for (int i = 0; i < getLogSize(); i++) {
            String id = getField(Fields.CORRELATION_ID, i);
            assertThat(id, not(Defaults.UNKNOWN));
            correlationIds.add(id);
        }
        assertThat(correlationIds.size(), is(1));
		assertThat(getField(Fields.TENANT_ID), is(nullValue()));
    }

    @Test
    public void PerformanceLogTest() {
        @SuppressWarnings("unused")
        final Response response = getClient().target(getBaseUri() + "testresource").request().header(
                                                                                                     HttpHeaders.CORRELATION_ID.getName(),
                                                                                                     "1").get();
		assertThat(getField(Fields.COMPONENT_ID), is(nullValue()));
        assertThat(getField(Fields.RESPONSE_SIZE_B), is("4"));
        assertThat(getField(Fields.RESPONSE_TIME_MS), not(nullValue()));
        assertThat(getField(Fields.RESPONSE_STATUS), is(Integer.toString(TestResource.EXPECTED_STATUS_CODE)));
        assertThat(getField(Fields.RESPONSE_CONTENT_TYPE), is(TestResource.EXPECTED_CONTENT_TYPE));
        assertThat(getField(Fields.DIRECTION), is(Direction.OUT.toString()));
        assertThat(getField(Fields.METHOD), is(TestResource.EXPECTED_REQUEST_METHOD));
        assertThat(getField(Fields.LAYER), is(ClientRequestContextAdapter.LAYER_NAME));
		assertThat(getField(Fields.TENANT_ID), is(nullValue()));
    }

    @Test
    public void ResponseTimeTest() {
        @SuppressWarnings("unused")
        final Response response = target("testresource").request().delete();

        assertThat(new Double(getField(Fields.RESPONSE_TIME_MS)), greaterThan(TestResource.EXPECTED_REQUEST_TIME));
		assertThat(getField(Fields.TENANT_ID), is(nullValue()));

    }
    @Test
    public void TenantPropagationTest() {
        LogContext.add(Fields.TENANT_ID, "tenant1");
        @SuppressWarnings("unused")
        final Response response = ClientRequestUtils.propagate(target("testchainedresource").request(), null).get();
        assertThat(getField(Fields.TENANT_ID), is("tenant1"));
    }
}
