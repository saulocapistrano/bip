package br.com.bip.infrastructure.realtime.websocket;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.domain.delivery.port.DeliveryRealtimeNotifierPort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeliveryRealtimeNotifier implements DeliveryRealtimeNotifierPort {

    private final SimpMessagingTemplate messagingTemplate;

    public DeliveryRealtimeNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void notifyNewDeliveryAvailable(DeliveryResponse response) {
        messagingTemplate.convertAndSend("/topic/deliveries/available", response);
    }

    @Override
    public void notifyUpdateToDriver(UUID driverId, DeliveryResponse response) {
        String destination = "/topic/drivers/" + driverId + "/deliveries";
        messagingTemplate.convertAndSend(destination, response);
    }

    @Override
    public void notifyUpdateDelivery(DeliveryResponse response) {
        messagingTemplate.convertAndSend("/topic/deliveries/updates", response);
    }
}
