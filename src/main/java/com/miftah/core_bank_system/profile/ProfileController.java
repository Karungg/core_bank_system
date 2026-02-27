package com.miftah.core_bank_system.profile;

import com.miftah.core_bank_system.dto.WebResponse;
import com.miftah.core_bank_system.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

        private final ProfileService profileService;
        private final MessageSource messageSource;

        @GetMapping(path = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<WebResponse<Page<ProfileResponse>>> getAll(
                        @PageableDefault(size = 10) Pageable pageable) {
                Page<ProfileResponse> responses = profileService.getAll(pageable);
                return ResponseEntity.status(HttpStatus.OK)
                                .body(WebResponse.success(HttpStatus.OK.value(),
                                                messageSource.getMessage("success.get", null,
                                                                LocaleContextHolder.getLocale()),
                                                responses));
        }

        @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<WebResponse<ProfileResponse>> getById(
                        @PathVariable("id") UUID id) {
                ProfileResponse response = profileService.getById(id);
                return ResponseEntity.status(HttpStatus.OK)
                                .body(WebResponse.success(HttpStatus.OK.value(),
                                                messageSource.getMessage("success.get", null,
                                                                LocaleContextHolder.getLocale()),
                                                response));
        }

        @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<WebResponse<ProfileResponse>> create(@AuthenticationPrincipal User user,
                        @RequestBody @Valid ProfileRequest request) {
                ProfileResponse response = profileService.create(user, request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(WebResponse.success(HttpStatus.CREATED.value(),
                                                messageSource.getMessage("success.create", null,
                                                                LocaleContextHolder.getLocale()),
                                                response));
        }

        @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<WebResponse<ProfileResponse>> get(@AuthenticationPrincipal User user) {
                ProfileResponse response = profileService.get(user);
                return ResponseEntity.status(HttpStatus.OK)
                                .body(WebResponse.success(HttpStatus.OK.value(),
                                                messageSource.getMessage("success.get", null,
                                                                LocaleContextHolder.getLocale()),
                                                response));
        }

        @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<WebResponse<ProfileResponse>> update(@AuthenticationPrincipal User user,
                        @RequestBody @Valid ProfileRequest request) {
                ProfileResponse response = profileService.update(user, request);
                return ResponseEntity.status(HttpStatus.OK)
                                .body(WebResponse.success(HttpStatus.OK.value(),
                                                messageSource.getMessage("success.update", null,
                                                                LocaleContextHolder.getLocale()),
                                                response));
        }
}
