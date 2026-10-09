package com.cafeflow.common.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.cafeflow.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Verifies the deny-by-default baseline without starting Spring Boot or any container:
 * only the real SecurityConfig, its handlers and the GlobalExceptionHandler are loaded.
 */
@SpringJUnitWebConfig(
        classes = {
            SecurityConfig.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class,
            GlobalExceptionHandler.class,
            SecurityBaselineTest.WebConfig.class
        })
class SecurityBaselineTest {

    @Configuration
    @EnableWebMvc
    static class WebConfig {}

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void anonymousRequestToApiIsRejectedWith401AndStructuredBody() throws Exception {
        mvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void anonymousWriteIsRejectedWith401NotBlockedByCsrf() throws Exception {
        mvc.perform(post("/api/v1/tables").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void authenticatedUserIsStillDeniedOnUnopenedEndpoints() throws Exception {
        // Deny-by-default: even a logged-in OWNER cannot reach an endpoint nobody has opened.
        mvc.perform(get("/api/v1/orders").with(user("owner").roles("OWNER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(
                        jsonPath("$.message").value("You do not have permission to perform this action."));
    }

    @Test
    void unknownActuatorEndpointsAreDenied() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/heapdump")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthProbesArePublic() throws Exception {
        // No actuator is wired in this slice, so a permitted request falls through to a 404 -
        // which proves the security chain let it pass (a denied request would be 401).
        mvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isNotFound());
    }
}
