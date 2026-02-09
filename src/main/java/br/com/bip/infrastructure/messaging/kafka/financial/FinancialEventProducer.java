package br.com.bip.infrastructure.messaging.kafka.financial;

import br.com.bip.application.financial.messaging.FinancialTransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FinancialEventProducer {

    private static final Logger log = LoggerFactory.getLogger(FinancialEventProducer.class);

    public static final String TOPIC_FINANCIAL_TRANSACTION = "bip.financial.transaction";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FinancialEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(FinancialTransactionEvent event) {
        String key = event.transactionId().toString();

        log.info("Enviando evento FinancialTransaction para Kafka. topic={}, key={}, type={}, amount={}",
                TOPIC_FINANCIAL_TRANSACTION, key, event.type(), event.amount());

        kafkaTemplate.send(TOPIC_FINANCIAL_TRANSACTION, key, event);
    }
}
