package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CloudFoundryResourceProviderTest {

    @Test
    public void canLoadViaSPI() {
        ServiceLoader<ResourceProvider> loader = ServiceLoader.load(ResourceProvider.class);
        Stream<ResourceProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertTrue(providers.anyMatch(p -> p instanceof CloudFoundryResourceProvider),
                CloudFoundryResourceProvider.class.getName() + " not loaded via SPI");
    }

}