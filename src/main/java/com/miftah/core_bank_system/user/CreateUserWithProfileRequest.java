package com.miftah.core_bank_system.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.miftah.core_bank_system.auth.RegisterRequest;
import com.miftah.core_bank_system.profile.ProfileRequest;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUserWithProfileRequest {

    @Valid
    @NotNull
    private RegisterRequest user;

    @Valid
    @NotNull
    private ProfileRequest profile;
}
