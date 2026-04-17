package com.miftah.core_bank_system.account;

import com.miftah.core_bank_system.audit.AuditAction;
import com.miftah.core_bank_system.audit.AuditService;
import com.miftah.core_bank_system.config.EncryptionUtil;
import com.miftah.core_bank_system.exception.AccountLockedException;
import com.miftah.core_bank_system.exception.AccountNotActiveException;
import com.miftah.core_bank_system.exception.InvalidPinException;
import com.miftah.core_bank_system.exception.InvalidStatusTransitionException;
import com.miftah.core_bank_system.exception.PinMismatchException;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import com.miftah.core_bank_system.exception.SamePinException;
import com.miftah.core_bank_system.notification.event.AccountStatusChangedEvent;
import com.miftah.core_bank_system.notification.event.PinChangedEvent;
import com.miftah.core_bank_system.transaction.Transaction;
import com.miftah.core_bank_system.transaction.TransactionRepository;
import com.miftah.core_bank_system.transaction.TransactionType;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    private final TransactionRepository transactionRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AccountGeneratorUtil accountGeneratorUtil;

    private final EncryptionUtil encryptionUtil;

    private final AuditService auditService;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public AccountResponse getById(UUID id) {
        log.info("Fetching account by ID: {}", id);
        return toResponse(findAccountByIdOrThrow(id));
    }

    @Override
    public Page<AccountResponse> getAll(Pageable pageable) {
        log.info("Fetching all accounts with pageable: {}", pageable);
        return accountRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    public List<AccountResponse> getByUserId(UUID userId) {
        log.info("Fetching accounts by user ID: {}", userId);
        return accountRepository.findByUserIdAndStatus(userId, AccountStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AccountResponse getByIdAndUserId(UUID accountId, UUID userId) {
        log.info("Fetching account by ID: {} and user ID: {}", accountId, userId);
        return toResponse(accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId)));
    }

    @Override
    public BalanceResponse getBalance(UUID accountId, UUID userId) {
        log.info("Checking balance for account ID: {}", accountId);

        Account account;
        if (userId != null) {
            account = accountRepository.findByIdAndUserId(accountId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        } else {
            account = findAccountByIdOrThrow(accountId);
        }

        return BalanceResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .type(account.getType())
                .status(account.getStatus())
                .checkedAt(Instant.now())
                .build();
    }

    @Override
    public Page<MutationResponse> getMutations(UUID accountId, UUID userId, LocalDate startDate, LocalDate endDate, MutationType mutationType, Pageable pageable) {
        log.info("Fetching mutations for account ID: {}", accountId);

        Account account;
        if (userId != null) {
            account = accountRepository.findByIdAndUserId(accountId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        } else {
            account = findAccountByIdOrThrow(accountId);
        }

        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (mutationType == MutationType.DEBIT) {
                predicates.add(cb.equal(root.get("fromAccount").get("id"), account.getId()));
            } else if (mutationType == MutationType.CREDIT) {
                predicates.add(cb.equal(root.get("toAccount").get("id"), account.getId()));
            } else {
                predicates.add(cb.or(
                        cb.equal(root.get("fromAccount").get("id"), account.getId()),
                        cb.equal(root.get("toAccount").get("id"), account.getId())
                ));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable).map(transaction -> toMutationResponse(transaction, account.getId()));
    }

    @Override
    @Transactional
    public AccountResponse create(AccountRequest request) {
        log.info("Creating account for user: {}", request.getUserId());

        User user = findUserByIdOrThrow(request.getUserId());

        String accountNumber;
        do {
            accountNumber = accountGeneratorUtil.generateAccountNumber();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        String cardNumber;
        do {
            cardNumber = accountGeneratorUtil.generateCardNumber();
        } while (accountRepository.existsByCardNumber(cardNumber));

        String cvv = accountGeneratorUtil.generateCvv();

        Account account = Account.builder()
                .user(user)
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .pin(passwordEncoder.encode(request.getPin()))
                .cardNumber(cardNumber)
                .cvv(encryptionUtil.encrypt(cvv))
                .type(request.getType())
                .status(AccountStatus.ACTIVE)
                .expiredDate(LocalDate.now().plusYears(5))
                .build();

        account = accountRepository.save(account);
        log.info("Account created successfully with ID: {}", account.getId());
        auditService.logAction(user, AuditAction.ACCOUNT_CREATED, "Account created with number: " + cardNumber);

        return toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse update(UUID id, AccountRequest request) {
        log.info("Updating account ID: {}", id);

        Account account = findAccountByIdOrThrow(id);
        User user = findUserByIdOrThrow(request.getUserId());

        account.setUser(user);
        account.setPin(passwordEncoder.encode(request.getPin()));
        account.setType(request.getType());

        account = accountRepository.save(account);
        log.info("Account updated successfully: {}", id);

        return toResponse(account);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        log.info("Soft deleting account ID: {}", id);
        Account account = findAccountByIdOrThrow(id);
        
        if (account.getStatus() == AccountStatus.CLOSED) {
            log.warn("Account {} is already closed", id);
            return;
        }
        
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
        log.info("Account soft deleted successfully: {}", id);
        auditService.logAction(account.getUser(), AuditAction.ACCOUNT_DELETED, "Account soft deleted");
    }

    @Override
    @Transactional
    public AccountResponse updateStatus(UUID id, UpdateAccountStatusRequest request) {
        log.info("Updating status for account ID: {} to {}", id, request.getStatus());
        Account account = findAccountByIdOrThrow(id);

        AccountStatus currentStatus = account.getStatus();
        AccountStatus newStatus = request.getStatus();

        if (currentStatus == AccountStatus.CLOSED) {
            throw new InvalidStatusTransitionException("Cannot change status of a CLOSED account");
        }

        boolean validTransition = false;
        switch (currentStatus) {
            case ACTIVE:
                if (newStatus == AccountStatus.FROZEN || newStatus == AccountStatus.SUSPENDED || newStatus == AccountStatus.CLOSED) {
                    validTransition = true;
                }
                break;
            case FROZEN:
            case SUSPENDED:
                if (newStatus == AccountStatus.ACTIVE || newStatus == AccountStatus.CLOSED) {
                    validTransition = true;
                }
                break;
            default:
                break;
        }

        if (!validTransition && currentStatus != newStatus) {
            throw new InvalidStatusTransitionException("Invalid transition from " + currentStatus + " to " + newStatus);
        }

        account.setStatus(newStatus);
        account = accountRepository.save(account);
        auditService.logAction(account.getUser(), AuditAction.ACCOUNT_STATUS_UPDATED, "Account status updated to: " + newStatus);
        
        applicationEventPublisher.publishEvent(new AccountStatusChangedEvent(
                account.getUser().getId(), account.getUser().getUsername(), account.getId(), currentStatus, newStatus));

        return toResponse(account);
    }

    @Override
    @Transactional
    public void changePin(User user, UUID accountId, ChangePinRequest request) {
        log.info("Changing PIN for account: {}", accountId);

        Account account = accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account is " + account.getStatus());
        }

        if (account.getPinLockedUntil() != null && account.getPinLockedUntil().isAfter(Instant.now())) {
            throw new AccountLockedException("Account PIN is locked until: " + account.getPinLockedUntil());
        }

        if (account.getPinChangedAt() != null && account.getPinChangedAt().isAfter(Instant.now().minus(24, ChronoUnit.HOURS))) {
            throw new RuntimeException("error.pin.cooldown");
        }

        if (!passwordEncoder.matches(request.getOldPin(), account.getPin())) {
            account.setFailedPinAttempts(account.getFailedPinAttempts() + 1);
            if (account.getFailedPinAttempts() >= 5) {
                account.setPinLockedUntil(Instant.now().plus(30, ChronoUnit.MINUTES));
                account.setFailedPinAttempts(0);
            }
            accountRepository.save(account);
            throw new InvalidPinException("error.transaction.pin.invalid");
        }

        if (request.getOldPin().equals(request.getNewPin())) {
            throw new SamePinException("error.pin.same");
        }

        if (!request.getNewPin().equals(request.getConfirmPin())) {
            throw new PinMismatchException("error.pin.mismatch");
        }

        account.setFailedPinAttempts(0);
        account.setPinLockedUntil(null);
        account.setPin(passwordEncoder.encode(request.getNewPin()));
        account.setPinChangedAt(Instant.now());

        accountRepository.save(account);

        auditService.logAction(user, AuditAction.PIN_CHANGE, "PIN changed successfully");

        applicationEventPublisher.publishEvent(new PinChangedEvent(user.getId(), user.getUsername(), account.getId()));
    }

    // ========== Private Helpers ==========

    private Account findAccountByIdOrThrow(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }

    private User findUserByIdOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private User findUserByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    private MutationResponse toMutationResponse(Transaction transaction, UUID accountId) {
        MutationType mType;
        String description;
        String counterpartyAccount = null;

        if (transaction.getFromAccount() != null && transaction.getFromAccount().getId().equals(accountId)) {
            mType = MutationType.DEBIT;
            if (transaction.getType() == TransactionType.TRANSFER) {
                String fullAcc = transaction.getToAccount().getAccountNumber();
                counterpartyAccount = "****" + fullAcc.substring(Math.max(0, fullAcc.length() - 4));
                description = "Transfer ke " + counterpartyAccount;
            } else {
                description = "Withdrawal";
            }
        } else {
            mType = MutationType.CREDIT;
            if (transaction.getType() == TransactionType.TRANSFER) {
                String fullAcc = transaction.getFromAccount().getAccountNumber();
                counterpartyAccount = "****" + fullAcc.substring(Math.max(0, fullAcc.length() - 4));
                description = "Transfer dari " + counterpartyAccount;
            } else {
                description = "Deposit";
            }
        }

        return MutationResponse.builder()
                .transactionId(transaction.getId())
                .type(transaction.getType())
                .mutationType(mType)
                .amount(transaction.getAmount())
                .counterpartyAccount(counterpartyAccount)
                .description(description)
                .transactionDate(transaction.getCreatedAt())
                .build();
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUser().getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .cardNumber(account.getCardNumber())
                .type(account.getType())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
