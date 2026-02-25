package com.miftah.core_bank_system.transaction;

import com.miftah.core_bank_system.user.User;

public interface TransactionService {

    TransactionResponse createTransaction(User user, TransactionRequest request);
}
