package com.miftah.core_bank_system.auth;

import com.miftah.core_bank_system.TestcontainersConfiguration;
import com.miftah.core_bank_system.account.AccountRepository;
import com.miftah.core_bank_system.profile.ProfileRepository;
import com.miftah.core_bank_system.user.UserRepository;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import java.util.Locale;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
        private AccountRepository accountRepository;

        @Autowired
        private ProfileRepository profileRepository;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private MessageSource messageSource;

        private String getMessage(String code) {
                return messageSource.getMessage(code, null, Locale.getDefault());
        }

        @BeforeEach
        void setUp() {
                accountRepository.deleteAll();
                profileRepository.deleteAll();
                userRepository.deleteAll();
        }

        @Test
        void register_Success_ShouldReturnCreated() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .username("testuser")
                                .password("password123")
                                .build();

                String message = getMessage("success.register");

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.code").value(201))
                                .andExpect(jsonPath("$.message").value(message))
                                .andExpect(jsonPath("$.data.username").value("testuser"))
                                .andExpect(jsonPath("$.data.id").exists());
        }

        @Test
        void register_ValidationFailed_ShouldReturnBadRequest() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .username("") // Invalid
                                .password("pwd") // Invalid
                                .build();
                
                String message = getMessage("error.validation");

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400))
                                .andExpect(jsonPath("$.message").value(message))
                                .andExpect(jsonPath("$.errors").exists());
        }

        @Test
        void register_DuplicateUsername_ShouldReturnBadRequest() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .username("existinguser")
                                .password("password123")
                                .build();

                // Create user first
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());
                
                String message = getMessage("error.validation");
                String expectedDuplicateError = "username: " + getMessage("error.username.duplicate");

                // Try to register again
                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400))
                                .andExpect(jsonPath("$.message").value(message))
                                .andExpect(jsonPath("$.errors")
                                                .value(containsString(expectedDuplicateError)));
        }

        @Test
        void login_Success_ShouldReturnToken() throws Exception {
                String password = "password123";
                RegisterRequest registerRequest = RegisterRequest.builder()
                                .username("testlogin")
                                .password(password)
                                .build();
                authServiceRegister(registerRequest);

                LoginRequest loginRequest = LoginRequest.builder()
                                .username("testlogin")
                                .password(password)
                                .build();

                String message = getMessage("success.login");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value(message))
                                .andExpect(jsonPath("$.data.token").exists());
        }

        @Test
        void login_BadCredentials_ShouldReturnUnauthorized() throws Exception {
                RegisterRequest registerRequest = RegisterRequest.builder()
                                .username("testbadcreds")
                                .password("password123")
                                .build();
                authServiceRegister(registerRequest);

                LoginRequest loginRequest = LoginRequest.builder()
                                .username("testbadcreds")
                                .password("wrongpassword")
                                .build();
                
                String message = getMessage("error.bad-credentials");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value(401))
                                .andExpect(jsonPath("$.message").value(message));
        }

        @Test
        void login_ValidationFailed_ShouldReturnBadRequest() throws Exception {
                LoginRequest request = LoginRequest.builder()
                                .username("")
                                .password("")
                                .build();

                String message = getMessage("error.validation");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400))
                                .andExpect(jsonPath("$.message").value(message))
                                .andExpect(jsonPath("$.errors").exists());
        }

        @Autowired
        private AuthService authService;

        private void authServiceRegister(RegisterRequest request) {
                authService.register(request);
        }

        @Test
        void me_Success_ShouldReturnUserDetails() throws Exception {
                String password = "password123";
                RegisterRequest registerRequest = RegisterRequest.builder()
                                .username("testme")
                                .password(password)
                                .build();
                authServiceRegister(registerRequest);

                LoginRequest loginRequest = LoginRequest.builder()
                                .username("testme")
                                .password(password)
                                .build();

                String token = authService.login(loginRequest).getToken();
                String message = getMessage("success.me");

                mockMvc.perform(get("/api/auth/me")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value(message))
                                .andExpect(jsonPath("$.data.username").value("testme"))
                                .andExpect(jsonPath("$.data.id").exists())
                                .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        void me_Unauthorized_ShouldReturnForbidden() throws Exception {
                mockMvc.perform(get("/api/auth/me")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }
}