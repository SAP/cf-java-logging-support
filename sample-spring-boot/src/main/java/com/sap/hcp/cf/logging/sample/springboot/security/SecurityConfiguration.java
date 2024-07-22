package com.sap.hcp.cf.logging.sample.springboot.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public UserDetailsService userDetails(@Autowired PasswordEncoder encoder,
                                          @Value("${auth.basic.username:user}") String username,
                                          @Value("${auth.basic.password:secret}") String password) {
        UserDetails user = User.withUsername(username).password(encoder.encode(password)).roles(Roles.USER).build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        return http.csrf(csrf -> csrf.disable()) // This is just a demo app. Authentication is only to prevent log spam.
                   .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                   .httpBasic(Customizer.withDefaults()).build();
    }
}
