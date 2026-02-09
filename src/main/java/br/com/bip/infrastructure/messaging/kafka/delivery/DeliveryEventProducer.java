package br.com.bip.infrastructure.messaging.kafka.delivery;

import br.com.bip.application.delivery.event.messaging.DeliveryCanceledEvent;
import br.com.bip.application.delivery.event.messaging.DeliveryCompletedEvent;
import br.com.bip.application.delivery.event.messaging.DeliveryRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventProducer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventProducer.class);

    public static final String TOPIC_DELIVERY_REQUESTED = "bip.delivery.requested";
    public static final String TOPIC_DELIVERY_COMPLETED = "bip.delivery.completed";
    public static final String TOPIC_DELIVERY_CANCELED  = "bip.delivery.canceled";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DeliveryEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendDeliveryRequested(DeliveryRequestedEvent event) {
        String key = event.deliveryId().toString();

        log.info("Enviando evento DeliveryRequested para Kafka. topic={}, key={}, deliveryId={}",
                TOPIC_DELIVERY_REQUESTED, key, event.deliveryId());

        kafkaTemplate.send(TOPIC_DELIVERY_REQUESTED, key, event);
    }

    public void sendDeliveryCompleted(DeliveryCompletedEvent event) {
        String key = event.deliveryId().toString();

        log.info("Enviando evento DeliveryCompleted para Kafka. topic={}, key={}, deliveryId={}",
                TOPIC_DELIVERY_COMPLETED, key, event.deliveryId());

        kafkaTemplate.send(TOPIC_DELIVERY_COMPLETED, key, event);
    }

    public void sendDeliveryCanceled(DeliveryCanceledEvent event) {
        String key = event.deliveryId().toString();
        log.info("Enviando evento DeliveryCanceled para Kafka. topic={}, key={}, deliveryId={}",
                TOPIC_DELIVERY_CANCELED, key, event.deliveryId());
        kafkaTemplate.send(TOPIC_DELIVERY_CANCELED, key, event);
    }
}
