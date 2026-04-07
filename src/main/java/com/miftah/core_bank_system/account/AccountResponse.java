package com.miftah.core_bank_system.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountResponse {

    private UUID id;

    private UUID userId;

    private String accountNumber;

    private BigDecimal balance;

    private String cardNumber;

    private AccountType type;

    private AccountStatus status;

    private LocalDate createdAt;

    private LocalDate updatedAt;
}
