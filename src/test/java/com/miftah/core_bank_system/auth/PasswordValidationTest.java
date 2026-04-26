package com.miftah.core_bank_system.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordValidationTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testValidPassword() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .password("StrongPass123!")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testPasswordWithoutUppercase() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .password("strongpass123!")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("{validation.password.weak}", violations.iterator().next().getMessageTemplate());
    }

    @Test
    public void testPasswordWithoutLowercase() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .password("STRONGPASS123!")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("{validation.password.weak}", violations.iterator().next().getMessageTemplate());
    }

    @Test
    public void testPasswordWithoutDigit() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .password("StrongPass!")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("{validation.password.weak}", violations.iterator().next().getMessageTemplate());
    }

    @Test
    public void testPasswordWithoutSpecialChar() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .password("StrongPass123")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertEquals("{validation.password.weak}", violations.iterator().next().getMessageTemplate());
    }

    @Test
    public void testPasswordTooShort() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .password("S1!p")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        // Should have at least the size violation, and possibly the pattern violation if it's evaluated
        assertTrue(violations.size() >= 1);
    }
}
