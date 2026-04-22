package com.miftah.core_bank_system.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource; 
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;
import com.miftah.core_bank_system.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "application.security.rate-limit.register=3"
})
class RateLimitingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_RateLimiting_ShouldReturn429After3Requests() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("rateuser")
                .password("Password123!")
                .build();

        String content = objectMapper.writeValueAsString(request);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(content))
                .andExpect(status().is(org.hamcrest.Matchers.oneOf(201, 400))); 
        }

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").value("Too Many Requests"))
            .andExpect(jsonPath("$.message").value("Rate limit exceeded. Please try again later."));
    }
}
