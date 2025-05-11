package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudLoggingBindingPropertiesSupplier;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

import java.util.logging.Logger;

public class CloudLoggingConfigurationCustomizerProvider implements AutoConfigurationCustomizerProvider {

    private static final Logger LOG = Logger.getLogger(CloudLoggingConfigurationCustomizerProvider.class.getName());
    private static final String VERSION = "3.8.4";

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        LOG.info("Initializing SAP BTP Observability extension " + VERSION);
        autoConfiguration.addPropertiesSupplier(new CloudLoggingBindingPropertiesSupplier());

        // ConfigurableLogRecordExporterProvider
    }

}
