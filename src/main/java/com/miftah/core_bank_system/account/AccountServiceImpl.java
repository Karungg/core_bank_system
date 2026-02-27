package com.miftah.core_bank_system.account;

import com.miftah.core_bank_system.exception.DuplicateResourceException;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
    
    @Override
    public AccountResponse getById(UUID id) {
        log.info("Fetching account by ID: {}", id);
        Account account = findAccountByIdOrThrow(id);
        return toResponse(account);
    }

    @Override
    public Page<AccountResponse> getAll(Pageable pageable) {
        log.info("Fetching all accounts with pageable: {}", pageable);
        return accountRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public AccountResponse create(AccountRequest request) {
        log.info("Creating account for user: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Map<String, String> errors = new HashMap<>();

        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            errors.put("accountNumber", "error.account.accountNumber.duplicate");
        }

        if (request.getCardNumber() != null && accountRepository.existsByCardNumber(request.getCardNumber())) {
            errors.put("cardNumber", "error.account.cardNumber.duplicate");
        }

        if (!errors.isEmpty()) {
            throw new DuplicateResourceException(errors);
        }

        Account account = Account.builder()
                .user(user)
                .accountNumber(request.getAccountNumber())
                .balance(request.getBalance())
                .pin(passwordEncoder.encode(request.getPin()))
                .cardNumber(request.getCardNumber())
                .cvv(request.getCvv())
                .type(request.getType())
                .build();

        account = accountRepository.save(account);
        log.info("Account created successfully with ID: {}", account.getId());

        return toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse update(UUID id, AccountRequest request) {
        log.info("Updating account ID: {}", id);

        Account account = findAccountByIdOrThrow(id);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Map<String, String> errors = new HashMap<>();

        if (accountRepository.existsByAccountNumberAndIdNot(request.getAccountNumber(), id)) {
            errors.put("accountNumber", "error.account.accountNumber.duplicate");
        }

        if (request.getCardNumber() != null && accountRepository.existsByCardNumberAndIdNot(request.getCardNumber(), id)) {
            errors.put("cardNumber", "error.account.cardNumber.duplicate");
        }

        if (!errors.isEmpty()) {
            throw new DuplicateResourceException(errors);
        }

        account.setUser(user);
        account.setAccountNumber(request.getAccountNumber());
        account.setBalance(request.getBalance());
        account.setPin(passwordEncoder.encode(request.getPin()));
        account.setCardNumber(request.getCardNumber());
        account.setCvv(request.getCvv());
        account.setType(request.getType());

        account = accountRepository.save(account);
        log.info("Account updated successfully: {}", id);

        return toResponse(account);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        log.info("Deleting account ID: {}", id);
        Account account = findAccountByIdOrThrow(id);
        accountRepository.delete(account);
        log.info("Account deleted successfully: {}", id);
    }

    private Account findAccountByIdOrThrow(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUser().getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .cardNumber(account.getCardNumber())
                .type(account.getType())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
