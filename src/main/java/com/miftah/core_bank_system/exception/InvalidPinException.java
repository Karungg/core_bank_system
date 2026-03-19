package com.miftah.core_bank_system.exception;

import lombok.Getter;

@Getter
public class InvalidPinException extends RuntimeException {
    
    private final String messageKey;

    public InvalidPinException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }
}
