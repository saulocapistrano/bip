package br.com.bip.domain.delivery.port;

import br.com.bip.application.delivery.dto.DeliveryResponse;

import java.util.UUID;

public interface DeliveryRealtimeNotifierPort {
    void notifyNewDeliveryAvailable(DeliveryResponse response);

    void notifyUpdateToDriver(UUID driverId, DeliveryResponse response);

    void notifyUpdateDelivery(DeliveryResponse response);
}
