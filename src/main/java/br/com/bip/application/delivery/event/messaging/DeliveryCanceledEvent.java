package br.com.bip.application.delivery.event.messaging;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
public record DeliveryCanceledEvent (
        UUID deliveryId,
        UUID clientId,
        UUID driverId,
        BigDecimal offeredPrice,
        BigDecimal penaltyAmount,
        String cancellationReason,
        OffsetDateTime canceledAt
) {
}
