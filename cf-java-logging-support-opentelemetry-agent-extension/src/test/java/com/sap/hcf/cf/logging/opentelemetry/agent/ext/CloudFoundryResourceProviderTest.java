package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import org.junit.Test;

import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertTrue;

public class CloudFoundryResourceProviderTest {

    @Test
    public void canLoadViaSPI() {
        ServiceLoader<ResourceProvider> loader = ServiceLoader.load(ResourceProvider.class);
        Stream<ResourceProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertTrue(CloudFoundryResourceProvider.class.getName() + " not loaded via SPI",
                providers.anyMatch(p -> p instanceof CloudFoundryResourceProvider));
    }

}