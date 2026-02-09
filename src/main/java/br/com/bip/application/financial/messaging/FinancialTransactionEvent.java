package br.com.bip.application.financial.messaging;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
public record FinancialTransactionEvent(
        UUID transactionId,
        FinancialTransactionType type,
        UUID fromUserId,
        UUID toUserId,
        BigDecimal amount,
        UUID relatedDeliveryId,
        String description,
        OffsetDateTime occurredAt
) {
}
