package com.miftah.core_bank_system.notification.event;

import java.util.UUID;

import lombok.Getter;

@Getter
public class PinChangedEvent extends BankEvent {
    private final UUID accountId;

    public PinChangedEvent(UUID userId, String username, UUID accountId) {
        super(userId, username);
        this.accountId = accountId;
    }
}
