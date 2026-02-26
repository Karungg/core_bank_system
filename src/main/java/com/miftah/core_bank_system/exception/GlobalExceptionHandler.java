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
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private final MessageSource messageSource;

        public GlobalExceptionHandler(MessageSource messageSource) {
                this.messageSource = messageSource;
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<WebResponse<String>> constraintViolationException(ConstraintViolationException exception) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        WebResponse.error(
                                HttpStatus.BAD_REQUEST.value(),
                                messageSource.getMessage("error.constraint", null, LocaleContextHolder.getLocale()),
                                exception.getMessage()));
        }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<WebResponse<String>> apiException(ResponseStatusException exception) {
                
                return ResponseEntity.status(exception.getStatusCode()).body(
                        WebResponse.error(
                                exception.getStatusCode().value(),
                                messageSource.getMessage("error.api", null, LocaleContextHolder.getLocale()),
                                exception.getReason()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<WebResponse<String>> methodArgumentNotValidException(MethodArgumentNotValidException exception) {
                String errors = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        WebResponse.error(
                                HttpStatus.BAD_REQUEST.value(),
                                messageSource.getMessage("error.validation", null, LocaleContextHolder.getLocale()),
                                errors));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<WebResponse<String>> unknownException(Exception exception) {

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        WebResponse.error(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                messageSource.getMessage("error.internal", null, LocaleContextHolder.getLocale()),
                                exception.getMessage()));
        }

        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<WebResponse<String>> duplicateResourceException(DuplicateResourceException exception) {
                String errorMessage;

                if (exception.getErrors() != null && !exception.getErrors().isEmpty()) {
                        errorMessage = exception.getErrors()
                                        .entrySet()
                                        .stream()
                                        .map(entry ->
                                                entry.getKey() + ": " +
                                                messageSource.getMessage(entry.getValue(), null, LocaleContextHolder.getLocale()))
                                        .collect(Collectors.joining(", "));
                } else {
                        errorMessage = exception.getField() + ": " + 
                                messageSource.getMessage(exception.getMessageKey(), null, LocaleContextHolder.getLocale());
                }

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        WebResponse.error(
                                HttpStatus.BAD_REQUEST.value(),
                                messageSource.getMessage("error.validation", null, LocaleContextHolder.getLocale()),
                                errorMessage));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<WebResponse<String>> badCredentialsException(BadCredentialsException exception) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        WebResponse.error(
                                HttpStatus.UNAUTHORIZED.value(),
                                messageSource.getMessage("error.bad-credentials", null, LocaleContextHolder.getLocale()),
                                exception.getMessage()));
        }
}
