package com.miftah.core_bank_system.user;

import com.miftah.core_bank_system.auth.RegisterRequest;
import com.miftah.core_bank_system.profile.ProfileRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
