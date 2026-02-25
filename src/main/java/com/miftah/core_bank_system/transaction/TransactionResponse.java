package com.miftah.core_bank_system.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {

    private UUID id;
    private UUID userId;
    private BigDecimal amount;
    private UUID fromAccountId;
    private UUID toAccountId;
    private Instant createdAt;
    private Instant updatedAt;
}
