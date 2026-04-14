package com.miftah.core_bank_system.account;

import com.miftah.core_bank_system.transaction.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MutationResponse {
    private UUID transactionId;
    private TransactionType type;
    private MutationType mutationType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String counterpartyAccount;
    private String description;
    private Instant transactionDate;
}
