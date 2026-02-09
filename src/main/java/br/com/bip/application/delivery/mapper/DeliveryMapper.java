package br.com.bip.application.delivery.mapper;

import br.com.bip.application.delivery.dto.DeliveryCreateRequest;
import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.event.messaging.DeliveryRequestedEvent;
import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;

import java.time.OffsetDateTime;

public final class DeliveryMapper {

    private DeliveryMapper() {
    }

    public static DeliveryRequest toNewDelivery(DeliveryCreateRequest request) {
        return DeliveryRequest.builder()
                .clientId(request.clientId())
                .pickupAddress(request.pickupAddress())
                .deliveryAddress(request.deliveryAddress())
                .description(request.description())
                .weightKg(request.weightKg())
                .offeredPrice(request.offeredPrice())
                .status(DeliveryStatus.AVAILABLE)
                .build();
    }

    public static DeliveryResponse toResponse(DeliveryRequest delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getClientId(),
                delivery.getDriverId(),
                delivery.getPickupAddress(),
                delivery.getDeliveryAddress(),
                delivery.getDescription(),
                delivery.getWeightKg(),
                delivery.getOfferedPrice(),
                delivery.getStatus(),
                delivery.getCancellationReason(),
                delivery.getReturnReason(),
                delivery.getCreatedAt()
        );
    }

    public static DeliveryRequestedEvent toRequestedEvent(DeliveryRequest delivery) {
        return new DeliveryRequestedEvent(
                delivery.getId(),
                delivery.getClientId(),
                delivery.getOfferedPrice(),
                delivery.getStatus(),
                delivery.getCreatedAt() != null ? delivery.getCreatedAt() : OffsetDateTime.now()
        );
    }
}
