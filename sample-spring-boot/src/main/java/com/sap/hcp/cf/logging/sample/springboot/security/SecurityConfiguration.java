package com.sap.hcp.cf.logging.sample.springboot.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()) // This is just a demo app. Authentication is only to prevent log spam.
                   .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                   .httpBasic(Customizer.withDefaults()).build();
    }
}
