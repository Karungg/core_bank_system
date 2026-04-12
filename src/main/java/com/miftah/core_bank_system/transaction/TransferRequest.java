package com.miftah.core_bank_system.transaction;

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
public class TransferRequest {

    @NotNull(message = "{validation.transaction.amount.required}")
    @DecimalMin(value = "1", message = "{validation.transaction.amount.min}")
    private BigDecimal amount;

    private UUID fromAccountId;

    private UUID toAccountId;

    @NotBlank(message = "{validation.transaction.pin.required}")
    @Size(min = 6, max = 6, message = "{validation.transaction.pin.length}")
    private String pin;
}
