package com.miftah.core_bank_system.transaction;

import com.miftah.core_bank_system.TestcontainersConfiguration;
import com.miftah.core_bank_system.account.Account;
import com.miftah.core_bank_system.account.AccountRepository;
import com.miftah.core_bank_system.account.AccountType;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.MessageSource;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
public class TransactionControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private AccountRepository accountRepository;

        @Autowired
        private TransactionRepository transactionRepository;

        @Autowired
        private AuthService authService;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private MessageSource messageSource;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private String getMessage(String code) {
                return messageSource.getMessage(code, null, Locale.getDefault());
        }

        private User user1;
        private User user2;
        private Account account1;
        private Account account2;
        private String token1;

        @BeforeEach
        void setUp() {
                transactionRepository.deleteAll();
                accountRepository.deleteAll();
                userRepository.deleteAll();

                // Register User 1
                RegisterRequest registerRequest1 = new RegisterRequest("user1", "password");
                authService.register(registerRequest1);
                user1 = userRepository.findByUsername("user1").orElseThrow();

                // Login User 1
                TokenResponse tokenResponse = authService.login(new LoginRequest("user1", "password"));
                token1 = tokenResponse.getToken();

                // Register User 2
                RegisterRequest registerRequest2 = new RegisterRequest("user2", "password");
                authService.register(registerRequest2);
                user2 = userRepository.findByUsername("user2").orElseThrow();

                // Create Account for User 1
                account1 = Account.builder()
                                .user(user1)
                                .accountNumber("111111")
                                .balance(new BigDecimal("1000"))
                                .cardNumber("1234567890123456")
                                .cvv("123")
                                .pin(passwordEncoder.encode("123456"))
                                .type(AccountType.SILVER)
                                .createdAt(LocalDate.now())
                                .updatedAt(LocalDate.now())
                                .build();
                accountRepository.save(account1);

                // Create Account for User 2
                account2 = Account.builder()
                                .user(user2)
                                .accountNumber("222222")
                                .balance(new BigDecimal("500"))
                                .cardNumber("6543210987654321")
                                .cvv("456")
                                .pin(passwordEncoder.encode("654321"))
                                .type(AccountType.GOLD)
                                .createdAt(LocalDate.now())
                                .updatedAt(LocalDate.now())
                                .build();
                accountRepository.save(account2);
        }

        private TransactionRequest createValidTransactionRequest() {
                return TransactionRequest.builder()
                                .fromAccountId(account1.getId())
                                .toAccountId(account2.getId())
                                .amount(new BigDecimal("100"))
                                .pin("123456")
                                .build();
        }

        @Test
        void createTransaction_Success() throws Exception {
                TransactionRequest request = createValidTransactionRequest();

                mockMvc.perform(post("/api/transactions")
                                .header("Authorization", "Bearer " + token1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.code", is(201)))
                                .andExpect(jsonPath("$.message", is(getMessage("success.create"))))
                                .andExpect(jsonPath("$.data.amount", is(100)))
                                .andExpect(jsonPath("$.data.fromAccountId", is(account1.getId().toString())))
                                .andExpect(jsonPath("$.data.toAccountId", is(account2.getId().toString())));
        }

        @Test
        void createTransaction_Unauthorized_NoToken() throws Exception {
                TransactionRequest request = createValidTransactionRequest();

                mockMvc.perform(post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void createTransaction_Unauthorized_WrongUser() throws Exception {
                // User 1 trying to send money from Account 2 (which belongs to User 2)
                TransactionRequest request = TransactionRequest.builder()
                                .fromAccountId(account2.getId())
                                .toAccountId(account1.getId())
                                .amount(new BigDecimal("100"))
                                .pin("654321")
                                .build();

                mockMvc.perform(post("/api/transactions")
                                .header("Authorization", "Bearer " + token1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code", is(403))); // Assuming 403 Forbidden is mapped to a code
        }

        @Test
        void createTransaction_InsufficientBalance() throws Exception {
                TransactionRequest request = TransactionRequest.builder()
                                .fromAccountId(account1.getId())
                                .toAccountId(account2.getId())
                                .amount(new BigDecimal("2000")) // More than balance (1000)
                                .pin("123456")
                                .build();

                mockMvc.perform(post("/api/transactions")
                                .header("Authorization", "Bearer " + token1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code", is(400)));
        }

        @Test
        void createTransaction_AccountNotFound() throws Exception {
                TransactionRequest request = TransactionRequest.builder()
                                .fromAccountId(UUID.randomUUID()) // Random non-existent account
                                .toAccountId(account2.getId())
                                .amount(new BigDecimal("100"))
                                .pin("123456")
                                .build();

                mockMvc.perform(post("/api/transactions")
                                .header("Authorization", "Bearer " + token1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code", is(404)));
        }

        @Test
        void createTransaction_InvalidPin() throws Exception {
                TransactionRequest request = TransactionRequest.builder()
                                .fromAccountId(account1.getId())
                                .toAccountId(account2.getId())
                                .amount(new BigDecimal("100"))
                                .pin("000000") // Wrong PIN
                                .build();

                mockMvc.perform(post("/api/transactions")
                                .header("Authorization", "Bearer " + token1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code", is(401)));
        }
}
