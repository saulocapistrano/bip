package br.com.bip.infrastructure.messaging.kafka.delivery;

import br.com.bip.application.delivery.event.messaging.DeliveryRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
@Component
public class DeliveryEventProducer {
    private static final Logger log = LoggerFactory.getLogger(DeliveryEventProducer.class);

    public static final String TOPIC_DELIVERY_REQUESTED = "bip.delivery.requested";

    private final KafkaTemplate<String, DeliveryRequestedEvent> kafkaTemplate;

    public DeliveryEventProducer(KafkaTemplate<String, DeliveryRequestedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendDeliveryRequested(DeliveryRequestedEvent event) {
        String key = event.deliveryId().toString();

        log.info("Enviando evento DeliveryRequested para Kafka. topic={}, key={}, deliveryId={}",
                TOPIC_DELIVERY_REQUESTED, key, event.deliveryId());

        kafkaTemplate.send(TOPIC_DELIVERY_REQUESTED, key, event);
    }

}
