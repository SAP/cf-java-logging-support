package com.sap.hcp.cf.logging.sample.springboot.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!cloud")
public class LocalCredentialsPrinter implements ApplicationRunner {

    private final SecurityProperties.User user;

    public LocalCredentialsPrinter(SecurityProperties securityProperties) {
        this.user = securityProperties.getUser();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (user.isPasswordGenerated()) {
            System.err.printf("""

                    ============================================================
                    Local development credentials
                      Username: %s
                      Password: %s
                    ============================================================

                    """, user.getName(), user.getPassword());
        }
    }
}
