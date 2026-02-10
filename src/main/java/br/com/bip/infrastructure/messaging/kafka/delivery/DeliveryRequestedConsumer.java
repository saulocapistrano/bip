package br.com.bip.infrastructure.messaging.kafka.delivery;


import br.com.bip.application.delivery.event.messaging.DeliveryRequestedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DeliveryRequestedConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryRequestedConsumer.class);

    private final ObjectMapper objectMapper;

    public DeliveryRequestedConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


     // listener Kafka para eceber JSON como String e converter para DeliveryRequestedEvent

    @KafkaListener(
            topics = "bip.delivery.requested",
            groupId = "bip-delivery-requested"
    )
    public void onMessage(String payload) {
        try {
            DeliveryRequestedEvent event =
                    objectMapper.readValue(payload, DeliveryRequestedEvent.class);

            handle(event);
        } catch (Exception e) {
            log.error("[KAFKA] Erro ao desserializar DeliveryRequestedEvent. payload={}", payload, e);
        }
    }

    void handle(DeliveryRequestedEvent event) {
        log.info(
                "[KAFKA] DeliveryRequestedEvent recebido: {}",
                event
        );
    }
}
