package com.miftah.core_bank_system.account;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateAccountStatusRequest {

    @NotNull(message = "{validation.account.status.required}")
    private AccountStatus status;

    private String reason;
}
