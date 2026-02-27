package com.miftah.core_bank_system.user;

import com.miftah.core_bank_system.auth.RegisterRequest;
import com.miftah.core_bank_system.dto.WebResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    
    private final MessageSource messageSource;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Page<UserResponse>>> getAll(@PageableDefault(size = 10) Pageable pageable) {
        Page<UserResponse> responses = userService.getAll(pageable);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, responses)
        );
    }

    @GetMapping(path = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<UserResponse>> getById(@PathVariable("userId") UUID userId) {
        UserResponse response = userService.getById(userId);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }

    @PostMapping(path = "/admin", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<UserResponse>> createAdmin(@RequestBody @Valid RegisterRequest request) {
        UserResponse response = userService.createAdmin(request);
        String message = messageSource.getMessage("success.create", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                WebResponse.success(HttpStatus.CREATED.value(), message, response)
        );
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<UserResponse>> createUserWithProfile(@RequestBody @Valid CreateUserWithProfileRequest request) {
        UserResponse response = userService.createUserWithProfile(request);
        String message = messageSource.getMessage("success.create", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                WebResponse.success(HttpStatus.CREATED.value(), message, response)
        );
    }

    @PutMapping(path = "/admin/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<UserResponse>> updateAdmin(@PathVariable("userId") UUID userId, @RequestBody @Valid UpdateUserRequest request) {
        UserResponse response = userService.updateAdmin(userId, request);
        String message = messageSource.getMessage("success.update", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }

    @DeleteMapping(path = "/admin/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<String>> deleteAdmin(@PathVariable("userId") UUID userId) {
        userService.deleteAdmin(userId);
        String message = messageSource.getMessage("success.delete", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, "OK")
        );
    }
}
