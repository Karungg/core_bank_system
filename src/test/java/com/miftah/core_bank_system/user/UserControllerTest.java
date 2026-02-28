package com.miftah.core_bank_system.user;

import com.miftah.core_bank_system.TestcontainersConfiguration;
import com.miftah.core_bank_system.account.AccountRepository;
import com.miftah.core_bank_system.auth.RegisterRequest;
import com.miftah.core_bank_system.profile.Gender;
import com.miftah.core_bank_system.profile.ProfileRepository;
import com.miftah.core_bank_system.profile.ProfileRequest;
import com.miftah.core_bank_system.profile.ProfileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.MessageSource;

import java.time.LocalDate;
import java.util.Locale;

import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@WithMockUser(username = "admin", password = "password", roles = "ADMIN")
public class UserControllerTest {

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
                                .username("admin")
                                .password("password")
                                .build();

                mockMvc.perform(post("/api/users/admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.code", is(201)))
                                .andExpect(jsonPath("$.message", is(getMessage("success.create"))))
                                .andExpect(jsonPath("$.data.username", is("admin")))
                                .andExpect(jsonPath("$.data.role", is("ADMIN")));

                assertTrue(userRepository.existsByUsername("admin"));
        }

        @Test
        void createAdmin_DuplicateUsername_ShouldReturnBadRequest() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .username("admin")
                                .password("password")
                                .build();

                userRepository.save(User.builder().username("admin").password("pwd").role(Role.ADMIN).build());

                mockMvc.perform(post("/api/users/admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createUser_Success_ShouldReturnCreated() throws Exception {
                RegisterRequest request = RegisterRequest.builder()
                                .username("regularuser")
                                .password("password")
                                .build();

                mockMvc.perform(post("/api/users")
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
                                .username("regularuser")
                                .password("password")
                                .build();

                userRepository.save(User.builder().username("regularuser").password("pwd").role(Role.USER).build());

                mockMvc.perform(post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createUserWithProfile_Success_ShouldReturnCreated() throws Exception {
                ProfileRequest profileRequest = createValidProfileRequest();

                RegisterRequest userRequest = RegisterRequest.builder()
                                .username("newuser")
                                .password("password")
                                .build();

                CreateUserWithProfileRequest request = CreateUserWithProfileRequest.builder()
                                .user(userRequest)
                                .profile(profileRequest)
                                .build();

                mockMvc.perform(post("/api/users/profile")
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
        void createUserWithProfile_DuplicateUsername_ShouldReturnBadRequest() throws Exception {
                ProfileRequest profileRequest = createValidProfileRequest();

                // Create duplicate user manually
                userRepository.save(User.builder().username("newuser").password("pwd").role(Role.USER).build());

                RegisterRequest userRequest = RegisterRequest.builder()
                                .username("newuser")
                                .password("password")
                                .build();

                CreateUserWithProfileRequest request = CreateUserWithProfileRequest.builder()
                                .user(userRequest)
                                .profile(profileRequest)
                                .build();

                mockMvc.perform(post("/api/users/profile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void updateAdmin_Success_ShouldReturnOk() throws Exception {
                User admin = userRepository
                                .save(User.builder().username("admin").password("password").role(Role.ADMIN).build());

                UpdateUserRequest request = UpdateUserRequest.builder()
                                .username("newadminname")
                                .password("newpassword")
                                .build();

                mockMvc.perform(put("/api/users/admin/" + admin.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(200)))
                                .andExpect(jsonPath("$.message", is(getMessage("success.update"))))
                                .andExpect(jsonPath("$.data.username", is("newadminname")));

                User updatedUser = userRepository.findById(admin.getId()).orElseThrow();
                assertTrue(updatedUser.getUsername().equals("newadminname"));
        }

        @Test
        void deleteAdmin_Success_ShouldReturnOk() throws Exception {
                User admin = userRepository
                                .save(User.builder().username("todelete").password("password").role(Role.ADMIN)
                                                .build());

                mockMvc.perform(delete("/api/users/admin/" + admin.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(200)))
                                .andExpect(jsonPath("$.message", is(getMessage("success.delete"))));

                assertTrue(userRepository.findById(admin.getId()).isEmpty());
        }

        @Test
        void getAll_Success_ShouldReturnOk() throws Exception {
                userRepository.save(User.builder().username("user1").password("pwd").role(Role.USER).build());
                userRepository.save(User.builder().username("user2").password("pwd").role(Role.USER).build());

                mockMvc.perform(get("/api/users")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(200)))
                                .andExpect(jsonPath("$.message", is(getMessage("success.get"))))
                                .andExpect(jsonPath("$.data.content").isArray())
                                .andExpect(jsonPath("$.data.totalElements")
                                                .value(greaterThanOrEqualTo(2)));
        }

        @Test
        void getById_Success_ShouldReturnOk() throws Exception {
                User user = userRepository
                                .save(User.builder().username("targetuser").password("pwd").role(Role.USER).build());

                mockMvc.perform(get("/api/users/" + user.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(200)))
                                .andExpect(jsonPath("$.message", is(getMessage("success.get"))))
                                .andExpect(jsonPath("$.data.username", is("targetuser")));
        }
}
