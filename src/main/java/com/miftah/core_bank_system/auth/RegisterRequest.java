package com.miftah.core_bank_system.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "{validation.username.required}")
    @Size(min = 3, max = 100, message = "{validation.username.size}")
    private String username;
    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 100, message = "{validation.password.size}")
    private String password;
}
