package com.miftah.core_bank_system.profile;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.miftah.core_bank_system.TestcontainersConfiguration;
import com.miftah.core_bank_system.auth.AuthService;
import com.miftah.core_bank_system.auth.LoginRequest;
import com.miftah.core_bank_system.auth.RegisterRequest;
import com.miftah.core_bank_system.auth.TokenResponse;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.user.UserRepository;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, Locale.getDefault());
    }

    private String token;
    private String adminToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE notifications, audit_logs, transactions, accounts, refresh_tokens, profiles, users RESTART IDENTITY CASCADE");

        // Register a normal user and get token
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser")
                .password("password")
                .build();
        authService.register(registerRequest);

        TokenResponse tokenResponse = authService.login(LoginRequest.builder()
                .username("testuser")
                .password("password")
                .build());
        token = tokenResponse.getToken();

        // Create an admin user manually since registration only creates USER role
        RegisterRequest adminReg = RegisterRequest.builder()
                .username("adminuser")
                .password("password")
                .build();
        authService.register(adminReg);
                User adminUser = userRepository.findByUsername("adminuser").orElseThrow();
        adminUser.setRole(com.miftah.core_bank_system.user.Role.ADMIN);
        userRepository.save(adminUser);

        TokenResponse adminTokenResponse = authService.login(LoginRequest.builder()
                .username("adminuser")
                .password("password")
                .build());
        adminToken = adminTokenResponse.getToken();
    }

    private ProfileRequest createValidProfileRequest() {
        return ProfileRequest.builder()
                .type(ProfileType.KTP)
                .expiryDate(LocalDate.of(2026, 1, 1))
                .identityNumber("1234567890123456")
                .name("John Doe")
                .country("Indonesia")
                .placeOfBirth("Jakarta")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .phone("08123456789")
                .nationality("Indonesia")
                .build();
    }

    private void createProfile(ProfileRequest request) throws Exception {
        mockMvc.perform(post("/api/v1/profiles")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }



    @Test
    void create_Success_ShouldReturnCreated() throws Exception {
        ProfileRequest request = createValidProfileRequest();

        mockMvc.perform(post("/api/v1/profiles")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value(getMessage("success.create")))
                .andExpect(jsonPath("$.data.identityNumber").value(request.getIdentityNumber()));
    }

    @Test
    void create_ValidationFailed_ShouldReturnBadRequest() throws Exception {
        ProfileRequest request = ProfileRequest.builder()
                .name("") // invalid
                .build();

        mockMvc.perform(post("/api/v1/profiles")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(getMessage("error.validation")))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void create_DuplicateFields_ShouldReturnBadRequest() throws Exception {
        ProfileRequest request = createValidProfileRequest();

        // Create first profile
        createProfile(request);

        // Create another user
        RegisterRequest registerRequest2 = RegisterRequest.builder()
                .username("testuser2")
                .password("password")
                .build();
        authService.register(registerRequest2);
        TokenResponse tokenResponse2 = authService.login(LoginRequest.builder()
                .username("testuser2")
                .password("password")
                .build());
        String token2 = tokenResponse2.getToken();

        // Try to create profile with same identity number and phone
        mockMvc.perform(post("/api/v1/profiles")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(getMessage("error.validation")))
                .andExpect(jsonPath("$.errors.identityNumber[0]").value(getMessage("error.profile.identityNumber.duplicate")))
                .andExpect(jsonPath("$.errors.phone[0]").value(getMessage("error.profile.phone.duplicate")));
    }



    @Test
    void get_Success_ShouldReturnProfile() throws Exception {
        ProfileRequest request = createValidProfileRequest();
        createProfile(request);

        mockMvc.perform(get("/api/v1/profiles")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(getMessage("success.get")))
                .andExpect(jsonPath("$.data.identityNumber").value(request.getIdentityNumber()));
    }

    @Test
    void get_NotFound_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/profiles")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }



    @Test
    void update_Success_ShouldReturnUpdatedProfile() throws Exception {
        ProfileRequest request = createValidProfileRequest();
        createProfile(request);

        request.setName("Jane Doe");
        mockMvc.perform(put("/api/v1/profiles")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(getMessage("success.update")))
                .andExpect(jsonPath("$.data.name").value("Jane Doe"));
    }

    @Test
    void update_NotFound_ShouldReturnNotFound() throws Exception {
        ProfileRequest request = createValidProfileRequest();

        mockMvc.perform(put("/api/v1/profiles")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }



    @Test
    void getAll_Success_ShouldReturnOk() throws Exception {
        ProfileRequest request = createValidProfileRequest();
        createProfile(request);

        mockMvc.perform(get("/api/v1/profiles/all")
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(getMessage("success.get")))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)));
    }



    @Test
    void getById_Success_ShouldReturnOk() throws Exception {
        ProfileRequest request = createValidProfileRequest();
        createProfile(request);

        // Find ID using the standard retrieve endpoint
        String responseString = mockMvc.perform(get("/api/v1/profiles")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode rootNode = objectMapper.readTree(responseString);
        String profileId = rootNode.path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/profiles/" + profileId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(getMessage("success.get")))
                .andExpect(jsonPath("$.data.identityNumber").value(request.getIdentityNumber()));
    }

    @Test
    void getById_NotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
