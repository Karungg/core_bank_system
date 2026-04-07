package com.miftah.core_bank_system.account;

import com.miftah.core_bank_system.config.EncryptionUtil;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.user.UserRepository;
import com.miftah.core_bank_system.audit.AuditService;
import com.miftah.core_bank_system.audit.AuditAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final AuditService auditService;

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
    public AccountResponse getByUsername(String username) {
        log.info("Fetching account by username: {}", username);

        User user = findUserByUsernameOrThrow(username);
        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "userId", user.getId()));

        return toResponse(account);
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
            throw new com.miftah.core_bank_system.exception.InvalidStatusTransitionException("Cannot change status of a CLOSED account");
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
            throw new com.miftah.core_bank_system.exception.InvalidStatusTransitionException("Invalid transition from " + currentStatus + " to " + newStatus);
        }

        account.setStatus(newStatus);
        account = accountRepository.save(account);
        auditService.logAction(account.getUser(), AuditAction.ACCOUNT_STATUS_UPDATED, "Account status updated to: " + newStatus);
        return toResponse(account);
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
