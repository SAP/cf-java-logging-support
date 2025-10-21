package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.sap.hcf.cf.logging.opentelemetry.agent.ext.testing.ResourceAssertions.assertStringAttribute;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemStubsExtension.class)
public class CloudFoundryResourceProviderTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @Test
    void canLoadViaSPI() {
        ServiceLoader<ResourceProvider> loader = ServiceLoader.load(ResourceProvider.class);
        Stream<ResourceProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertThat(providers).describedAs(CloudFoundryResourceProvider.class.getName() + " not loaded via SPI")
                             .anySatisfy(p -> assertThat(p).isInstanceOf(CloudFoundryResourceProvider.class));
    }

    @Test
    void generatesResource() throws Exception {
        String vcapApplication =
                new String(Files.readAllBytes(Paths.get(getClass().getResource("vcap_application.json").toURI())));
        environmentVariables.set("VCAP_APPLICATION", vcapApplication);

        CloudFoundryResourceProvider provider = new CloudFoundryResourceProvider();
        Resource resource = provider.createResource(DefaultConfigProperties.createFromMap(Collections.emptyMap()));

        assertStringAttribute(resource, "sap.cf.app_id").isEqualTo("test-app-id");
        assertStringAttribute(resource, "sap.cf.app_name").isEqualTo("test-application");
        assertStringAttribute(resource, "sap.cf.instance_id").isEqualTo("42");
        assertStringAttribute(resource, "sap.cf.org_id").isEqualTo("test-org-id");
        assertStringAttribute(resource, "sap.cf.org_name").isEqualTo("test-org");
        assertStringAttribute(resource, "sap.cf.process.id").isEqualTo("test-process-id");
        assertStringAttribute(resource, "sap.cf.process.type").isEqualTo("test-process-type");
        assertStringAttribute(resource, "sap.cf.source_id").isEqualTo("test-app-id");
        assertStringAttribute(resource, "sap.cf.space_id").isEqualTo("test-space-id");
        assertStringAttribute(resource, "sap.cf.space_name").isEqualTo("test-space");
        assertStringAttribute(resource, "service.name").isEqualTo("test-application");
    }

}
