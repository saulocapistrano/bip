package br.com.bip.infrastructure.messaging.kafka.financial;

import br.com.bip.application.financial.messaging.FinancialTransactionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FinancialTransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(FinancialTransactionConsumer.class);

    private final ObjectMapper objectMapper;

    public FinancialTransactionConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "bip.financial.transaction",
            groupId = "bip-financial"
    )
    public void onMessage(String payload) {
        try {
            FinancialTransactionEvent event =
                    objectMapper.readValue(payload, FinancialTransactionEvent.class);

            handle(event);
        } catch (Exception e) {
            log.error("[KAFKA] Erro ao desserializar FinancialTransactionEvent. payload={}", payload, e);
        }
    }

    void handle(FinancialTransactionEvent event) {
        log.info("[KAFKA] FinancialTransactionEvent recebido: {}", event);
        // furuta implementação para  integrar com outros sistema de auditoria, antifraude, etc.
    }
}
