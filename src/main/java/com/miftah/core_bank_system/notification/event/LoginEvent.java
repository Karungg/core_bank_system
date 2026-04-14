package com.miftah.core_bank_system.notification.event;

import lombok.Getter;

import java.util.UUID;

@Getter
public class LoginEvent extends BankEvent {
    private final String ipAddress;
    private final boolean success;

    public LoginEvent(UUID userId, String username, String ipAddress, boolean success) {
        super(userId, username);
        this.ipAddress = ipAddress;
        this.success = success;
    }
}
