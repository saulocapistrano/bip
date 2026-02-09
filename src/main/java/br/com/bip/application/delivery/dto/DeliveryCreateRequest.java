package br.com.bip.application.delivery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryCreateRequest(

        @NotNull
        UUID clientId,

        @NotBlank
        @Size(max = 255)
        String pickupAddress,

        @NotBlank
        @Size(max = 255)
        String deliveryAddress,

        @NotBlank
        @Size(max = 500)
        String description,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal weightKg,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal offeredPrice
) {
}
