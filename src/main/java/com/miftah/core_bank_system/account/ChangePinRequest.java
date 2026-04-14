package com.miftah.core_bank_system.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePinRequest {
    @NotBlank(message = "{validation.pin.old.required}")
    @Size(min = 6, max = 6, message = "{validation.pin.old.length}")
    private String oldPin;

    @NotBlank(message = "{validation.pin.new.required}")
    @Size(min = 6, max = 6, message = "{validation.pin.new.length}")
    private String newPin;

    @NotBlank(message = "{validation.pin.confirm.required}")
    @Size(min = 6, max = 6, message = "{validation.pin.confirm.length}")
    private String confirmPin;
}
