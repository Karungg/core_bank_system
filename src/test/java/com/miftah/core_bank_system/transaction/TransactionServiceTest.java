package com.miftah.core_bank_system.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.miftah.core_bank_system.account.Account;
import com.miftah.core_bank_system.account.AccountRepository;
import com.miftah.core_bank_system.account.AccountStatus;
import com.miftah.core_bank_system.audit.AuditAction;
import com.miftah.core_bank_system.audit.AuditService;
import com.miftah.core_bank_system.exception.AccountLockedException;
import com.miftah.core_bank_system.exception.AccountNotActiveException;
import com.miftah.core_bank_system.exception.InsufficientBalanceException;
import com.miftah.core_bank_system.exception.InvalidPinException;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import com.miftah.core_bank_system.exception.SameAccountTransactionException;
import com.miftah.core_bank_system.exception.UnauthorizedTransactionException;
import com.miftah.core_bank_system.notification.event.TransactionCompletedEvent;
import com.miftah.core_bank_system.user.User;

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

    @Mock
    private AuditService auditService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User user;
    private User otherUser;
    private Account fromAccount;
    private Account toAccount;

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
                .accountNumber("111122223333")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .pin("encodedPin")
                .failedPinAttempts(0)
                .build();

        toAccount = Account.builder()
                .id(UUID.randomUUID())
                .user(otherUser)
                .accountNumber("444455556666")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .build();
    }



    @Test
    void transfer_Success() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(new BigDecimal("200.00"))
                .pin("123456")
                .build();

        // ordered lookup mocking to support any UUID locking order
        when(accountRepository.findByIdForUpdate(any(UUID.class))).thenAnswer(invocation -> {
            UUID arg = invocation.getArgument(0);
            if (arg.equals(fromAccount.getId())) return Optional.of(fromAccount);
            if (arg.equals(toAccount.getId())) return Optional.of(toAccount);
            return Optional.empty();
        });

        when(passwordEncoder.matches("123456", "encodedPin")).thenReturn(true);

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TransactionResponse response = transactionService.transfer(user, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("200.00"), response.getAmount());
        assertEquals(TransactionType.TRANSFER, response.getType());
        
        assertEquals(new BigDecimal("800.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), toAccount.getBalance());

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(auditService).logAction(any(), any(), any());
        verify(applicationEventPublisher, times(2)).publishEvent(any(TransactionCompletedEvent.class)); // to both accounts
    }

    @Test
    void transfer_FromAccountNotFound_ThrowsException() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(UUID.randomUUID())
                .toAccountId(toAccount.getId())
                .amount(new BigDecimal("200.00"))
                .build();

        when(accountRepository.findByIdForUpdate(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.transfer(user, request));
    }

    @Test
    void transfer_ToAccountNotActive_ThrowsException() {
        toAccount.setStatus(AccountStatus.SUSPENDED);
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(new BigDecimal("200.00"))
                .build();

        when(accountRepository.findByIdForUpdate(any(UUID.class))).thenAnswer(invocation -> {
            UUID arg = invocation.getArgument(0);
            if (arg.equals(fromAccount.getId())) return Optional.of(fromAccount);
            if (arg.equals(toAccount.getId())) return Optional.of(toAccount);
            return Optional.empty();
        });

        assertThrows(AccountNotActiveException.class, () -> transactionService.transfer(user, request));
    }

    @Test
    void transfer_UnauthorizedUser_ThrowsException() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(new BigDecimal("200.00"))
                .build();

        when(accountRepository.findByIdForUpdate(any(UUID.class))).thenAnswer(invocation -> {
            UUID arg = invocation.getArgument(0);
            if (arg.equals(fromAccount.getId())) return Optional.of(fromAccount);
            if (arg.equals(toAccount.getId())) return Optional.of(toAccount);
            return Optional.empty();
        });

        assertThrows(UnauthorizedTransactionException.class, () -> transactionService.transfer(otherUser, request));
    }

    @Test
    void transfer_SameAccount_ThrowsException() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(fromAccount.getId())
                .amount(new BigDecimal("200.00"))
                .build();

        when(accountRepository.findByIdForUpdate(any(UUID.class))).thenReturn(Optional.of(fromAccount));

        assertThrows(SameAccountTransactionException.class, () -> transactionService.transfer(user, request));
    }

    @Test
    void transfer_InvalidPin_IncrementsFailedAttempts() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(new BigDecimal("200.00"))
                .pin("wrongPin")
                .build();

        when(accountRepository.findByIdForUpdate(any(UUID.class))).thenAnswer(invocation -> {
            UUID arg = invocation.getArgument(0);
            if (arg.equals(fromAccount.getId())) return Optional.of(fromAccount);
            if (arg.equals(toAccount.getId())) return Optional.of(toAccount);
            return Optional.empty();
        });

        when(passwordEncoder.matches("wrongPin", "encodedPin")).thenReturn(false);

        assertThrows(InvalidPinException.class, () -> transactionService.transfer(user, request));
        assertEquals(1, fromAccount.getFailedPinAttempts());
        verify(accountRepository).save(fromAccount);
    }

    @Test
    void transfer_InvalidPin_LocksAccountOnFifthAttempt() {
        fromAccount.setFailedPinAttempts(4);
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(new BigDecimal("200.00"))
                .pin("wrongPin")
                .build();

        when(accountRepository.findByIdForUpdate(any(UUID.class))).thenAnswer(invocation -> {
            UUID arg = invocation.getArgument(0);
            if (arg.equals(fromAccount.getId())) return Optional.of(fromAccount);
            if (arg.equals(toAccount.getId())) return Optional.of(toAccount);
            return Optional.empty();
        });

        when(passwordEncoder.matches("wrongPin", "encodedPin")).thenReturn(false);

        assertThrows(InvalidPinException.class, () -> transactionService.transfer(user, request));
        assertEquals(0, fromAccount.getFailedPinAttempts());
        assertNotNull(fromAccount.getPinLockedUntil());
        verify(accountRepository).save(fromAccount);
    }

    @Test
    void transfer_AccountLocked_ThrowsException() {
        fromAccount.setPinLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(new BigDecimal("200.00"))
                .build();

        when(accountRepository.findByIdForUpdate(any(UUID.class))).thenAnswer(invocation -> {
            UUID arg = invocation.getArgument(0);
            if (arg.equals(fromAccount.getId())) return Optional.of(fromAccount);
            if (arg.equals(toAccount.getId())) return Optional.of(toAccount);
            return Optional.empty();
        });

        assertThrows(AccountLockedException.class, () -> transactionService.transfer(user, request));
    }

    @Test
    void transfer_InsufficientBalance_ThrowsException() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(new BigDecimal("2000.00")) // Exceeds 1000
                .pin("123456")
                .build();

        when(accountRepository.findByIdForUpdate(any(UUID.class))).thenAnswer(invocation -> {
            UUID arg = invocation.getArgument(0);
            if (arg.equals(fromAccount.getId())) return Optional.of(fromAccount);
            if (arg.equals(toAccount.getId())) return Optional.of(toAccount);
            return Optional.empty();
        });

        when(passwordEncoder.matches("123456", "encodedPin")).thenReturn(true);

        assertThrows(InsufficientBalanceException.class, () -> transactionService.transfer(user, request));
    }




    @Test
    void deposit_Success() {
        DepositRequest request = DepositRequest.builder()
                .accountId(toAccount.getId())
                .amount(new BigDecimal("300.00"))
                .build();

        when(accountRepository.findByIdForUpdate(toAccount.getId())).thenReturn(Optional.of(toAccount));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TransactionResponse response = transactionService.deposit(user, request); // admin in context

        assertNotNull(response);
        assertEquals(TransactionType.DEPOSIT, response.getType());
        assertEquals(new BigDecimal("800.00"), toAccount.getBalance()); // 500 + 300

        verify(accountRepository).save(toAccount);
        verify(auditService).logAction(any(), eq(AuditAction.TRANSACTION_DEPOSIT), anyString());
        verify(applicationEventPublisher).publishEvent(any(TransactionCompletedEvent.class));
    }

    @Test
    void deposit_AccountNotActive_ThrowsException() {
        toAccount.setStatus(AccountStatus.CLOSED);
        DepositRequest request = DepositRequest.builder()
                .accountId(toAccount.getId())
                .amount(new BigDecimal("300.00"))
                .build();

        when(accountRepository.findByIdForUpdate(toAccount.getId())).thenReturn(Optional.of(toAccount));

        assertThrows(AccountNotActiveException.class, () -> transactionService.deposit(user, request));
    }



    @Test
    void withdrawal_Success() {
        WithdrawalRequest request = WithdrawalRequest.builder()
                .accountId(fromAccount.getId())
                .amount(new BigDecimal("150.00"))
                .pin("123456")
                .build();

        when(accountRepository.findByIdForUpdate(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        when(passwordEncoder.matches("123456", "encodedPin")).thenReturn(true);

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TransactionResponse response = transactionService.withdrawal(user, request);

        assertNotNull(response);
        assertEquals(TransactionType.WITHDRAWAL, response.getType());
        assertEquals(new BigDecimal("850.00"), fromAccount.getBalance()); // 1000 - 150

        verify(accountRepository).save(fromAccount);
        verify(auditService).logAction(any(), eq(AuditAction.TRANSACTION_WITHDRAWAL), anyString());
        verify(applicationEventPublisher).publishEvent(any(TransactionCompletedEvent.class));
    }

    @Test
    void withdrawal_Unauthorized_ThrowsException() {
        WithdrawalRequest request = WithdrawalRequest.builder()
                .accountId(fromAccount.getId())
                .amount(new BigDecimal("150.00"))
                .build();

        when(accountRepository.findByIdForUpdate(fromAccount.getId())).thenReturn(Optional.of(fromAccount));

        assertThrows(UnauthorizedTransactionException.class, () -> transactionService.withdrawal(otherUser, request)); // other user calling it
    }



    @Test
    void getTransactions_Success() {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .user(user)
                .amount(new BigDecimal("100"))
                .type(TransactionType.DEPOSIT)
                .build();
                
        Page<Transaction> page = new PageImpl<>(List.of(tx));

        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<TransactionResponse> responsePage = transactionService.getTransactions(
                LocalDate.now().minusDays(1), LocalDate.now(), TransactionType.DEPOSIT, null, null, Pageable.unpaged());

        assertEquals(1, responsePage.getTotalElements());
        assertEquals(TransactionType.DEPOSIT, responsePage.getContent().get(0).getType());
    }

    @Test
    void getMyTransactions_Success() {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .user(user)
                .amount(new BigDecimal("100"))
                .type(TransactionType.TRANSFER)
                .build();

        Page<Transaction> page = new PageImpl<>(List.of(tx));

        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<TransactionResponse> responsePage = transactionService.getMyTransactions(
                user, null, null, null, null, null, Pageable.unpaged());

        assertEquals(1, responsePage.getTotalElements());
        assertEquals(TransactionType.TRANSFER, responsePage.getContent().get(0).getType());
    }

    @Test
    void getTransactionById_Success() {
        UUID txId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .id(txId)
                .user(user)
                .amount(new BigDecimal("100"))
                .type(TransactionType.WITHDRAWAL)
                .build();

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

        TransactionResponse response = transactionService.getTransactionById(txId);

        assertEquals(txId, response.getId());
        assertEquals(TransactionType.WITHDRAWAL, response.getType());
    }

    @Test
    void getTransactionById_NotFound_ThrowsException() {
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findById(txId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.getTransactionById(txId));
    }
}