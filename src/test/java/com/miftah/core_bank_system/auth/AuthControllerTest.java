package com.miftah.core_bank_system.auth;

import com.miftah.core_bank_system.TestcontainersConfiguration;
import com.miftah.core_bank_system.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private AuthService authService;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private MessageSource messageSource;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private String getMessage(String code) {
                return messageSource.getMessage(code, null, Locale.getDefault());
        }

        @BeforeEach
        void setUp() {
                jdbcTemplate.execute("TRUNCATE TABLE notifications, audit_logs, transactions, accounts, refresh_tokens, profiles, users RESTART IDENTITY CASCADE");
        }

        private void registerUser(String username, String password) {
                authService.register(RegisterRequest.builder()
                                .username(username)
                                .password(password)
                                .build());
        }

        private String loginAndGetToken(String username, String password) throws Exception {
                LoginRequest loginRequest = LoginRequest.builder()
                                .username(username)
                                .password(password)
                                .build();

                String jsonResponse = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                        .andReturn().getResponse().getContentAsString();

                JsonNode root = objectMapper.readTree(jsonResponse);
                return root.get("data").get("token").asText();
        }

        private String loginAndGetRefreshToken(String username, String password) throws Exception {
                LoginRequest loginRequest = LoginRequest.builder()
                                .username(username)
                                .password(password)
                                .build();

                String jsonResponse = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                        .andReturn().getResponse().getContentAsString();

                JsonNode root = objectMapper.readTree(jsonResponse);
                return root.get("data").get("refreshToken").asText();
        }

        // ==========================================
        // POST /api/v1/auth/register
        // ==========================================

        @Test
        void register_Success_ShouldReturnCreated() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .username("testuser")
                                .password("password123")
                                .build();

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.code").value(201))
                        .andExpect(jsonPath("$.message").value(getMessage("success.register")))
                        .andExpect(jsonPath("$.data.username").value("testuser"))
                        .andExpect(jsonPath("$.data.id").exists())
                        .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        void register_ValidationFailed_BlankFields_ShouldReturnBadRequest() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .username("")
                                .password("pwd")
                                .build();

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(400))
                        .andExpect(jsonPath("$.message").value(getMessage("error.validation")))
                        .andExpect(jsonPath("$.errors").exists());
        }

        @Test
        void register_DuplicateUsername_ShouldReturnBadRequest() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .username("existinguser")
                                .password("password123")
                                .build();

                // Register first
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated());

                // Try to register again
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(400))
                        .andExpect(jsonPath("$.message").value(getMessage("error.validation")))
                        .andExpect(jsonPath("$.errors.username[0]").value(getMessage("error.username.duplicate")));
        }

        // ==========================================
        // POST /api/v1/auth/login
        // ==========================================

        @Test
        void login_Success_ShouldReturnTokens() throws Exception {
                registerUser("testlogin", "password123");

                LoginRequest loginRequest = LoginRequest.builder()
                                .username("testlogin")
                                .password("password123")
                                .build();

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.message").value(getMessage("success.login")))
                        .andExpect(jsonPath("$.data.token").exists())
                        .andExpect(jsonPath("$.data.refreshToken").exists());
        }

        @Test
        void login_BadCredentials_ShouldReturnUnauthorized() throws Exception {
                registerUser("testbadcreds", "password123");

                LoginRequest loginRequest = LoginRequest.builder()
                                .username("testbadcreds")
                                .password("wrongpassword")
                                .build();

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value(401))
                        .andExpect(jsonPath("$.message").value(getMessage("error.bad-credentials")));
        }

        @Test
        void login_UserNotFound_ShouldReturnNotFound() throws Exception {
                LoginRequest loginRequest = LoginRequest.builder()
                                .username("nonexistent")
                                .password("password123")
                                .build();

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                        .andExpect(status().isNotFound());
        }

        @Test
        void login_ValidationFailed_ShouldReturnBadRequest() throws Exception {
                LoginRequest request = LoginRequest.builder()
                                .username("")
                                .password("")
                                .build();

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(400))
                        .andExpect(jsonPath("$.message").value(getMessage("error.validation")))
                        .andExpect(jsonPath("$.errors").exists());
        }

        // ==========================================
        // GET /api/v1/auth/me
        // ==========================================

        @Test
        void me_Success_ShouldReturnUserDetails() throws Exception {
                registerUser("testme", "password123");
                String token = loginAndGetToken("testme", "password123");

                mockMvc.perform(get("/api/v1/auth/me")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.message").value(getMessage("success.me")))
                        .andExpect(jsonPath("$.data.username").value("testme"))
                        .andExpect(jsonPath("$.data.id").exists())
                        .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        void me_NoToken_ShouldReturnForbidden() throws Exception {
                mockMvc.perform(get("/api/v1/auth/me"))
                        .andExpect(status().isForbidden());
        }

        @Test
        void me_InvalidToken_ShouldReturnForbidden() throws Exception {
                mockMvc.perform(get("/api/v1/auth/me")
                                .header("Authorization", "Bearer invalid-jwt-token"))
                        .andExpect(status().isForbidden());
        }

        // ==========================================
        // POST /api/v1/auth/refresh
        // ==========================================

        @Test
        void refreshToken_Success_ShouldReturnNewTokens() throws Exception {
                registerUser("testrefresh", "password123");
                String refreshToken = loginAndGetRefreshToken("testrefresh", "password123");

                TokenRefreshRequest refreshRequest = TokenRefreshRequest.builder()
                                .refreshToken(refreshToken)
                                .build();

                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(refreshRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                        .andExpect(jsonPath("$.data.token").exists())
                        .andExpect(jsonPath("$.data.refreshToken").exists())
                        .andExpect(jsonPath("$.data.refreshToken").value(not(refreshToken))); // rotated
        }

        @Test
        void refreshToken_InvalidToken_ShouldReturnForbidden() throws Exception {
                TokenRefreshRequest refreshRequest = TokenRefreshRequest.builder()
                                .refreshToken("invalid-token")
                                .build();

                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(refreshRequest)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(403))
                        .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void refreshToken_UsedToken_ShouldReturnForbidden() throws Exception {
                registerUser("testusedtoken", "password123");
                String refreshToken = loginAndGetRefreshToken("testusedtoken", "password123");

                TokenRefreshRequest refreshRequest = TokenRefreshRequest.builder()
                                .refreshToken(refreshToken)
                                .build();

                // First use should succeed
                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(refreshRequest)))
                        .andExpect(status().isOk());

                // Second use with the same old token should fail (it was rotated/deleted)
                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(refreshRequest)))
                        .andExpect(status().isForbidden());
        }

        // ==========================================
        // POST /api/v1/auth/logout
        // ==========================================

        @Test
        void logout_Success_ShouldReturnOk() throws Exception {
                registerUser("testlogout", "password123");
                String refreshToken = loginAndGetRefreshToken("testlogout", "password123");

                TokenRefreshRequest logoutRequest = TokenRefreshRequest.builder()
                                .refreshToken(refreshToken)
                                .build();

                mockMvc.perform(post("/api/v1/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.message").value("Logged out successfully"));

                // Verify refresh token is invalidated
                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutRequest)))
                        .andExpect(status().isForbidden());
        }

        @Test
        void logout_InvalidToken_ShouldStillReturnOk() throws Exception {
                TokenRefreshRequest logoutRequest = TokenRefreshRequest.builder()
                                .refreshToken("nonexistent-token")
                                .build();

                mockMvc.perform(post("/api/v1/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(logoutRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200));
        }
}