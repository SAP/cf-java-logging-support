package com.sap.hcp.cf.logging.sample.springboot;

import com.sap.hcp.cf.logging.servlet.filter.RequestLoggingFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import java.time.Clock;

@SpringBootApplication
public class SampleAppSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleAppSpringBootApplication.class, args);
    }

    /**
     * Registers a customized {@link RequestLoggingFilter} with the servlet. We inject our own dynamic logging
     * configuration, that contains the public RSA key from our keystore.
     *
     * @return a registration of the {@link RequestLoggingFilter}
     */
    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> loggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestLoggingFilter());
        registrationBean.setName("request-logging");
        registrationBean.addUrlPatterns("/*");
        registrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    /**
     * Provides a global {@link Clock} instance. Useful for testing.
     *
     * @return the global clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}
