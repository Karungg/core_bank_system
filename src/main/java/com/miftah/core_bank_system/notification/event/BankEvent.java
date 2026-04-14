package com.miftah.core_bank_system.notification.event;

import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class BankEvent {
    private final UUID userId;
    private final String username;
    private final Instant timestamp;

    public BankEvent(UUID userId, String username) {
        this.userId = userId;
        this.username = username;
        this.timestamp = Instant.now();
    }
}
