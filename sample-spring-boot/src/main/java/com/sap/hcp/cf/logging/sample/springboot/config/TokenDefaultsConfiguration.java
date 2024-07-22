package com.sap.hcp.cf.logging.sample.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "defaults.token")
public class TokenDefaultsConfiguration {

    private Duration expiration;
    private String issuer;

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = Duration.parse(expiration);
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

}
