package br.com.bip.application.delivery.event.messaging;


import br.com.bip.domain.delivery.model.DeliveryStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryRequestedEvent(
        UUID deliveryId,
        UUID clientId,
        BigDecimal offeredPrice,
        DeliveryStatus status,
        OffsetDateTime createdAt
) {
}
