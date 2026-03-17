package com.miftah.core_bank_system.transaction;

import com.miftah.core_bank_system.account.Account;
import com.miftah.core_bank_system.account.AccountRepository;
import com.miftah.core_bank_system.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

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

        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "error.transaction.account.notFound"));

        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "error.transaction.account.notFound"));

        // Secure transaction: Check if the fromAccount belongs to the authenticated
        // user
        if (!fromAccount.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized transaction attempt by user: {}", user.getUsername());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "error.transaction.unauthorized");
        }

        if (fromAccount.getUser().getId().equals(toAccount.getUser().getId())) {
            log.warn("Cannot transaction with same account");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.transaction.sameAccount");
        }

        // Validate PIN before any financial operation
        if (!passwordEncoder.matches(request.getPin(), fromAccount.getPin())) {
            log.warn("Invalid PIN for account: {}", fromAccount.getAccountNumber());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "error.transaction.pin.invalid");
        }

        // Check balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Insufficient balance for account: {}", fromAccount.getAccountNumber());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.transaction.balance.insufficient");
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
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
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
