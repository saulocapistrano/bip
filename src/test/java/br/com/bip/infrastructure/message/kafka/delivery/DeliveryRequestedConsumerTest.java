package br.com.bip.infrastructure.message.kafka.delivery;


import br.com.bip.application.delivery.event.messaging.DeliveryRequestedEvent;
import br.com.bip.infrastructure.messaging.kafka.delivery.DeliveryRequestedConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeliveryRequestedConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DeliveryRequestedConsumer consumer;

    @Test
    void onMessage_deveDesserializarPayloadValidoSemErro() throws Exception {
        String payload = "{\"fake\":\"json\"}";

        when(objectMapper.readValue(anyString(), eq(DeliveryRequestedEvent.class)))
                .thenReturn(null);

        assertThatNoException()
                .isThrownBy(() -> consumer.onMessage(payload));

        verify(objectMapper).readValue(payload, DeliveryRequestedEvent.class);
    }

    @Test
    void onMessage_deveNaoExplodirQuandoDesserializacaoFalhar() throws Exception {
        String payload = "json-invalido";

        when(objectMapper.readValue(anyString(), eq(DeliveryRequestedEvent.class)))
                .thenThrow(new RuntimeException("erro de parse"));

        assertThatNoException()
                .isThrownBy(() -> consumer.onMessage(payload));

        verify(objectMapper).readValue(payload, DeliveryRequestedEvent.class);
    }

    @Test
    void onMessage_deveAceitarEventoNuloSemExplodir() throws Exception {
        String payload = "{\"fake\":\"json\"}";

        when(objectMapper.readValue(anyString(), eq(DeliveryRequestedEvent.class)))
                .thenReturn(null);

        assertThatNoException()
                .isThrownBy(() -> consumer.onMessage(payload));

        verify(objectMapper).readValue(payload, DeliveryRequestedEvent.class);
    }
}
