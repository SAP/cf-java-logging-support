package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import org.junit.Test;

import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertTrue;

public class CloudLoggingConfigurationCustomizerProviderTest {

    @Test
    public void canLoadViaSPI() {
        ServiceLoader<AutoConfigurationCustomizerProvider> loader = ServiceLoader.load(AutoConfigurationCustomizerProvider.class);
        Stream<AutoConfigurationCustomizerProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertTrue(CloudFoundryResourceProvider.class.getName() + " not loaded via SPI.",
                providers.anyMatch(p -> p instanceof CloudLoggingConfigurationCustomizerProvider));
    }

}