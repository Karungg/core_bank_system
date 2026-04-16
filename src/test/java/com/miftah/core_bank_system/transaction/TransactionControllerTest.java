package com.miftah.core_bank_system.transaction;

import com.miftah.core_bank_system.TestcontainersConfiguration;
import com.miftah.core_bank_system.account.Account;
import com.miftah.core_bank_system.account.AccountRepository;
import com.miftah.core_bank_system.account.AccountStatus;
import com.miftah.core_bank_system.auth.AuthService;
import com.miftah.core_bank_system.auth.LoginRequest;
import com.miftah.core_bank_system.auth.RegisterRequest;
import com.miftah.core_bank_system.auth.TokenResponse;
import com.miftah.core_bank_system.user.Role;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, Locale.getDefault());
    }

    private String userToken;
    private String otherUserToken;
    private String adminToken;

    private Account userAccount;
    private Account otherUserAccount;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE notifications, audit_logs, transactions, accounts, refresh_tokens, profiles, users RESTART IDENTITY CASCADE");

        // 1. Setup normal user
        authService.register(RegisterRequest.builder().username("testuser").password("password").build());
        userToken = authService.login(LoginRequest.builder().username("testuser").password("password").build()).getToken();
        User testUser = userRepository.findByUsername("testuser").orElseThrow();

        userAccount = Account.builder()
                .user(testUser)
                .accountNumber("1234567890")
                .cardNumber("1111222233334444")
                .cvv("123")
                .expiredDate(LocalDate.now().plusYears(3))
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .type(com.miftah.core_bank_system.account.AccountType.SILVER)
                .pin(passwordEncoder.encode("123456"))
                .failedPinAttempts(0)
                .build();
        accountRepository.save(userAccount);

        // 2. Setup another user for transfers
        authService.register(RegisterRequest.builder().username("otheruser").password("password").build());
        otherUserToken = authService.login(LoginRequest.builder().username("otheruser").password("password").build()).getToken();
        User otherUser = userRepository.findByUsername("otheruser").orElseThrow();

        otherUserAccount = Account.builder()
                .user(otherUser)
                .accountNumber("0987654321")
                .cardNumber("5555666677778888")
                .cvv("999")
                .expiredDate(LocalDate.now().plusYears(3))
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .type(com.miftah.core_bank_system.account.AccountType.SILVER)
                .pin(passwordEncoder.encode("654321"))
                .failedPinAttempts(0)
                .build();
        accountRepository.save(otherUserAccount);

        // 3. Setup ADMIN user
        authService.register(RegisterRequest.builder().username("admin").password("password").build());
        User adminUser = userRepository.findByUsername("admin").orElseThrow();
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);
        adminToken = authService.login(LoginRequest.builder().username("admin").password("password").build()).getToken();
    }

    // ==========================================
    // TRANSFER
    // ==========================================

    @Test
    void transfer_Success() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(userAccount.getId())
                .toAccountId(otherUserAccount.getId())
                .amount(new BigDecimal("500.00"))
                .pin("123456")
                .build();

        mockMvc.perform(post("/api/v1/transactions/transfer")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value(getMessage("success.create")))
                .andExpect(jsonPath("$.data.amount").value("500.0"))
                .andExpect(jsonPath("$.data.type").value("TRANSFER"));
    }

    @Test
    void transfer_InvalidPin_ReturnsUnauthorized() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(userAccount.getId())
                .toAccountId(otherUserAccount.getId())
                .amount(new BigDecimal("500.00"))
                .pin("000000") // Wrong PIN
                .build();

        mockMvc.perform(post("/api/v1/transactions/transfer")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void transfer_UnauthorizedUser_ReturnsForbidden() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(userAccount.getId()) // using testuser's account
                .toAccountId(otherUserAccount.getId())
                .amount(new BigDecimal("500.00"))
                .pin("123456")
                .build();

        // But authenticating as other user
        mockMvc.perform(post("/api/v1/transactions/transfer")
                .header("Authorization", "Bearer " + otherUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden"))
                .andExpect(jsonPath("$.errors").value(getMessage("error.transaction.unauthorized")));
    }


    // ==========================================
    // DEPOSIT
    // ==========================================

    @Test
    void deposit_Success_Admin() throws Exception {
        DepositRequest request = DepositRequest.builder()
                .accountId(userAccount.getId())
                .amount(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(post("/api/v1/transactions/deposit")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.amount").value("1000.0"));
    }

    @Test
    void deposit_Forbidden_NormalUser() throws Exception {
        DepositRequest request = DepositRequest.builder()
                .accountId(userAccount.getId())
                .amount(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(post("/api/v1/transactions/deposit")
                .header("Authorization", "Bearer " + userToken) // only admin can deposit
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // handled by spring security correctly
    }


    // ==========================================
    // WITHDRAWAL
    // ==========================================

    @Test
    void withdrawal_Success() throws Exception {
        WithdrawalRequest request = WithdrawalRequest.builder()
                .accountId(userAccount.getId())
                .amount(new BigDecimal("1000.00"))
                .pin("123456")
                .build();

        mockMvc.perform(post("/api/v1/transactions/withdrawal")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.data.amount").value("1000.0"));
    }

    @Test
    void withdrawal_InsufficientBalance_ReturnsBadRequest() throws Exception {
        WithdrawalRequest request = WithdrawalRequest.builder()
                .accountId(userAccount.getId())
                .amount(new BigDecimal("100000.00")) // Too much!
                .pin("123456")
                .build();

        mockMvc.perform(post("/api/v1/transactions/withdrawal")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    // ==========================================
    // GET QUERIES
    // ==========================================

    @Test
    void getTransactions_Admin_Success() throws Exception {
        // Pre-create a deposit
        DepositRequest request = DepositRequest.builder()
                .accountId(userAccount.getId())
                .amount(new BigDecimal("1000.00"))
                .build();
        mockMvc.perform(post("/api/v1/transactions/deposit")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/v1/transactions")
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "0")
                .param("type", "DEPOSIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void getMyTransactions_User_Success() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/me")
                .header("Authorization", "Bearer " + userToken)
                .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
