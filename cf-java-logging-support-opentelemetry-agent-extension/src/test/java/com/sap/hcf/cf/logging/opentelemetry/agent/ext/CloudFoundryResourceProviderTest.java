package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CloudFoundryResourceProviderTest {

    @Rule
    public EnvironmentVariablesRule environmentVariablesRule = new EnvironmentVariablesRule();

    @Test
    public void canLoadViaSPI() {
        ServiceLoader<ResourceProvider> loader = ServiceLoader.load(ResourceProvider.class);
        Stream<ResourceProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertTrue(CloudFoundryResourceProvider.class.getName() + " not loaded via SPI",
                   providers.anyMatch(p -> p instanceof CloudFoundryResourceProvider));
    }

    @Test
    public void generatesResource() throws Exception {
        String vcapApplication =
                new String(Files.readAllBytes(Paths.get(getClass().getResource("vcap_application.json").toURI())));
        environmentVariablesRule.set("VCAP_APPLICATION", vcapApplication);

        CloudFoundryResourceProvider provider = new CloudFoundryResourceProvider();
        Resource resource = provider.createResource(DefaultConfigProperties.createFromMap(Collections.emptyMap()));

        assertEquals("test-app-id", resource.getAttribute(AttributeKey.stringKey("sap.cf.app_id")));
        assertEquals("test-application", resource.getAttribute(AttributeKey.stringKey("sap.cf.app_name")));
        assertEquals("42", resource.getAttribute(AttributeKey.stringKey("sap.cf.instance_id")));
        assertEquals("test-org-id", resource.getAttribute(AttributeKey.stringKey("sap.cf.org_id")));
        assertEquals("test-org", resource.getAttribute(AttributeKey.stringKey("sap.cf.org_name")));
        assertEquals("test-process-id", resource.getAttribute(AttributeKey.stringKey("sap.cf.process.id")));
        assertEquals("test-process-type", resource.getAttribute(AttributeKey.stringKey("sap.cf.process.type")));
        assertEquals("test-app-id", resource.getAttribute(AttributeKey.stringKey("sap.cf.source_id")));
        assertEquals("test-space-id", resource.getAttribute(AttributeKey.stringKey("sap.cf.space_id")));
        assertEquals("test-space", resource.getAttribute(AttributeKey.stringKey("sap.cf.space_name")));
        assertEquals("test-application", resource.getAttribute(AttributeKey.stringKey("service.name")));
    }
}
