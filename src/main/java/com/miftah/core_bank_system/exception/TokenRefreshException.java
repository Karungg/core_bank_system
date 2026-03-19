package com.miftah.core_bank_system.exception;



public class TokenRefreshException extends RuntimeException {

    private final String token;

    public TokenRefreshException(String token, String message) {
        super(String.format("Failed for [%s]: %s", token, message));
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
