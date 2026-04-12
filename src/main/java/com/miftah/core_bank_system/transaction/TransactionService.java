package com.miftah.core_bank_system.transaction;

import com.miftah.core_bank_system.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface TransactionService {

    TransactionResponse transfer(User user, TransferRequest request);

    TransactionResponse deposit(User user, DepositRequest request);

    TransactionResponse withdrawal(User user, WithdrawalRequest request);

    Page<TransactionResponse> getTransactions(LocalDate startDate, LocalDate endDate, TransactionType type, BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);

    Page<TransactionResponse> getMyTransactions(User user, LocalDate startDate, LocalDate endDate, TransactionType type, BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);

    TransactionResponse getTransactionById(UUID id);
}
