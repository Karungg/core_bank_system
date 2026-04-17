package com.miftah.core_bank_system.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private TransactionType type;
    private Instant createdAt;
    private Instant updatedAt;
}
