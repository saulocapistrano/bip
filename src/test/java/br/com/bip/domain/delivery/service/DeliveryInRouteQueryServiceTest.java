package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.repository.DeliveryInRouteCachePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DeliveryInRouteQueryServiceTest {

    private DeliveryInRouteCachePort inRouteCachePort;
    private DeliveryInRouteQueryService service;

    @BeforeEach
    void setUp() {
        inRouteCachePort = mock(DeliveryInRouteCachePort.class);
        service = new DeliveryInRouteQueryService(inRouteCachePort);
    }

    @Test
    void listByDriver_deveRetornarListaMapeada() {
        UUID driverId = UUID.randomUUID();
        DeliveryRequest d1 = DeliveryRequest.builder()
                .id(UUID.randomUUID())
                .clientId(UUID.randomUUID())
                .driverId(driverId)
                .pickupAddress("A")
                .deliveryAddress("B")
                .description("desc")
                .weightKg(BigDecimal.ONE)
                .offeredPrice(BigDecimal.TEN)
                .status(DeliveryStatus.IN_ROUTE)
                .createdAt(OffsetDateTime.now())
                .build();

        when(inRouteCachePort.findByDriverId(driverId)).thenReturn(List.of(d1));

        List<DeliveryResponse> responses = service.listByDriver(driverId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(d1.getId());
        assertThat(responses.get(0).driverId()).isEqualTo(driverId);

        verify(inRouteCachePort).findByDriverId(driverId);
        verifyNoMoreInteractions(inRouteCachePort);
    }
}
