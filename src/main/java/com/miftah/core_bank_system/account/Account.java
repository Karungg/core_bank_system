package com.miftah.core_bank_system.account;

import com.miftah.core_bank_system.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_number", unique = true, length = 10, nullable = false)
    private String accountNumber;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal balance;

    @Column(length = 255, nullable = false)
    private String pin;

    @Column(name = "card_number", unique = true, length = 16, nullable = false)
    private String cardNumber;

    @Column(length = 255, nullable = false)
    private String cvv;

    @Column(name = "expired_date", nullable = false)
    private LocalDate expiredDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "failed_pin_attempts", nullable = false)
    @Builder.Default
    private Integer failedPinAttempts = 0;

    @Column(name = "pin_locked_until")
    private java.time.Instant pinLockedUntil;

    @Column(name = "pin_changed_at")
    private java.time.Instant pinChangedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDate updatedAt;
}