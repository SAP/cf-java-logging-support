package com.sap.hcp.cf.logging.sample.springboot;

import com.sap.hcp.cf.logging.sample.springboot.keystore.KeyStoreDynLogConfiguration;
import com.sap.hcp.cf.logging.servlet.dynlog.DynamicLogLevelConfiguration;
import com.sap.hcp.cf.logging.servlet.filter.*;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Clock;

@SpringBootApplication
@EnableWebMvc
public class SampleAppSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleAppSpringBootApplication.class, args);
    }

    /**
     * Registers a customized {@link RequestLoggingFilter} with the servlet. We inject our own dynamic logging
     * configuration, that contains the public RSA key from our keystore.
     *
     * @param dynLogConfig
     *         autowired with {@link KeyStoreDynLogConfiguration}
     * @return a registration of the {@link RequestLoggingFilter}
     */
    @Bean
    public FilterRegistrationBean<MyLoggingFilter> loggingFilter(@Autowired DynamicLogLevelConfiguration dynLogConfig) {
        FilterRegistrationBean<MyLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new MyLoggingFilter(dynLogConfig));
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

    /**
     * Provides a {@link BCryptPasswordEncoder} for Basic-Auth.
     *
     * @return the encoder
     */
    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    private class MyLoggingFilter extends CompositeFilter {

        private MyLoggingFilter(DynamicLogLevelConfiguration dynLogConfig) {
            super(new AddVcapEnvironmentToLogContextFilter(), new AddHttpHeadersToLogContextFilter(),
                  new CorrelationIdFilter(), new DynamicLogLevelFilter(() -> dynLogConfig),
                  new GenerateRequestLogFilter());
        }
    }
}
