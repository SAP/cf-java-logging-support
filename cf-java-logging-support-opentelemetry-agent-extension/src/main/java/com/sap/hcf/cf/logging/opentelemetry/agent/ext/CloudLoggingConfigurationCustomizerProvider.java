package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CaasBindingPropertiesSupplier;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudLoggingBindingPropertiesSupplier;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.DefaultOtelBackendPropertiesSupplier;
import com.sap.hcf.cf.logging.opentelemetry.agent.ext.exporter.SanitizeSpanExporterCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

import java.util.logging.Logger;

import static com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.DefaultOtelBackendPropertiesSupplier.builder;

public class CloudLoggingConfigurationCustomizerProvider implements AutoConfigurationCustomizerProvider {

    private static final Logger LOG = Logger.getLogger(CloudLoggingConfigurationCustomizerProvider.class.getName());
    private static final String VERSION = "4.1.0";

    private static DefaultOtelBackendPropertiesSupplier getDefaultOtelBackendPropertiesSupplier() {
        return builder() //
                         .add(new CaasBindingPropertiesSupplier()) // this has priority
                         .add(new CloudLoggingBindingPropertiesSupplier()) // look for Cloud Logging as fallback and backward compatibility
                         .build();
    }

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        LOG.info("Initializing SAP BTP Observability extension " + VERSION);
        autoConfiguration.addPropertiesSupplier(getDefaultOtelBackendPropertiesSupplier());
        autoConfiguration.addSpanExporterCustomizer(new SanitizeSpanExporterCustomizer());
    }

}
