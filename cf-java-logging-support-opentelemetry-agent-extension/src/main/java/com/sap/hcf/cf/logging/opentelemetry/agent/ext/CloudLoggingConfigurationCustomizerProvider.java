package com.sap.hcf.cf.logging.opentelemetry.agent.ext;

import com.sap.hcf.cf.logging.opentelemetry.agent.ext.binding.CloudLoggingBindingPropertiesSupplier;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.pivotal.cfenv.core.CfEnv;

public class CloudLoggingConfigurationCustomizerProvider implements AutoConfigurationCustomizerProvider {

    private static final CfEnv cfEnv = new CfEnv();

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        autoConfiguration
                .addPropertiesSupplier(new CloudLoggingBindingPropertiesSupplier());

        // ConfigurableLogRecordExporterProvider
    }

}
