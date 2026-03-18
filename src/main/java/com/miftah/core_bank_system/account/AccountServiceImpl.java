package com.miftah.core_bank_system.account;

import com.miftah.core_bank_system.config.EncryptionUtil;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.user.UserRepository;
import com.miftah.core_bank_system.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AccountGeneratorUtil accountGeneratorUtil;

    private final EncryptionUtil encryptionUtil;
    
    @Override
    @Transactional(readOnly = true)
    public AccountResponse getById(UUID id) {
        log.info("Fetching account by ID: {}", id);
        Account account = findAccountByIdOrThrow(id);
        return toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAll(Pageable pageable) {
        log.info("Fetching all accounts with pageable: {}", pageable);
        return accountRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getByUsername(String username) {
        log.info("fetching account by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            
        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        return toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse create(AccountRequest request) {
        log.info("Creating account for user: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String accountNumber = accountGeneratorUtil.generateAccountNumber();
        String cardNumber = accountGeneratorUtil.generateCardNumber();
        String cvv = accountGeneratorUtil.generateCvv();

        do {
            accountNumber = accountGeneratorUtil.generateAccountNumber();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        do {
            cardNumber = accountGeneratorUtil.generateCardNumber();
        } while (accountRepository.existsByCardNumber(cardNumber));

        Account account = Account.builder()
                .user(user)
                .accountNumber(accountNumber)
                .balance(BigDecimal.valueOf(0))
                .pin(passwordEncoder.encode(request.getPin()))
                .cardNumber(cardNumber)
                .cvv(encryptionUtil.encrypt(cvv))
                .expiredDate(LocalDate.now())
                .type(request.getType())
                .expiredDate(LocalDate.now().plusYears(5))
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
