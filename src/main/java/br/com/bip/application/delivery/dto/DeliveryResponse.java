package br.com.bip.application.delivery.dto;

import br.com.bip.domain.delivery.model.DeliveryStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryResponse(

        UUID id,
        UUID clientId,
        UUID driverId,
        String pickupAddress,
        String deliveryAddress,
        String description,
        BigDecimal weightKg,
        BigDecimal offeredPrice,
        DeliveryStatus status,
        String cancellationReason,
        String returnReason,
        OffsetDateTime createdAt
) {
}
