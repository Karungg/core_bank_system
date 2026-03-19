package com.miftah.core_bank_system.exception;

import lombok.Getter;

@Getter
public class UnauthorizedTransactionException extends RuntimeException {
    
    private final String messageKey;

    public UnauthorizedTransactionException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }
}
