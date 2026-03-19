package com.miftah.core_bank_system.exception;

import lombok.Getter;

@Getter
public class SameAccountTransactionException extends RuntimeException {
    
    private final String messageKey;

    public SameAccountTransactionException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }
}
