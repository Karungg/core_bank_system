package com.miftah.core_bank_system.account;

import com.miftah.core_bank_system.config.EncryptionUtil;
import com.miftah.core_bank_system.exception.*;
import com.miftah.core_bank_system.notification.event.AccountStatusChangedEvent;
import com.miftah.core_bank_system.notification.event.PinChangedEvent;
import com.miftah.core_bank_system.transaction.Transaction;
import com.miftah.core_bank_system.transaction.TransactionRepository;
import com.miftah.core_bank_system.transaction.TransactionType;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.user.UserRepository;
import com.miftah.core_bank_system.audit.AuditService;
import com.miftah.core_bank_system.audit.AuditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountGeneratorUtil accountGeneratorUtil;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private AuditService auditService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

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
                .pin("123456")
                .type(AccountType.BLACK)
                .build();

        account = Account.builder()
                .id(UUID.randomUUID())
                .user(user)
                .accountNumber("1234567890")
                .balance(new BigDecimal("1000.00"))
                .pin("encoded_pin")
                .cardNumber("1234-5678-9012-3456")
                .cvv("encrypted_cvv")
                .type(accountRequest.getType())
                .status(AccountStatus.ACTIVE)
                .failedPinAttempts(0)
                .build();
    }

    // ==========================================
    // create
    // ==========================================

    @Test
    void create_Success() {
        when(userRepository.findById(accountRequest.getUserId())).thenReturn(Optional.of(user));
        when(accountGeneratorUtil.generateAccountNumber()).thenReturn("1234567890");
        when(accountGeneratorUtil.generateCardNumber()).thenReturn("1234-5678-9012-3456");
        when(accountGeneratorUtil.generateCvv()).thenReturn("123");
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.existsByCardNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pin");
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted_cvv");
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.create(accountRequest);

        assertNotNull(response);
        assertEquals(account.getId(), response.getId());
        assertEquals("1234567890", response.getAccountNumber());
        verify(accountRepository).save(any(Account.class));
        verify(auditService).logAction(eq(user), eq(AuditAction.ACCOUNT_CREATED), anyString());
    }

    @Test
    void create_UserNotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findById(accountRequest.getUserId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.create(accountRequest));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void create_DuplicateAccountNumber_ShouldRetry() {
        when(userRepository.findById(accountRequest.getUserId())).thenReturn(Optional.of(user));
        when(accountGeneratorUtil.generateAccountNumber()).thenReturn("1111111111", "1234567890");
        when(accountGeneratorUtil.generateCardNumber()).thenReturn("1234-5678-9012-3456");
        when(accountGeneratorUtil.generateCvv()).thenReturn("123");
        lenient().when(accountRepository.existsByAccountNumber("1111111111")).thenReturn(true);
        when(accountRepository.existsByCardNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pin");
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted_cvv");
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.create(accountRequest);

        assertNotNull(response);
        verify(accountGeneratorUtil, times(2)).generateAccountNumber();
        verify(accountRepository).save(any(Account.class));
    }

    // ==========================================
    // getById
    // ==========================================

    @Test
    void getById_Success() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getById(account.getId());

        assertNotNull(response);
        assertEquals(account.getId(), response.getId());
        assertEquals(account.getAccountNumber(), response.getAccountNumber());
        verify(accountRepository).findById(account.getId());
    }

    @Test
    void getById_NotFound_ShouldThrowResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getById(id));
    }

    // ==========================================
    // getAll
    // ==========================================

    @Test
    void getAll_Success() {
        Page<Account> page = new PageImpl<>(Collections.singletonList(account));
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<AccountResponse> response = accountService.getAll(Pageable.unpaged());

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(account.getId(), response.getContent().get(0).getId());
    }

    // ==========================================
    // getByUserId
    // ==========================================

    @Test
    void getByUserId_Success_ShouldReturnActiveAccounts() {
        when(accountRepository.findByUserIdAndStatus(user.getId(), AccountStatus.ACTIVE))
                .thenReturn(List.of(account));

        List<AccountResponse> responses = accountService.getByUserId(user.getId());

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(account.getId(), responses.get(0).getId());
        verify(accountRepository).findByUserIdAndStatus(user.getId(), AccountStatus.ACTIVE);
    }

    @Test
    void getByUserId_NoAccounts_ShouldReturnEmptyList() {
        when(accountRepository.findByUserIdAndStatus(user.getId(), AccountStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        List<AccountResponse> responses = accountService.getByUserId(user.getId());

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    // ==========================================
    // getByIdAndUserId
    // ==========================================

    @Test
    void getByIdAndUserId_Success() {
        when(accountRepository.findByIdAndUserId(account.getId(), user.getId()))
                .thenReturn(Optional.of(account));

        AccountResponse response = accountService.getByIdAndUserId(account.getId(), user.getId());

        assertNotNull(response);
        assertEquals(account.getId(), response.getId());
        verify(accountRepository).findByIdAndUserId(account.getId(), user.getId());
    }

    @Test
    void getByIdAndUserId_NotFound_ShouldThrowResourceNotFoundException() {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.getByIdAndUserId(accountId, userId));
    }

    // ==========================================
    // getBalance
    // ==========================================

    @Test
    void getBalance_WithUserId_ShouldReturnBalance() {
        when(accountRepository.findByIdAndUserId(account.getId(), user.getId()))
                .thenReturn(Optional.of(account));

        BalanceResponse response = accountService.getBalance(account.getId(), user.getId());

        assertNotNull(response);
        assertEquals(account.getId(), response.getAccountId());
        assertEquals(account.getBalance(), response.getBalance());
        assertEquals(account.getAccountNumber(), response.getAccountNumber());
        verify(accountRepository).findByIdAndUserId(account.getId(), user.getId());
    }

    @Test
    void getBalance_WithNullUserId_ShouldFindByIdOnly() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        BalanceResponse response = accountService.getBalance(account.getId(), null);

        assertNotNull(response);
        assertEquals(account.getId(), response.getAccountId());
        verify(accountRepository).findById(account.getId());
        verify(accountRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void getBalance_NotFound_ShouldThrowResourceNotFoundException() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdAndUserId(eq(accountId), any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.getBalance(accountId, user.getId()));
    }

    // ==========================================
    // getMutations
    // ==========================================

    @Test
    @SuppressWarnings("unchecked")
    void getMutations_Success_ShouldReturnMutationPage() {
        Account toAccount = Account.builder()
                .id(UUID.randomUUID()).user(user).accountNumber("0987654321").build();
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .fromAccount(account).toAccount(toAccount)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.TRANSFER)
                .createdAt(Instant.now())
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        when(accountRepository.findByIdAndUserId(account.getId(), user.getId()))
                .thenReturn(Optional.of(account));
        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        Page<MutationResponse> result = accountService.getMutations(
                account.getId(), user.getId(), null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(tx.getId(), result.getContent().get(0).getTransactionId());
    }

    // ==========================================
    // update
    // ==========================================

    @Test
    void update_Success() {
        AccountRequest updateRequest = AccountRequest.builder()
                .userId(user.getId())
                .pin("654321")
                .type(AccountType.GOLD)
                .build();

        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(userRepository.findById(updateRequest.getUserId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("new_encoded_pin");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.update(account.getId(), updateRequest);

        assertNotNull(response);
        assertEquals(account.getAccountNumber(), response.getAccountNumber());
        assertEquals(AccountType.GOLD, response.getType());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void update_AccountNotFound_ShouldThrowResourceNotFoundException() {
        UUID fakeId = UUID.randomUUID();
        when(accountRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.update(fakeId, accountRequest));
    }

    // ==========================================
    // delete
    // ==========================================

    @Test
    void delete_ActiveAccount_ShouldSoftDelete() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        accountService.delete(account.getId());

        assertEquals(AccountStatus.CLOSED, account.getStatus());
        verify(accountRepository).save(account);
        verify(auditService).logAction(eq(user), eq(AuditAction.ACCOUNT_DELETED), anyString());
    }

    @Test
    void delete_AlreadyClosedAccount_ShouldDoNothing() {
        account.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        accountService.delete(account.getId());

        verify(accountRepository, never()).save(any());
        verify(auditService, never()).logAction(any(), any(), any());
    }

    @Test
    void delete_NotFound_ShouldThrowResourceNotFoundException() {
        UUID fakeId = UUID.randomUUID();
        when(accountRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.delete(fakeId));
    }

    // ==========================================
    // updateStatus
    // ==========================================

    @Test
    void updateStatus_ActiveToFrozen_ShouldSucceedAndPublishEvent() {
        UpdateAccountStatusRequest request = UpdateAccountStatusRequest.builder()
                .status(AccountStatus.FROZEN).build();

        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.updateStatus(account.getId(), request);

        assertNotNull(response);
        assertEquals(AccountStatus.FROZEN, account.getStatus());
        verify(accountRepository).save(account);
        verify(auditService).logAction(eq(user), eq(AuditAction.ACCOUNT_STATUS_UPDATED), anyString());
        verify(applicationEventPublisher).publishEvent(any(AccountStatusChangedEvent.class));
    }

    @Test
    void updateStatus_ActiveToSuspended_ShouldSucceed() {
        UpdateAccountStatusRequest request = UpdateAccountStatusRequest.builder()
                .status(AccountStatus.SUSPENDED).build();

        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.updateStatus(account.getId(), request);

        assertNotNull(response);
        assertEquals(AccountStatus.SUSPENDED, account.getStatus());
    }

    @Test
    void updateStatus_FrozenToActive_ShouldSucceed() {
        account.setStatus(AccountStatus.FROZEN);
        UpdateAccountStatusRequest request = UpdateAccountStatusRequest.builder()
                .status(AccountStatus.ACTIVE).build();

        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.updateStatus(account.getId(), request);

        assertNotNull(response);
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }

    @Test
    void updateStatus_ClosedAccount_ShouldThrowInvalidStatusTransitionException() {
        account.setStatus(AccountStatus.CLOSED);
        UpdateAccountStatusRequest request = UpdateAccountStatusRequest.builder()
                .status(AccountStatus.ACTIVE).build();

        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThrows(InvalidStatusTransitionException.class,
                () -> accountService.updateStatus(account.getId(), request));
    }

    @Test
    void updateStatus_FrozenToSuspended_ShouldThrowInvalidStatusTransitionException() {
        account.setStatus(AccountStatus.FROZEN);
        UpdateAccountStatusRequest request = UpdateAccountStatusRequest.builder()
                .status(AccountStatus.SUSPENDED).build();

        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThrows(InvalidStatusTransitionException.class,
                () -> accountService.updateStatus(account.getId(), request));
    }

    // ==========================================
    // changePin
    // ==========================================

    @Test
    void changePin_Success_ShouldUpdatePinAndPublishEvent() {
        ChangePinRequest request = ChangePinRequest.builder()
                .oldPin("123456").newPin("654321").confirmPin("654321").build();

        when(accountRepository.findByIdAndUserId(account.getId(), user.getId()))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("123456", "encoded_pin")).thenReturn(true);
        when(passwordEncoder.encode("654321")).thenReturn("new_encoded_pin");

        accountService.changePin(user, account.getId(), request);

        assertEquals("new_encoded_pin", account.getPin());
        assertEquals(0, account.getFailedPinAttempts());
        assertNull(account.getPinLockedUntil());
        assertNotNull(account.getPinChangedAt());
        verify(accountRepository).save(account);
        verify(auditService).logAction(eq(user), eq(AuditAction.PIN_CHANGE), anyString());
        verify(applicationEventPublisher).publishEvent(any(PinChangedEvent.class));
    }

    @Test
    void changePin_AccountNotFound_ShouldThrowResourceNotFoundException() {
        UUID fakeAccountId = UUID.randomUUID();
        ChangePinRequest request = ChangePinRequest.builder()
                .oldPin("123456").newPin("654321").confirmPin("654321").build();

        when(accountRepository.findByIdAndUserId(fakeAccountId, user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.changePin(user, fakeAccountId, request));
    }

    @Test
    void changePin_AccountNotActive_ShouldThrowAccountNotActiveException() {
        account.setStatus(AccountStatus.FROZEN);
        ChangePinRequest request = ChangePinRequest.builder()
                .oldPin("123456").newPin("654321").confirmPin("654321").build();

        when(accountRepository.findByIdAndUserId(account.getId(), user.getId()))
                .thenReturn(Optional.of(account));

        assertThrows(AccountNotActiveException.class,
                () -> accountService.changePin(user, account.getId(), request));
    }

    @Test
    void changePin_AccountLocked_ShouldThrowAccountLockedException() {
        account.setPinLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        ChangePinRequest request = ChangePinRequest.builder()
                .oldPin("123456").newPin("654321").confirmPin("654321").build();

        when(accountRepository.findByIdAndUserId(account.getId(), user.getId()))
                .thenReturn(Optional.of(account));

        assertThrows(AccountLockedException.class,
                () -> accountService.changePin(user, account.getId(), request));
    }

    @Test
    void changePin_CooldownPeriod_ShouldThrowRuntimeException() {
        account.setPinChangedAt(Instant.now().minus(10, ChronoUnit.HOURS)); // less than 24h
        ChangePinRequest request = ChangePinRequest.builder()
                .oldPin("123456").newPin("654321").confirmPin("654321").build();

        when(accountRepository.findByIdAndUserId(account.getId(), user.getId()))
                .thenReturn(Optional.of(account));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.changePin(user, account.getId(), request));
        assertEquals("error.pin.cooldown", ex.getMessage());
    }

    @Test
    void changePin_WrongOldPin_ShouldIncrementFailedAttemptsAndThrow() {
        ChangePinRequest request = ChangePinRequest.builder()
                .oldPin("wrongpin").newPin("654321").confirmPin("654321").build();

        when(accountRepository.findByIdAndUserId(account.getId(), user.getId()))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrongpin", "encoded_pin")).thenReturn(false);

        assertThrows(InvalidPinException.class,
                () -> accountService.changePin(user, account.getId(), request));

        assertEquals(1, account.getFailedPinAttempts());
        verify(accountRepository).save(account);
    }

    @Test
    void changePin_TooManyFailedAttempts_ShouldLockAccount() {
        account.setFailedPinAttempts(4); // next failure will be the 5th
        ChangePinRequest request = ChangePinRequest.builder()
                .oldPin("wrongpin").newPin("654321").confirmPin("654321").build();

        when(accountRepository.findByIdAndUserId(account.getId(), user.getId()))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrongpin", "encoded_pin")).thenReturn(false);

        assertThrows(InvalidPinException.class,
                () -> accountService.changePin(user, account.getId(), request));

        assertNotNull(account.getPinLockedUntil());
        assertEquals(0, account.getFailedPinAttempts()); // reset on lock
        verify(accountRepository).save(account);
    }

    @Test
    void changePin_SameAsOldPin_ShouldThrowSamePinException() {
        ChangePinRequest request = ChangePinRequest.builder()
                .oldPin("123456").newPin("123456").confirmPin("123456").build();

        when(accountRepository.findByIdAndUserId(account.getId(), user.getId()))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("123456", "encoded_pin")).thenReturn(true);

        assertThrows(SamePinException.class,
                () -> accountService.changePin(user, account.getId(), request));
    }

    @Test
    void changePin_NewPinMismatch_ShouldThrowPinMismatchException() {
        ChangePinRequest request = ChangePinRequest.builder()
                .oldPin("123456").newPin("654321").confirmPin("999999").build();

        when(accountRepository.findByIdAndUserId(account.getId(), user.getId()))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("123456", "encoded_pin")).thenReturn(true);

        assertThrows(PinMismatchException.class,
                () -> accountService.changePin(user, account.getId(), request));
    }
}
