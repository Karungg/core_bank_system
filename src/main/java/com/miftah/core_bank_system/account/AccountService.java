package com.miftah.core_bank_system.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AccountService {
    AccountResponse create(AccountRequest request);

    AccountResponse getById(UUID id);

    List<AccountResponse> getByUserId(UUID userId);

    AccountResponse getByIdAndUserId(UUID accountId, UUID userId);

    BalanceResponse getBalance(UUID accountId, UUID userId);

    Page<MutationResponse> getMutations(UUID accountId, UUID userId, LocalDate startDate, LocalDate endDate, MutationType mutationType, Pageable pageable);

    void changePin(com.miftah.core_bank_system.user.User user, UUID accountId, ChangePinRequest request);

    Page<AccountResponse> getAll(Pageable pageable);

    AccountResponse update(UUID id, AccountRequest request);

    AccountResponse updateStatus(UUID id, UpdateAccountStatusRequest request);

    void delete(UUID id);
}
