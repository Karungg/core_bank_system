package com.miftah.core_bank_system.audit;

public enum AuditAction {
    LOGIN,
    LOGOUT,
    ACCOUNT_CREATED,
    ACCOUNT_STATUS_UPDATED,
    ACCOUNT_DELETED,
    TRANSACTION_TRANSFER,
    TRANSACTION_DEPOSIT,
    TRANSACTION_WITHDRAWAL,
    PIN_CHANGE
}
