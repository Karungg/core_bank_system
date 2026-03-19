package com.miftah.core_bank_system.exception;

import lombok.Getter;

@Getter
public class InsufficientBalanceException extends RuntimeException {
    
    private final String messageKey;

    public InsufficientBalanceException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }
}
