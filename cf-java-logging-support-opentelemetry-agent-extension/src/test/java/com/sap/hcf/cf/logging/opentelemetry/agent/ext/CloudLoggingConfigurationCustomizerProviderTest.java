package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CloudLoggingConfigurationCustomizerProviderTest {

    @Test
    public void canLoadViaSPI() {
        ServiceLoader<AutoConfigurationCustomizerProvider> loader = ServiceLoader.load(AutoConfigurationCustomizerProvider.class);
        Stream<AutoConfigurationCustomizerProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertTrue(providers.anyMatch(p -> p instanceof CloudLoggingConfigurationCustomizerProvider),
                CloudFoundryResourceProvider.class.getName() + " not loaded via SPI.");
    }

}