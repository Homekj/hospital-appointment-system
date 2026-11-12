package com.aaa.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cors_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class CorsPolicyIntegrationTest {
    private static final String APPROVED_ORIGIN = "http://192.168.0.48:5173";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void configuredFrontendOriginCanPassCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/departments")
                        .header("Origin", APPROVED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", APPROVED_ORIGIN));
    }

    @Test
    void unlistedOriginIsRejectedByCorsPolicy() throws Exception {
        mockMvc.perform(options("/api/departments")
                        .header("Origin", "https://unlisted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void legacyAdminRouteIsProtectedByTheRegisteredInterceptor() throws Exception {
        mockMvc.perform(get("/admin/doctor/page"))
                .andExpect(status().isUnauthorized());
    }
}
