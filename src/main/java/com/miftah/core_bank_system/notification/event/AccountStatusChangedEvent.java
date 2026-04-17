package com.miftah.core_bank_system.notification.event;

import java.util.UUID;

import lombok.Getter;

import com.miftah.core_bank_system.account.AccountStatus;

@Getter
public class AccountStatusChangedEvent extends BankEvent {
    private final UUID accountId;
    private final AccountStatus oldStatus;
    private final AccountStatus newStatus;

    public AccountStatusChangedEvent(UUID userId, String username, UUID accountId, AccountStatus oldStatus, AccountStatus newStatus) {
        super(userId, username);
        this.accountId = accountId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}
