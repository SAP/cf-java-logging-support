package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudFoundryResourceProviderTest {

    @Test
    public void canLoadViaSPI() {
        ServiceLoader<ResourceProvider> loader = ServiceLoader.load(ResourceProvider.class);
        Stream<ResourceProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertThat(providers).describedAs(CloudFoundryResourceProvider.class.getName() + " not loaded via SPI")
                             .anySatisfy(p -> assertThat(p).isInstanceOf(CloudFoundryResourceProvider.class));
    }

}
