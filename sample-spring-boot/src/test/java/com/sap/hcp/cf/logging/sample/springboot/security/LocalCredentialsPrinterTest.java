package com.sap.hcp.cf.logging.sample.springboot.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class LocalCredentialsPrinterTest {

    @Test
    void shouldPrintGeneratedLocalCredentials(CapturedOutput output) {
        SecurityProperties securityProperties = new SecurityProperties();

        new LocalCredentialsPrinter(securityProperties).run(null);

        assertThat(output).contains("Local development credentials")
                          .contains("Username: user")
                          .contains("Password: " + securityProperties.getUser().getPassword());
    }

    @Test
    void shouldNotPrintConfiguredCredentials(CapturedOutput output) {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.getUser().setPassword("configured-secret");

        new LocalCredentialsPrinter(securityProperties).run(null);

        assertThat(output).doesNotContain("Local development credentials")
                          .doesNotContain("configured-secret");
    }

    @Test
    void shouldNotRegisterPrinterWithCloudProfile() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("cloud");
            context.register(LocalCredentialsPrinter.class);
            context.refresh();

            assertThat(context.getBeansOfType(LocalCredentialsPrinter.class)).isEmpty();
        }
    }

}
