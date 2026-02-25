package com.miftah.core_bank_system.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AccountService {
    AccountResponse create(AccountRequest request);

    AccountResponse getById(UUID id);

    Page<AccountResponse> getAll(Pageable pageable);

    AccountResponse update(UUID id, AccountRequest request);

    void delete(UUID id);
}
