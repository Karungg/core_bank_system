package com.miftah.core_bank_system.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountRequest {

    @NotNull(message = "{validation.account.userId.required}")
    private UUID userId;

    @NotBlank(message = "{validation.account.pin.required}")
    @Size(min = 6, max = 6, message = "{validation.account.pin.size}")
    private String pin;

    @NotNull(message = "{validation.account.type.required}")
    private AccountType type;
}
