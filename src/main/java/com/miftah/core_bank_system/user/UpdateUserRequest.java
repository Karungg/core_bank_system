package com.miftah.core_bank_system.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateUserRequest {

    @NotBlank(message = "{validation.username.required}")
    @Size(min = 3, max = 100, message = "{validation.username.size}")
    private String username;

    @Size(min = 8, max = 100, message = "{validation.password.size}")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "{validation.password.weak}"
    )
    private String password;
}
