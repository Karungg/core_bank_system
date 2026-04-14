package com.miftah.core_bank_system.transaction;

import com.miftah.core_bank_system.dto.WebResponse;
import com.miftah.core_bank_system.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final MessageSource messageSource;

    @PostMapping(path = "/transfer", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid TransferRequest request) {

        TransactionResponse response = transactionService.transfer(user, request);
        String message = messageSource.getMessage("success.create", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                WebResponse.success(HttpStatus.CREATED.value(), message, response)
        );
    }

    @PostMapping(path = "/deposit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<TransactionResponse>> deposit(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid DepositRequest request) {

        TransactionResponse response = transactionService.deposit(user, request);
        String message = messageSource.getMessage("success.create", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                WebResponse.success(HttpStatus.CREATED.value(), message, response)
        );
    }

    @PostMapping(path = "/withdrawal", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<TransactionResponse>> withdrawal(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid WithdrawalRequest request) {

        TransactionResponse response = transactionService.withdrawal(user, request);
        String message = messageSource.getMessage("success.create", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                WebResponse.success(HttpStatus.CREATED.value(), message, response)
        );
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Page<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        Page<TransactionResponse> response = transactionService.getTransactions(startDate, endDate, type, minAmount, maxAmount, pageable);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }

    @GetMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Page<TransactionResponse>>> getMyTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        Page<TransactionResponse> response = transactionService.getMyTransactions(user, startDate, endDate, type, minAmount, maxAmount, pageable);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<TransactionResponse>> getTransactionById(@PathVariable("id") UUID id) {
        TransactionResponse response = transactionService.getTransactionById(id);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }
}