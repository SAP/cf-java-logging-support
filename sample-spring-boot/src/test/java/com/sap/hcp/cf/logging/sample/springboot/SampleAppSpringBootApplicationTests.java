package com.sap.hcp.cf.logging.sample.springboot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
class SampleAppSpringBootApplicationTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(@Autowired WebApplicationContext context) {
        mockMvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void shouldNotAuthenticateWithFormerDefaultCredentials() throws Exception {
        mockMvc.perform(get("/publickey").with(httpBasic("user", "secret")))
               .andExpect(status().isUnauthorized());
    }

}
