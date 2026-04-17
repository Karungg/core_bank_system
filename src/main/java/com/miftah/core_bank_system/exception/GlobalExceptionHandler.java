package com.miftah.core_bank_system.exception;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.miftah.core_bank_system.dto.WebResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<WebResponse<Object>> constraintViolationException(ConstraintViolationException exception) {
        String message = messageSource.getMessage("error.constraint", null, LocaleContextHolder.getLocale());
        
        Map<String, List<String>> errors = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.groupingBy(
                        violation -> {
                            String path = violation.getPropertyPath().toString();
                            int lastDotIdx = path.lastIndexOf('.');
                            return lastDotIdx != -1 ? path.substring(lastDotIdx + 1) : path;
                        },
                        Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())
                ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                WebResponse.error(HttpStatus.BAD_REQUEST.value(), message, errors)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponse<Object>> methodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = messageSource.getMessage("error.validation", null, LocaleContextHolder.getLocale());
        
        Map<String, List<String>> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                WebResponse.error(HttpStatus.BAD_REQUEST.value(), message, errors)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<WebResponse<String>> unknownException(Exception exception) {
        log.error("Unhandled exception: ", exception);
        String message = messageSource.getMessage("error.internal", null, LocaleContextHolder.getLocale());
        String secureErrorFeedback = "An unexpected error occurred. Please contact support.";
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, secureErrorFeedback)
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<WebResponse<Object>> duplicateResourceException(DuplicateResourceException exception) {
        String message = messageSource.getMessage("error.validation", null, LocaleContextHolder.getLocale());
        Map<String, List<String>> errors;

        if (exception.getErrors() != null && !exception.getErrors().isEmpty()) {
            errors = exception.getErrors()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> List.of(messageSource.getMessage(entry.getValue(), null, LocaleContextHolder.getLocale()))
                    ));
        } else {
            errors = Map.of(
                    exception.getField(),
                    List.of(messageSource.getMessage(exception.getMessageKey(), null, LocaleContextHolder.getLocale()))
            );
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                WebResponse.error(HttpStatus.BAD_REQUEST.value(), message, errors));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<WebResponse<String>> badCredentialsException(BadCredentialsException exception) {
        log.warn("Bad credentials attempt: {}", exception.getMessage());
        String message = messageSource.getMessage("error.bad-credentials", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                WebResponse.error(HttpStatus.UNAUTHORIZED.value(), message, exception.getMessage())
        );
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<WebResponse<String>> resourceNotFoundException(ResourceNotFoundException exception) {
        String message = messageSource.getMessage("error.not-found", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                WebResponse.error(HttpStatus.NOT_FOUND.value(), message, exception.getMessage())
        );
    }

    @ExceptionHandler(UnauthorizedTransactionException.class)
    public ResponseEntity<WebResponse<String>> unauthorizedTransactionException(UnauthorizedTransactionException exception) {
        String message = messageSource.getMessage(exception.getMessageKey(), null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                WebResponse.error(HttpStatus.FORBIDDEN.value(), "Forbidden", message)
        );
    }

    @ExceptionHandler(SameAccountTransactionException.class)
    public ResponseEntity<WebResponse<String>> sameAccountTransactionException(SameAccountTransactionException exception) {
        String message = messageSource.getMessage(exception.getMessageKey(), null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                WebResponse.error(HttpStatus.BAD_REQUEST.value(), "Bad Request", message)
        );
    }

    @ExceptionHandler(InvalidPinException.class)
    public ResponseEntity<WebResponse<String>> invalidPinException(InvalidPinException exception) {
        String message = messageSource.getMessage(exception.getMessageKey(), null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                WebResponse.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", message)
        );
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<WebResponse<String>> insufficientBalanceException(InsufficientBalanceException exception) {
        String message = messageSource.getMessage(exception.getMessageKey(), null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                WebResponse.error(HttpStatus.BAD_REQUEST.value(), "Bad Request", message)
        );
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<WebResponse<String>> tokenRefreshException(TokenRefreshException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                WebResponse.error(HttpStatus.FORBIDDEN.value(), "Forbidden", exception.getMessage())
        );
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<WebResponse<String>> accountNotActiveException(AccountNotActiveException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                WebResponse.error(HttpStatus.BAD_REQUEST.value(), "Bad Request", exception.getMessage())
        );
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<WebResponse<String>> invalidStatusTransitionException(InvalidStatusTransitionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                WebResponse.error(HttpStatus.BAD_REQUEST.value(), "Bad Request", exception.getMessage())
        );
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<WebResponse<String>> accountLockedException(AccountLockedException exception) {
        return ResponseEntity.status(HttpStatus.LOCKED).body(
                WebResponse.error(HttpStatus.LOCKED.value(), "Locked", exception.getMessage())
        );
    }
}
