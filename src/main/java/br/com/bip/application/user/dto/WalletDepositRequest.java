package br.com.bip.application.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletDepositRequest(

        @NotNull
        UUID clientId,
        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount

) {
}
