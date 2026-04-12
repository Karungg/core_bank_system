package com.miftah.core_bank_system.transaction;

import com.miftah.core_bank_system.account.Account;
import com.miftah.core_bank_system.account.AccountRepository;
import com.miftah.core_bank_system.account.AccountStatus;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.exception.InsufficientBalanceException;
import com.miftah.core_bank_system.exception.InvalidPinException;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import com.miftah.core_bank_system.exception.SameAccountTransactionException;
import com.miftah.core_bank_system.exception.UnauthorizedTransactionException;
import com.miftah.core_bank_system.audit.AuditService;
import com.miftah.core_bank_system.audit.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    @Transactional
    public TransactionResponse transfer(User user, TransferRequest request) {
        log.info("Creating transfer from account: {} to account: {}", request.getFromAccountId(), request.getToAccountId());

        UUID firstLockId;
        UUID secondLockId;
        if (request.getFromAccountId().compareTo(request.getToAccountId()) < 0) {
            firstLockId = request.getFromAccountId();
            secondLockId = request.getToAccountId();
        } else {
            firstLockId = request.getToAccountId();
            secondLockId = request.getFromAccountId();
        }

        Account firstLocked = accountRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", firstLockId));
        Account secondLocked = accountRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", secondLockId));

        Account fromAccount = request.getFromAccountId().equals(firstLockId) ? firstLocked : secondLocked;
        Account toAccount = request.getToAccountId().equals(firstLockId) ? firstLocked : secondLocked;

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new com.miftah.core_bank_system.exception.AccountNotActiveException("From account is " + fromAccount.getStatus());
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new com.miftah.core_bank_system.exception.AccountNotActiveException("To account is " + toAccount.getStatus());
        }

        if (!fromAccount.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized transaction attempt by user: {}", user.getUsername());
            throw new UnauthorizedTransactionException("error.transaction.unauthorized");
        }

        if (fromAccount.getUser().getId().equals(toAccount.getUser().getId())) {
            log.warn("Cannot transaction with same account");
            throw new SameAccountTransactionException("error.transaction.sameAccount");
        }

        if (fromAccount.getPinLockedUntil() != null && fromAccount.getPinLockedUntil().isAfter(java.time.Instant.now())) {
            throw new com.miftah.core_bank_system.exception.AccountLockedException("Account PIN is locked until: " + fromAccount.getPinLockedUntil());
        }

        if (!passwordEncoder.matches(request.getPin(), fromAccount.getPin())) {
            fromAccount.setFailedPinAttempts(fromAccount.getFailedPinAttempts() + 1);
            if (fromAccount.getFailedPinAttempts() >= 5) {
                fromAccount.setPinLockedUntil(java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.MINUTES));
                fromAccount.setFailedPinAttempts(0);
            }
            accountRepository.save(fromAccount);
            log.warn("Invalid PIN for account: {}", fromAccount.getAccountNumber());
            throw new InvalidPinException("error.transaction.pin.invalid");
        } else {
            fromAccount.setFailedPinAttempts(0);
            fromAccount.setPinLockedUntil(null);
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Insufficient balance for account: {}", fromAccount.getAccountNumber());
            throw new InsufficientBalanceException("error.transaction.balance.insufficient");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(request.getAmount())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .type(TransactionType.TRANSFER)
                .build();
        transactionRepository.save(transaction);

        log.info("Transfer created successfully with ID: {}", transaction.getId());
        auditService.logAction(user, AuditAction.TRANSACTION_TRANSFER, "Transfer " + request.getAmount() + " to " + toAccount.getAccountNumber());
        return toTransactionResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse deposit(User admin, DepositRequest request) {
        log.info("Admin {} depositing to account: {}", admin.getUsername(), request.getAccountId());

        Account toAccount = accountRepository.findByIdForUpdate(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getAccountId()));

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new com.miftah.core_bank_system.exception.AccountNotActiveException("Account is " + toAccount.getStatus());
        }

        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        accountRepository.save(toAccount);

        Transaction transaction = Transaction.builder()
                .user(admin)
                .amount(request.getAmount())
                .fromAccount(null)
                .toAccount(toAccount)
                .type(TransactionType.DEPOSIT)
                .build();
        transactionRepository.save(transaction);

        log.info("Deposit created successfully with ID: {}", transaction.getId());
        auditService.logAction(admin, AuditAction.TRANSACTION_DEPOSIT, "Deposit " + request.getAmount() + " to " + toAccount.getAccountNumber());
        return toTransactionResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse withdrawal(User user, WithdrawalRequest request) {
        log.info("User {} withdrawing from account: {}", user.getUsername(), request.getAccountId());

        Account fromAccount = accountRepository.findByIdForUpdate(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getAccountId()));

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new com.miftah.core_bank_system.exception.AccountNotActiveException("Account is " + fromAccount.getStatus());
        }

        if (!fromAccount.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized transaction attempt by user: {}", user.getUsername());
            throw new UnauthorizedTransactionException("error.transaction.unauthorized");
        }

        if (fromAccount.getPinLockedUntil() != null && fromAccount.getPinLockedUntil().isAfter(java.time.Instant.now())) {
            throw new com.miftah.core_bank_system.exception.AccountLockedException("Account PIN is locked until: " + fromAccount.getPinLockedUntil());
        }

        if (!passwordEncoder.matches(request.getPin(), fromAccount.getPin())) {
            fromAccount.setFailedPinAttempts(fromAccount.getFailedPinAttempts() + 1);
            if (fromAccount.getFailedPinAttempts() >= 5) {
                fromAccount.setPinLockedUntil(java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.MINUTES));
                fromAccount.setFailedPinAttempts(0);
            }
            accountRepository.save(fromAccount);
            log.warn("Invalid PIN for account: {}", fromAccount.getAccountNumber());
            throw new InvalidPinException("error.transaction.pin.invalid");
        } else {
            fromAccount.setFailedPinAttempts(0);
            fromAccount.setPinLockedUntil(null);
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Insufficient balance for account: {}", fromAccount.getAccountNumber());
            throw new InsufficientBalanceException("error.transaction.balance.insufficient");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        accountRepository.save(fromAccount);

        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(request.getAmount())
                .fromAccount(fromAccount)
                .toAccount(null)
                .type(TransactionType.WITHDRAWAL)
                .build();
        transactionRepository.save(transaction);

        log.info("Withdrawal created successfully with ID: {}", transaction.getId());
        auditService.logAction(user, AuditAction.TRANSACTION_WITHDRAWAL, "Withdrawal " + request.getAmount() + " from " + fromAccount.getAccountNumber());
        return toTransactionResponse(transaction);
    }

    @Override
    public Page<TransactionResponse> getTransactions(LocalDate startDate, LocalDate endDate, TransactionType type, BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable) {
        log.info("Fetching transactions with filters");
        Specification<Transaction> spec = buildSpecification(null, startDate, endDate, type, minAmount, maxAmount);
        return transactionRepository.findAll(spec, pageable).map(this::toTransactionResponse);
    }

    @Override
    public Page<TransactionResponse> getMyTransactions(User user, LocalDate startDate, LocalDate endDate, TransactionType type, BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable) {
        log.info("Fetching transactions for user: {}", user.getUsername());
        Specification<Transaction> spec = buildSpecification(user.getId(), startDate, endDate, type, minAmount, maxAmount);
        return transactionRepository.findAll(spec, pageable).map(this::toTransactionResponse);
    }

    @Override
    public TransactionResponse getTransactionById(UUID id) {
        log.info("Fetching transaction by id: {}", id);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        return toTransactionResponse(transaction);
    }

    private Specification<Transaction> buildSpecification(UUID userId, LocalDate startDate, LocalDate endDate, TransactionType type, BigDecimal minAmount, BigDecimal maxAmount) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (endDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUser().getId())
                .amount(transaction.getAmount())
                .fromAccountId(transaction.getFromAccount() != null ? transaction.getFromAccount().getId() : null)
                .toAccountId(transaction.getToAccount() != null ? transaction.getToAccount().getId() : null)
                .type(transaction.getType())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
