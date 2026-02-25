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

    @NotBlank(message = "{validation.account.accountNumber.required}")
    @Size(max = 50, message = "{validation.account.accountNumber.size}")
    private String accountNumber;

    @NotNull(message = "{validation.account.balance.required}")
    @DecimalMin(value = "0.0", message = "Balance must be non-negative")
    private BigDecimal balance;

    @NotBlank(message = "{validation.account.pin.required}")
    @Size(min = 6, max = 6, message = "{validation.account.pin.size}")
    private String pin;

    @NotBlank(message = "{validation.account.cardNumber.required}")
    @Size(max = 30, message = "{validation.account.cardNumber.size}")
    private String cardNumber;

    @NotBlank(message = "{validation.account.cvv.required}")
    @Size(min = 3, max = 3, message = "{validation.account.cvv.size}")
    private String cvv;

    @NotNull(message = "{validation.account.type.required}")
    private AccountType type;
}
