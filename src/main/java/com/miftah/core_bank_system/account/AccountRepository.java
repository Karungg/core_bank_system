package com.miftah.core_bank_system.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;


@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);

    List<Account> findByUserId(UUID userId);

    List<Account> findByUserIdAndStatus(UUID userId, AccountStatus status);

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByCardNumber(String cardNumber);

    boolean existsByAccountNumberAndIdNot(String accountNumber, UUID id);

    boolean existsByCardNumberAndIdNot(String cardNumber, UUID id);
}
