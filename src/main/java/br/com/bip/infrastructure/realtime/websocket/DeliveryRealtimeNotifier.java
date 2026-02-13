package br.com.bip.infrastructure.realtime.websocket;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.domain.delivery.port.DeliveryRealtimeNotifierPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeliveryRealtimeNotifier implements DeliveryRealtimeNotifierPort {

    private static final Logger log = LoggerFactory.getLogger(DeliveryRealtimeNotifier.class);

    private final SimpMessagingTemplate messagingTemplate;

    public DeliveryRealtimeNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void notifyNewDeliveryAvailable(DeliveryResponse response) {
        log.info("[WS] broadcast new delivery available. destination={}, deliveryId={}",
                "/topic/deliveries/available",
                response.id());
        messagingTemplate.convertAndSend("/topic/deliveries/available", response);
    }

    @Override
    public void notifyUpdateToDriver(UUID driverId, DeliveryResponse response) {
        String destination = "/topic/drivers/" + driverId + "/deliveries";

        log.info("[WS] send update to driver. destination={}, deliveryId={}, driverId={}",
                destination,
                response.id(),
                driverId);

        messagingTemplate.convertAndSend(destination, response);
    }

    @Override
    public void notifyUpdateDelivery(DeliveryResponse response) {
        log.info("[WS] broadcast delivery update. destination={}, deliveryId={}, status={}",
                "/topic/deliveries/updates",
                response.id(),
                response.status());
        messagingTemplate.convertAndSend("/topic/deliveries/updates", response);
    }
}
