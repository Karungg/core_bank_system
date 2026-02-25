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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

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
        private ObjectMapper objectMapper;

        private String token;
        private User user;

        @BeforeEach
        void setUp() {
                accountRepository.deleteAll();
                userRepository.deleteAll();

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
                token = tokenResponse.getToken();
        }

        @Test
        void create_Success() throws Exception {
                AccountRequest request = AccountRequest.builder()
                                .userId(user.getId())
                                .accountNumber("1234567890")
                                .balance(new BigDecimal("1000.00"))
                                .pin("123456")
                                .cardNumber("1234-5678-9012-3456")
                                .cvv("123")
                                .type(AccountType.SILVER)
                                .build();

                mockMvc.perform(post("/api/accounts")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.code").value(201))
                                .andExpect(jsonPath("$.data.accountNumber").value(request.getAccountNumber()));
        }

        @Test
        void create_ValidationFail() throws Exception {
                AccountRequest request = AccountRequest.builder()
                                .userId(user.getId())
                                // Missing required fields
                                .build();

                mockMvc.perform(post("/api/accounts")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        void getById_Success() throws Exception {
                Account account = Account.builder()
                                .user(user)
                                .accountNumber("1234567890")
                                .balance(new BigDecimal("1000.00"))
                                .pin("encoded_pin")
                                .cardNumber("1234-5678-9012-3456")
                                .cvv("123")
                                .type(AccountType.SILVER)
                                .build();
                account = accountRepository.save(account);

                mockMvc.perform(get("/api/accounts/" + account.getId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.id").value(account.getId().toString()));
        }

        @Test
        void getAll_Success() throws Exception {
                Account account = Account.builder()
                                .user(user)
                                .accountNumber("1234567890")
                                .balance(new BigDecimal("1000.00"))
                                .pin("encoded_pin")
                                .cardNumber("1234-5678-9012-3456")
                                .cvv("123")
                                .type(AccountType.SILVER)
                                .build();
                accountRepository.save(account);

                mockMvc.perform(get("/api/accounts")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.content", hasSize(1)));
        }

        @Test
        void update_Success() throws Exception {
                Account account = Account.builder()
                                .user(user)
                                .accountNumber("1234567890")
                                .balance(new BigDecimal("1000.00"))
                                .pin("encoded_pin")
                                .cardNumber("1234-5678-9012-3456")
                                .cvv("123")
                                .type(AccountType.SILVER)
                                .build();
                account = accountRepository.save(account);

                AccountRequest updateRequest = AccountRequest.builder()
                                .userId(user.getId())
                                .accountNumber("0987654321")
                                .balance(new BigDecimal("2000.00"))
                                .pin("654321")
                                .cardNumber("9876-5432-1098-7654")
                                .cvv("321")
                                .type(AccountType.BLACK)
                                .build();

                mockMvc.perform(put("/api/accounts/" + account.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.accountNumber").value("0987654321"))
                                .andExpect(jsonPath("$.data.type").value("BLACK"));
        }

        @Test
        void delete_Success() throws Exception {
                Account account = Account.builder()
                                .user(user)
                                .accountNumber("1234567890")
                                .balance(new BigDecimal("1000.00"))
                                .pin("encoded_pin")
                                .cardNumber("1234-5678-9012-3456")
                                .cvv("123")
                                .type(AccountType.SILVER)
                                .build();
                account = accountRepository.save(account);

                mockMvc.perform(delete("/api/accounts/" + account.getId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/accounts/" + account.getId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound());
        }
}
