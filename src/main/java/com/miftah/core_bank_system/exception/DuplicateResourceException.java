package com.miftah.core_bank_system.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class DuplicateResourceException extends RuntimeException {

    private final String field;

    private final String messageKey;

    private final Map<String, String> errors;
    
    public DuplicateResourceException(String field, String messageKey) {
        super();
        this.field = field;
        this.messageKey = messageKey;
        this.errors = null;
    }

    public DuplicateResourceException(Map<String, String> errors) {
        super();
        this.field = null;
        this.messageKey = null;
        this.errors = errors;
    }
}
