package com.miftah.core_bank_system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PinMismatchException extends RuntimeException {
    public PinMismatchException(String message) {
        super(message);
    }
}
