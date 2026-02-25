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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final MessageSource messageSource;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<TransactionResponse>> createTransaction(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid TransactionRequest request) {

        TransactionResponse response = transactionService.createTransaction(user, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WebResponse.success(HttpStatus.CREATED.value(),
                        messageSource.getMessage("success.create", null, LocaleContextHolder.getLocale()),
                        response));
    }
}