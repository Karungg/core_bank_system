package com.miftah.core_bank_system.transaction;

import com.miftah.core_bank_system.account.Account;
import com.miftah.core_bank_system.account.AccountRepository;
import com.miftah.core_bank_system.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User user;
    private User otherUser;
    private Account fromAccount;
    private Account toAccount;
    private TransactionRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("otheruser")
                .build();

        fromAccount = Account.builder()
                .id(UUID.randomUUID())
                .user(user)
                .accountNumber("1234567890")
                .balance(new BigDecimal("1000"))
                .build();

        toAccount = Account.builder()
                .id(UUID.randomUUID())
                .user(otherUser)
                .accountNumber("0987654321")
                .balance(new BigDecimal("500"))
                .build();

        request = TransactionRequest.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(new BigDecimal("100"))
                .build();
    }

    @Test
    void createTransaction_Success() {
        when(accountRepository.findById(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccount.getId())).thenReturn(Optional.of(toAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(UUID.randomUUID());
            transaction.setCreatedAt(Instant.now());
            transaction.setUpdatedAt(Instant.now());
            return transaction;
        });

        TransactionResponse response = transactionService.createTransaction(user, request);

        assertNotNull(response);
        assertEquals(user.getId(), response.getUserId());
        assertEquals(request.getAmount(), response.getAmount());
        assertEquals(fromAccount.getId(), response.getFromAccountId());
        assertEquals(toAccount.getId(), response.getToAccountId());

        // Verify balances updated
        assertEquals(new BigDecimal("900"), fromAccount.getBalance());
        assertEquals(new BigDecimal("600"), toAccount.getBalance());

        verify(accountRepository, times(1)).save(fromAccount);
        verify(accountRepository, times(1)).save(toAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createTransaction_FromAccountNotFound() {
        when(accountRepository.findById(fromAccount.getId())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> transactionService.createTransaction(user, request));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_ToAccountNotFound() {
        when(accountRepository.findById(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccount.getId())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> transactionService.createTransaction(user, request));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_Unauthorized() {
        // fromAccount belongs to otherUser, but we try to transact as 'user'
        fromAccount.setUser(otherUser);
        when(accountRepository.findById(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccount.getId())).thenReturn(Optional.of(toAccount));

        assertThrows(ResponseStatusException.class, () -> transactionService.createTransaction(user, request));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_InsufficientBalance() {
        request.setAmount(new BigDecimal("2000")); // Balance is 1000
        when(accountRepository.findById(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccount.getId())).thenReturn(Optional.of(toAccount));

        assertThrows(ResponseStatusException.class, () -> transactionService.createTransaction(user, request));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
