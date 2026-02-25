package com.miftah.core_bank_system.account;

import com.miftah.core_bank_system.exception.DuplicateResourceException;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User user;
    private AccountRequest accountRequest;
    private Account account;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .password("encoded_password")
                .build();

        accountRequest = AccountRequest.builder()
                .userId(user.getId())
                .accountNumber("1234567890")
                .balance(new BigDecimal("1000.00"))
                .pin("123456")
                .cardNumber("1234-5678-9012-3456")
                .cvv("123")
                .type(AccountType.BLACK)
                .build();

        account = Account.builder()
                .id(UUID.randomUUID())
                .user(user)
                .accountNumber(accountRequest.getAccountNumber())
                .balance(accountRequest.getBalance())
                .pin("encoded_pin")
                .cardNumber(accountRequest.getCardNumber())
                .cvv(accountRequest.getCvv())
                .type(accountRequest.getType())
                .build();
    }

    @Test
    void create_Success() {
        when(userRepository.findById(accountRequest.getUserId())).thenReturn(Optional.of(user));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.existsByCardNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pin");
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.create(accountRequest);

        assertNotNull(response);
        assertEquals(account.getId(), response.getId());
        assertEquals(accountRequest.getAccountNumber(), response.getAccountNumber());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void create_UserNotFound() {
        when(userRepository.findById(accountRequest.getUserId())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> accountService.create(accountRequest));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void create_DuplicateAccountNumber() {
        when(userRepository.findById(accountRequest.getUserId())).thenReturn(Optional.of(user));
        when(accountRepository.existsByAccountNumber(accountRequest.getAccountNumber())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> accountService.create(accountRequest));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getById_Success() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getById(account.getId());

        assertNotNull(response);
        assertEquals(account.getId(), response.getId());
    }

    @Test
    void getById_NotFound() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> accountService.getById(id));
    }

    @Test
    void getAll_Success() {
        Page<Account> page = new PageImpl<>(Collections.singletonList(account));
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<AccountResponse> response = accountService.getAll(Pageable.unpaged());

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void update_Success() {
        AccountRequest updateRequest = AccountRequest.builder()
                .userId(user.getId())
                .accountNumber("0987654321")
                .balance(new BigDecimal("2000.00"))
                .pin("654321")
                .cardNumber("9876-5432-1098-7654")
                .cvv("321")
                .type(AccountType.GOLD)
                .build();

        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(userRepository.findById(updateRequest.getUserId())).thenReturn(Optional.of(user));
        when(accountRepository.existsByAccountNumberAndIdNot(anyString(), any(UUID.class))).thenReturn(false);
        when(accountRepository.existsByCardNumberAndIdNot(anyString(), any(UUID.class))).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("new_encoded_pin");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.update(account.getId(), updateRequest);

        assertNotNull(response);
        assertEquals(updateRequest.getAccountNumber(), response.getAccountNumber());
        assertEquals(AccountType.GOLD, response.getType());
    }

    @Test
    void delete_Success() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        accountService.delete(account.getId());

        verify(accountRepository).delete(account);
    }
}
