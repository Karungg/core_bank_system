package com.miftah.core_bank_system.account;

import com.miftah.core_bank_system.dto.WebResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.miftah.core_bank_system.user.User;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    private final MessageSource messageSource;

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<AccountResponse>> getById(@PathVariable("id") UUID id) {
        AccountResponse response = accountService.getById(id);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Page<AccountResponse>>> getAll(@PageableDefault(size = 10) Pageable pageable) {
        Page<AccountResponse> responses = accountService.getAll(pageable);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, responses)
        );
    }

    @GetMapping(path = "/{id}/mutations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Page<MutationResponse>>> getMutationsAdmin(
            @PathVariable("id") UUID id,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) MutationType mutationType,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        
        Page<MutationResponse> responses = accountService.getMutations(id, null, startDate, endDate, mutationType, pageable);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, responses)
        );
    }

    @GetMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<java.util.List<AccountResponse>>> getMyAccounts(@AuthenticationPrincipal User user) {
        java.util.List<AccountResponse> response = accountService.getByUserId(user.getId());

        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
            WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }

    @GetMapping(path = "/me/{accountId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<AccountResponse>> getMyAccountById(
            @AuthenticationPrincipal User user,
            @PathVariable("accountId") UUID accountId) {
        AccountResponse response = accountService.getByIdAndUserId(accountId, user.getId());

        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
            WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }

    @GetMapping(path = "/me/{accountId}/balance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<BalanceResponse>> getMyBalance(
            @AuthenticationPrincipal User user,
            @PathVariable("accountId") UUID accountId) {
        BalanceResponse response = accountService.getBalance(accountId, user.getId());

        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
            WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }

    @GetMapping(path = "/me/{accountId}/mutations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Page<MutationResponse>>> getMyMutations(
            @AuthenticationPrincipal User user,
            @PathVariable("accountId") UUID accountId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) MutationType mutationType,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        
        Page<MutationResponse> responses = accountService.getMutations(accountId, user.getId(), startDate, endDate, mutationType, pageable);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
            WebResponse.success(HttpStatus.OK.value(), message, responses)
        );
    }

    @PutMapping(path = "/me/{accountId}/pin", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<String>> changePin(
            @AuthenticationPrincipal User user,
            @PathVariable("accountId") UUID accountId,
            @RequestBody @Valid ChangePinRequest request) {

        accountService.changePin(user, accountId, request);
        String message = messageSource.getMessage("success.update", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.OK).body(
            WebResponse.success(HttpStatus.OK.value(), message, "OK")
        );
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<AccountResponse>> create(@RequestBody @Valid AccountRequest request) {
        AccountResponse response = accountService.create(request);
        String message = messageSource.getMessage("success.create", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
                WebResponse.success(HttpStatus.CREATED.value(), message, response)
        );
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<AccountResponse>> update(@PathVariable("id") UUID id, @RequestBody @Valid AccountRequest request) {
        AccountResponse response = accountService.update(id, request);
        String message = messageSource.getMessage("success.update", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }

    @PatchMapping(path = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<AccountResponse>> updateStatus(@PathVariable("id") UUID id, @RequestBody @Valid UpdateAccountStatusRequest request) {
        AccountResponse response = accountService.updateStatus(id, request);
        String message = messageSource.getMessage("success.update", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }

    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<String>> delete(@PathVariable("id") UUID id) {
        accountService.delete(id);
        String message = messageSource.getMessage("success.delete", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, "OK")
        );
    }
}
