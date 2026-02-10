package br.com.bip.infrastructure.message.kafka.delivery;

import br.com.bip.application.delivery.event.messaging.DeliveryCanceledEvent;
import br.com.bip.application.delivery.event.messaging.DeliveryCompletedEvent;
import br.com.bip.infrastructure.messaging.kafka.delivery.DeliveryLifecycleConsumer;
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
class DeliveryLifecycleConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DeliveryLifecycleConsumer consumer;

    @Test
    void onCompleted_deveDesserializarPayloadValidoSemErro() throws Exception {
        String payload = "{\"fake\":\"json\"}";

        when(objectMapper.readValue(anyString(), eq(DeliveryCompletedEvent.class)))
                .thenReturn(null);

        assertThatNoException()
                .isThrownBy(() -> consumer.onCompleted(payload));

        verify(objectMapper).readValue(payload, DeliveryCompletedEvent.class);
    }

    @Test
    void onCompleted_deveNaoExplodirQuandoDesserializacaoFalhar() throws Exception {
        String payload = "json-invalido";

        when(objectMapper.readValue(anyString(), eq(DeliveryCompletedEvent.class)))
                .thenThrow(new RuntimeException("erro de parse"));

        assertThatNoException()
                .isThrownBy(() -> consumer.onCompleted(payload));

        verify(objectMapper).readValue(payload, DeliveryCompletedEvent.class);
    }

    @Test
    void onCanceled_deveDesserializarPayloadValidoSemErro() throws Exception {
        String payload = "{\"fake\":\"json\"}";

        when(objectMapper.readValue(anyString(), eq(DeliveryCanceledEvent.class)))
                .thenReturn(null);

        assertThatNoException()
                .isThrownBy(() -> consumer.onCanceled(payload));

        verify(objectMapper).readValue(payload, DeliveryCanceledEvent.class);
    }

    @Test
    void onCanceled_deveNaoExplodirQuandoDesserializacaoFalhar() throws Exception {
        String payload = "json-invalido";

        when(objectMapper.readValue(anyString(), eq(DeliveryCanceledEvent.class)))
                .thenThrow(new RuntimeException("erro de parse"));

        assertThatNoException()
                .isThrownBy(() -> consumer.onCanceled(payload));

        verify(objectMapper).readValue(payload, DeliveryCanceledEvent.class);
    }

    @Test
    void onCompleted_deveAceitarEventoNuloSemExplodir() throws Exception {
        String payload = "{\"fake\":\"json\"}";

        when(objectMapper.readValue(anyString(), eq(DeliveryCompletedEvent.class)))
                .thenReturn(null);

        assertThatNoException()
                .isThrownBy(() -> consumer.onCompleted(payload));

        verify(objectMapper).readValue(payload, DeliveryCompletedEvent.class);
    }

    @Test
    void onCanceled_deveAceitarEventoNuloSemExplodir() throws Exception {
        String payload = "{\"fake\":\"json\"}";

        when(objectMapper.readValue(anyString(), eq(DeliveryCanceledEvent.class)))
                .thenReturn(null);

        assertThatNoException()
                .isThrownBy(() -> consumer.onCanceled(payload));

        verify(objectMapper).readValue(payload, DeliveryCanceledEvent.class);
    }
}
