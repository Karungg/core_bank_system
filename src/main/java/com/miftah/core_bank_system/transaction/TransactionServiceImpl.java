package com.miftah.core_bank_system.transaction;

import com.miftah.core_bank_system.account.Account;
import com.miftah.core_bank_system.account.AccountRepository;
import com.miftah.core_bank_system.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.miftah.core_bank_system.exception.InsufficientBalanceException;
import com.miftah.core_bank_system.exception.InvalidPinException;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import com.miftah.core_bank_system.exception.SameAccountTransactionException;
import com.miftah.core_bank_system.exception.UnauthorizedTransactionException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public TransactionResponse createTransaction(User user, TransactionRequest request) {
        log.info("Creating transaction from account: {} to account: {}", request.getFromAccountId(),
                request.getToAccountId());

        // Lock ordering: always lock the account with the smaller UUID first to prevent deadlocks
        UUID firstLockId;
        UUID secondLockId;
        if (request.getFromAccountId().compareTo(request.getToAccountId()) < 0) {
            firstLockId = request.getFromAccountId();
            secondLockId = request.getToAccountId();
        } else {
            firstLockId = request.getToAccountId();
            secondLockId = request.getFromAccountId();
        }

        // Acquire pessimistic locks in consistent order
        Account firstLocked = accountRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", firstLockId));
        Account secondLocked = accountRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", secondLockId));

        // Assign to named variables based on role
        Account fromAccount = request.getFromAccountId().equals(firstLockId) ? firstLocked : secondLocked;
        Account toAccount = request.getToAccountId().equals(firstLockId) ? firstLocked : secondLocked;

        // Secure transaction: Check if the fromAccount belongs to the authenticated user
        if (!fromAccount.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized transaction attempt by user: {}", user.getUsername());
            throw new UnauthorizedTransactionException("error.transaction.unauthorized");
        }

        if (fromAccount.getUser().getId().equals(toAccount.getUser().getId())) {
            log.warn("Cannot transaction with same account");
            throw new SameAccountTransactionException("error.transaction.sameAccount");
        }

        // Validate PIN before any financial operation
        if (!passwordEncoder.matches(request.getPin(), fromAccount.getPin())) {
            log.warn("Invalid PIN for account: {}", fromAccount.getAccountNumber());
            throw new InvalidPinException("error.transaction.pin.invalid");
        }

        // Check balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Insufficient balance for account: {}", fromAccount.getAccountNumber());
            throw new InsufficientBalanceException("error.transaction.balance.insufficient");
        }

        // Perform transaction
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(request.getAmount())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .build();
        transactionRepository.save(transaction);

        log.info("Transaction created successfully with ID: {}", transaction.getId());
        return toTransactionResponse(transaction);
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUser().getId())
                .amount(transaction.getAmount())
                .fromAccountId(transaction.getFromAccount().getId())
                .toAccountId(transaction.getToAccount().getId())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
