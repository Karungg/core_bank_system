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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
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

    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<String>> delete(@PathVariable("id") UUID id) {
        accountService.delete(id);
        String message = messageSource.getMessage("success.delete", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, "OK")
        );
    }
}
