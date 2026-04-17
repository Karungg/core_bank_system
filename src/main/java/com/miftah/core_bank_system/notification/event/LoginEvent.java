package com.miftah.core_bank_system.notification.event;

import java.util.UUID;

import lombok.Getter;

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
