package br.com.bip.infrastructure.messaging.kafka.delivery;

import br.com.bip.application.delivery.event.messaging.DeliveryCanceledEvent;
import br.com.bip.application.delivery.event.messaging.DeliveryCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DeliveryLifecycleConsumer {
// não acessa campos específicos – só logar o próprio event
    private static final Logger log = LoggerFactory.getLogger(DeliveryLifecycleConsumer.class);

    private final ObjectMapper objectMapper;

    public DeliveryLifecycleConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "bip.delivery.completed",
            groupId = "bip-delivery-lifecycle"
    )
    public void onCompleted(String payload) {
        try {
            DeliveryCompletedEvent event =
                    objectMapper.readValue(payload, DeliveryCompletedEvent.class);

            handleCompleted(event);
        } catch (Exception e) {
            log.error("[KAFKA] Erro ao desserializar DeliveryCompletedEvent. payload={}", payload, e);
        }
    }

    @KafkaListener(
            topics = "bip.delivery.canceled",
            groupId = "bip-delivery-lifecycle"
    )
    public void onCanceled(String payload) {
        try {
            DeliveryCanceledEvent event =
                    objectMapper.readValue(payload, DeliveryCanceledEvent.class);

            handleCanceled(event);
        } catch (Exception e) {
            log.error("[KAFKA] Erro ao desserializar DeliveryCanceledEvent. payload={}", payload, e);
        }
    }

    void handleCompleted(DeliveryCompletedEvent event) {
        log.info("[KAFKA] DeliveryCompletedEvent recebido: {}", event);
        // Ex: atualizar métrica, log de auditoria, etc.
    }

    void handleCanceled(DeliveryCanceledEvent event) {
        log.info("[KAFKA] DeliveryCanceledEvent recebido: {}", event);
        // Ex: atualizar métrica, log de auditoria, etc.
    }
}
