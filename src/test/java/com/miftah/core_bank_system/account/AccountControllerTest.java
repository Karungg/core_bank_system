package com.miftah.core_bank_system.account;

import com.miftah.core_bank_system.TestcontainersConfiguration;
import com.miftah.core_bank_system.auth.AuthService;
import com.miftah.core_bank_system.auth.LoginRequest;
import com.miftah.core_bank_system.auth.RegisterRequest;
import com.miftah.core_bank_system.auth.TokenResponse;
import com.miftah.core_bank_system.user.User;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class AccountControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private AccountRepository accountRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private AuthService authService;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private MessageSource messageSource;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private User user;
        private String userToken;

        private String getMessage(String code) {
                return messageSource.getMessage(code, null, Locale.getDefault());
        }

        @BeforeEach
        void setUp() {
                jdbcTemplate.execute("TRUNCATE TABLE notifications, audit_logs, transactions, accounts, refresh_tokens, profiles, users RESTART IDENTITY CASCADE");

                RegisterRequest registerRequest = RegisterRequest.builder()
                                .username("testuser")
                                .password("password")
                                .build();
                authService.register(registerRequest);

                user = userRepository.findByUsername("testuser").orElseThrow();

                TokenResponse tokenResponse = authService.login(LoginRequest.builder()
                                .username("testuser")
                                .password("password")
                                .build());
                userToken = tokenResponse.getToken();
        }

        private Account createTestAccount() {
                return createTestAccount(AccountStatus.ACTIVE);
        }

        private Account createTestAccount(AccountStatus status) {
                return accountRepository.save(Account.builder()
                                .user(user)
                                .accountNumber("1234567890")
                                .balance(new BigDecimal("1000.00"))
                                .pin(passwordEncoder.encode("123456"))
                                .cardNumber("1234-5678-9012-3456")
                                .cvv("encrypted_cvv_123")
                                .type(AccountType.SILVER)
                                .expiredDate(LocalDate.now().plusYears(5))
                                .status(status)
                                .failedPinAttempts(0)
                                .build());
        }

        // ==========================================
        // POST /api/v1/accounts (ADMIN)
        // ==========================================

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void create_Success() throws Exception {
                AccountRequest request = AccountRequest.builder()
                                .userId(user.getId())
                                .pin("123456")
                                .type(AccountType.SILVER)
                                .build();

                mockMvc.perform(post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.code").value(201))
                        .andExpect(jsonPath("$.message").value(getMessage("success.create")))
                        .andExpect(jsonPath("$.data.accountNumber").exists())
                        .andExpect(jsonPath("$.data.type").value("SILVER"))
                        .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void create_ValidationFail_MissingFields() throws Exception {
                AccountRequest request = AccountRequest.builder()
                                .userId(user.getId())
                                .build();

                mockMvc.perform(post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(400))
                        .andExpect(jsonPath("$.message").value(getMessage("error.validation")));
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void create_ValidationFail_PinTooShort() throws Exception {
                AccountRequest request = AccountRequest.builder()
                                .userId(user.getId())
                                .pin("123")
                                .type(AccountType.SILVER)
                                .build();

                mockMvc.perform(post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(400))
                        .andExpect(jsonPath("$.message").value(getMessage("error.validation")));
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void create_UserNotFound_ShouldReturnNotFound() throws Exception {
                AccountRequest request = AccountRequest.builder()
                                .userId(UUID.randomUUID())
                                .pin("123456")
                                .type(AccountType.SILVER)
                                .build();

                mockMvc.perform(post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isNotFound());
        }

        // ==========================================
        // GET /api/v1/accounts/{id} (ADMIN)
        // ==========================================

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void getById_Success() throws Exception {
                Account account = createTestAccount();

                mockMvc.perform(get("/api/v1/accounts/" + account.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.message").value(getMessage("success.get")))
                        .andExpect(jsonPath("$.data.id").value(account.getId().toString()))
                        .andExpect(jsonPath("$.data.accountNumber").value("1234567890"));
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void getById_NotFound_ShouldReturn404() throws Exception {
                mockMvc.perform(get("/api/v1/accounts/" + UUID.randomUUID()))
                        .andExpect(status().isNotFound());
        }

        // ==========================================
        // GET /api/v1/accounts (ADMIN)
        // ==========================================

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void getAll_Success() throws Exception {
                createTestAccount();

                mockMvc.perform(get("/api/v1/accounts"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.message").value(getMessage("success.get")))
                        .andExpect(jsonPath("$.data.content", hasSize(1)));
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void getAll_Empty_ShouldReturnEmptyPage() throws Exception {
                mockMvc.perform(get("/api/v1/accounts"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.content", hasSize(0)))
                        .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        // ==========================================
        // GET /api/v1/accounts/{id}/mutations (ADMIN)
        // ==========================================

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void getMutationsAdmin_Success_EmptyPage() throws Exception {
                Account account = createTestAccount();

                mockMvc.perform(get("/api/v1/accounts/" + account.getId() + "/mutations"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        // ==========================================
        // GET /api/v1/accounts/me (USER - JWT)
        // ==========================================

        @Test
        void getMyAccounts_Success() throws Exception {
                Account account = createTestAccount();

                mockMvc.perform(get("/api/v1/accounts/me")
                                .header("Authorization", "Bearer " + userToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.message").value(getMessage("success.get")))
                        .andExpect(jsonPath("$.data[0].id").value(account.getId().toString()))
                        .andExpect(jsonPath("$.data[0].userId").value(user.getId().toString()));
        }

        // ==========================================
        // GET /api/v1/accounts/me/{accountId} (USER - JWT)
        // ==========================================

        @Test
        void getMyAccountById_Success() throws Exception {
                Account account = createTestAccount();

                mockMvc.perform(get("/api/v1/accounts/me/" + account.getId())
                                .header("Authorization", "Bearer " + userToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.data.id").value(account.getId().toString()));
        }

        // ==========================================
        // GET /api/v1/accounts/me/{accountId}/balance (USER - JWT)
        // ==========================================

        @Test
        void getMyBalance_Success() throws Exception {
                Account account = createTestAccount();

                mockMvc.perform(get("/api/v1/accounts/me/" + account.getId() + "/balance")
                                .header("Authorization", "Bearer " + userToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.data.balance").value(1000.00))
                        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                        .andExpect(jsonPath("$.data.accountNumber").value("1234567890"));
        }

        // ==========================================
        // GET /api/v1/accounts/me/{accountId}/mutations (USER - JWT)
        // ==========================================

        @Test
        void getMyMutations_Success_EmptyPage() throws Exception {
                Account account = createTestAccount();

                mockMvc.perform(get("/api/v1/accounts/me/" + account.getId() + "/mutations")
                                .header("Authorization", "Bearer " + userToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        // ==========================================
        // PUT /api/v1/accounts/me/{accountId}/pin (USER - JWT)
        // ==========================================

        @Test
        void changePin_Success() throws Exception {
                Account account = createTestAccount();

                ChangePinRequest request = ChangePinRequest.builder()
                                .oldPin("123456")
                                .newPin("654321")
                                .confirmPin("654321")
                                .build();

                mockMvc.perform(put("/api/v1/accounts/me/" + account.getId() + "/pin")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.message").value(getMessage("success.update")));
        }

        @Test
        void changePin_ValidationFail_MissingFields() throws Exception {
                Account account = createTestAccount();

                ChangePinRequest request = ChangePinRequest.builder().build();

                mockMvc.perform(put("/api/v1/accounts/me/" + account.getId() + "/pin")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(400))
                        .andExpect(jsonPath("$.message").value(getMessage("error.validation")));
        }

        // ==========================================
        // PUT /api/v1/accounts/{id} (ADMIN)
        // ==========================================

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void update_Success() throws Exception {
                Account account = createTestAccount();

                AccountRequest updateRequest = AccountRequest.builder()
                                .userId(user.getId())
                                .pin("654321")
                                .type(AccountType.BLACK)
                                .build();

                mockMvc.perform(put("/api/v1/accounts/" + account.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.message").value(getMessage("success.update")))
                        .andExpect(jsonPath("$.data.accountNumber").value("1234567890"))
                        .andExpect(jsonPath("$.data.type").value("BLACK"));
        }

        // ==========================================
        // PATCH /api/v1/accounts/{id}/status (ADMIN)
        // ==========================================

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void updateStatus_ActiveToSuspended_ShouldSucceed() throws Exception {
                Account account = createTestAccount();

                UpdateAccountStatusRequest request = UpdateAccountStatusRequest.builder()
                                .status(AccountStatus.SUSPENDED).build();

                mockMvc.perform(patch("/api/v1/accounts/" + account.getId() + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.message").value(getMessage("success.update")))
                        .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void updateStatus_ActiveToFrozen_ShouldSucceed() throws Exception {
                Account account = createTestAccount();

                UpdateAccountStatusRequest request = UpdateAccountStatusRequest.builder()
                                .status(AccountStatus.FROZEN).build();

                mockMvc.perform(patch("/api/v1/accounts/" + account.getId() + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("FROZEN"));
        }

        // ==========================================
        // DELETE /api/v1/accounts/{id} (ADMIN)
        // ==========================================

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void delete_Success() throws Exception {
                Account account = createTestAccount();

                mockMvc.perform(delete("/api/v1/accounts/" + account.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.message").value(getMessage("success.delete")));

                // Verify soft-delete: status should be CLOSED
                mockMvc.perform(get("/api/v1/accounts/" + account.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("CLOSED"));
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void delete_AlreadyClosed_ShouldStillReturnOk() throws Exception {
                Account account = createTestAccount(AccountStatus.CLOSED);

                mockMvc.perform(delete("/api/v1/accounts/" + account.getId()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200));
        }
}
