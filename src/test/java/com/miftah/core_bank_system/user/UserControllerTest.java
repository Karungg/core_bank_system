package com.miftah.core_bank_system.user;

import java.time.LocalDate;
import java.util.Locale;

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

import tools.jackson.databind.ObjectMapper;

import com.miftah.core_bank_system.TestcontainersConfiguration;
import com.miftah.core_bank_system.auth.AuthService;
import com.miftah.core_bank_system.auth.LoginRequest;
import com.miftah.core_bank_system.auth.RegisterRequest;
import com.miftah.core_bank_system.profile.Gender;
import com.miftah.core_bank_system.profile.ProfileRequest;
import com.miftah.core_bank_system.profile.ProfileType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class UserControllerTest {

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

    private String adminToken;

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, Locale.getDefault());
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE notifications, audit_logs, transactions, accounts, refresh_tokens, profiles, users RESTART IDENTITY CASCADE");

        // Create an admin user manually & login
        RegisterRequest adminReg = RegisterRequest.builder()
                .username("adminuser")
                .password("password")
                .build();
        authService.register(adminReg);

        User adminUser = userRepository.findByUsername("adminuser").orElseThrow();
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);

        adminToken = authService.login(LoginRequest.builder()
                .username("adminuser")
                .password("password")
                .build()).getToken();
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

    @Test
    void createAdmin_Success_ShouldReturnCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("newadmin")
                .password("password")
                .build();

        mockMvc.perform(post("/api/v1/users/admin")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is(201)))
                .andExpect(jsonPath("$.message", is(getMessage("success.create"))))
                .andExpect(jsonPath("$.data.username", is("newadmin")))
                .andExpect(jsonPath("$.data.role", is("ADMIN")));

        assertTrue(userRepository.existsByUsername("newadmin"));
    }

    @Test
    void createAdmin_DuplicateUsername_ShouldReturnBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("adminuser") // already created in setup
                .password("password")
                .build();

        mockMvc.perform(post("/api/v1/users/admin")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(getMessage("error.validation")))
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void createUser_Success_ShouldReturnCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("regularuser")
                .password("password")
                .build();

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is(201)))
                .andExpect(jsonPath("$.message", is(getMessage("success.create"))))
                .andExpect(jsonPath("$.data.username", is("regularuser")))
                .andExpect(jsonPath("$.data.role", is("USER")));

        assertTrue(userRepository.existsByUsername("regularuser"));
    }

    @Test
    void createUser_DuplicateUsername_ShouldReturnBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("adminuser") // already exists
                .password("password")
                .build();

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void createUserWithProfile_Success_ShouldReturnCreated() throws Exception {
        CreateUserWithProfileRequest request = CreateUserWithProfileRequest.builder()
                .user(RegisterRequest.builder().username("newuser").password("password").build())
                .profile(createValidProfileRequest())
                .build();

        mockMvc.perform(post("/api/v1/users/profile")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is(201)))
                .andExpect(jsonPath("$.message", is(getMessage("success.create"))))
                .andExpect(jsonPath("$.data.username", is("newuser")))
                .andExpect(jsonPath("$.data.role", is("USER")));

        assertTrue(userRepository.existsByUsername("newuser"));
    }

    @Test
    void updateUser_Success_ShouldReturnOk() throws Exception {
        // Create standard user via auth service
        authService.register(RegisterRequest.builder().username("olduser").password("password").build());
        User user = userRepository.findByUsername("olduser").orElseThrow();

        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("updateduser")
                .password("newpassword")
                .build();

        mockMvc.perform(put("/api/v1/users/" + user.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", is(getMessage("success.update"))))
                .andExpect(jsonPath("$.data.username", is("updateduser")));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(updatedUser.getUsername().equals("updateduser"));
    }

    @Test
    void updateAdmin_Success_ShouldReturnOk() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("superadmin")
                .password("newpassword")
                .build();

        User adminUser = userRepository.findByUsername("adminuser").orElseThrow();

        mockMvc.perform(put("/api/v1/users/admin/" + adminUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", is(getMessage("success.update"))))
                .andExpect(jsonPath("$.data.username", is("superadmin")));

        User updatedUser = userRepository.findById(adminUser.getId()).orElseThrow();
        assertTrue(updatedUser.getUsername().equals("superadmin"));
    }

    @Test
    void deleteAdmin_Success_ShouldReturnOk() throws Exception {
        // create dummy admin to delete
        authService.register(RegisterRequest.builder().username("deleteadmin").password("pwd").build());
        User dummyAdmin = userRepository.findByUsername("deleteadmin").orElseThrow();

        mockMvc.perform(delete("/api/v1/users/admin/" + dummyAdmin.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", is(getMessage("success.delete"))));

        assertTrue(userRepository.findById(dummyAdmin.getId()).isEmpty());
    }

    @Test
    void getAll_Success_ShouldReturnOk() throws Exception {
        authService.register(RegisterRequest.builder().username("user1").password("pwd").build());
        authService.register(RegisterRequest.builder().username("user2").password("pwd").build());

        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(3))); // 2 plus the adminuser
    }

    @Test
    void getById_Success_ShouldReturnOk() throws Exception {
        authService.register(RegisterRequest.builder().username("targetuser").password("pwd").build());
        User target = userRepository.findByUsername("targetuser").orElseThrow();

        mockMvc.perform(get("/api/v1/users/" + target.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.username", is("targetuser")));
    }
}
