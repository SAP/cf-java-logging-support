package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudLoggingConfigurationCustomizerProviderTest {

    @Test
    public void canLoadViaSPI() {
        ServiceLoader<AutoConfigurationCustomizerProvider> loader =
                ServiceLoader.load(AutoConfigurationCustomizerProvider.class);
        Stream<AutoConfigurationCustomizerProvider> providers = StreamSupport.stream(loader.spliterator(), false);
        assertThat(providers).describedAs(AutoConfigurationCustomizerProvider.class.getName() + " not loaded via SPI")
                             .anySatisfy(p -> assertThat(p).isInstanceOf(AutoConfigurationCustomizerProvider.class));
    }

}
