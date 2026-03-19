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
import org.springframework.security.crypto.password.PasswordEncoder;
import com.miftah.core_bank_system.exception.InsufficientBalanceException;
import com.miftah.core_bank_system.exception.InvalidPinException;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import com.miftah.core_bank_system.exception.SameAccountTransactionException;
import com.miftah.core_bank_system.exception.UnauthorizedTransactionException;

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

    @Mock
    private PasswordEncoder passwordEncoder;

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
                .pin("$2a$10$encodedPin") // stored as encoded
                .build();

        toAccount = Account.builder()
                .id(UUID.randomUUID())
                .user(otherUser)
                .accountNumber("0987654321")
                .balance(new BigDecimal("500"))
                .pin("$2a$10$encodedPin2")
                .build();

        request = TransactionRequest.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(new BigDecimal("100"))
                .pin("123456") // raw PIN supplied by user
                .build();
    }

    @Test
    void createTransaction_Success() {
        when(accountRepository.findByIdForUpdate(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(toAccount.getId())).thenReturn(Optional.of(toAccount));
        when(passwordEncoder.matches(request.getPin(), fromAccount.getPin())).thenReturn(true);
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
        lenient().when(accountRepository.findByIdForUpdate(fromAccount.getId())).thenReturn(Optional.empty());
        lenient().when(accountRepository.findByIdForUpdate(toAccount.getId())).thenReturn(Optional.of(toAccount));

        assertThrows(ResourceNotFoundException.class, () -> transactionService.createTransaction(user, request));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_ToAccountNotFound() {
        lenient().when(accountRepository.findByIdForUpdate(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        lenient().when(accountRepository.findByIdForUpdate(toAccount.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.createTransaction(user, request));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_Unauthorized() {
        // fromAccount belongs to otherUser, but we try to transact as 'user'
        fromAccount.setUser(otherUser);
        when(accountRepository.findByIdForUpdate(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(toAccount.getId())).thenReturn(Optional.of(toAccount));

        assertThrows(UnauthorizedTransactionException.class, () -> transactionService.createTransaction(user, request));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_SameAccount() {
        // toAccount is set the same as fromAccount
        request.setToAccountId(fromAccount.getId());
        
        when(accountRepository.findByIdForUpdate(fromAccount.getId())).thenReturn(Optional.of(fromAccount));

        assertThrows(SameAccountTransactionException.class, () -> transactionService.createTransaction(user, request));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_InsufficientBalance() {
        request.setAmount(new BigDecimal("2000")); // Balance is 1000
        when(accountRepository.findByIdForUpdate(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(toAccount.getId())).thenReturn(Optional.of(toAccount));
        when(passwordEncoder.matches(request.getPin(), fromAccount.getPin())).thenReturn(true);

        assertThrows(InsufficientBalanceException.class, () -> transactionService.createTransaction(user, request));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_InvalidPin() {
        when(accountRepository.findByIdForUpdate(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(toAccount.getId())).thenReturn(Optional.of(toAccount));
        when(passwordEncoder.matches(request.getPin(), fromAccount.getPin())).thenReturn(false);

        assertThrows(InvalidPinException.class, () -> transactionService.createTransaction(user, request));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}