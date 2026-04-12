package com.miftah.core_bank_system.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class DepositRequest {

    @NotNull(message = "{validation.account.userId.required}") // reusing appropriate message or adding new
    private UUID accountId;

    @NotNull(message = "{validation.transaction.amount.required}")
    @DecimalMin(value = "1", message = "{validation.transaction.amount.min}")
    private BigDecimal amount;
}
