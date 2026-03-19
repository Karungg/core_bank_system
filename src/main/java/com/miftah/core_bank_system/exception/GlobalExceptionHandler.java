package com.miftah.core_bank_system.exception;

import com.miftah.core_bank_system.dto.WebResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<WebResponse<String>> constraintViolationException(ConstraintViolationException exception) {
        String message = messageSource.getMessage("error.constraint", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                WebResponse.error(HttpStatus.BAD_REQUEST.value(), message, exception.getMessage())
        );
    }



    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponse<String>> methodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = messageSource.getMessage("error.validation", null, LocaleContextHolder.getLocale());
        
        String errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                WebResponse.error(HttpStatus.BAD_REQUEST.value(), message, errors)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<WebResponse<String>> unknownException(Exception exception) {
        String message = messageSource.getMessage("error.internal", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, exception.getMessage())
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<WebResponse<String>> duplicateResourceException(DuplicateResourceException exception) {
        String message = messageSource.getMessage("error.validation", null, LocaleContextHolder.getLocale());
        String errorMessage;

        if (exception.getErrors() != null && !exception.getErrors().isEmpty()) {
            errorMessage = exception.getErrors()
                    .entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + ": " +
                            messageSource.getMessage(entry.getValue(), null, LocaleContextHolder.getLocale()))
                    .collect(Collectors.joining(", "));
        } else {
            errorMessage = exception.getField() + ": " +
                    messageSource.getMessage(exception.getMessageKey(), null, LocaleContextHolder.getLocale());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                WebResponse.error(HttpStatus.BAD_REQUEST.value(), message, errorMessage)
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<WebResponse<String>> badCredentialsException(BadCredentialsException exception) {
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
}
