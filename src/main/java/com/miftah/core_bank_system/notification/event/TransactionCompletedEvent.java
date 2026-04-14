package com.miftah.core_bank_system.notification.event;

import com.miftah.core_bank_system.transaction.TransactionType;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class TransactionCompletedEvent extends BankEvent {
    private final UUID transactionId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String fromAccountNumber;
    private final String toAccountNumber;

    public TransactionCompletedEvent(UUID userId, String username, UUID transactionId, TransactionType type, BigDecimal amount, String fromAccountNumber, String toAccountNumber) {
        super(userId, username);
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
    }
}
