package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.event.messaging.DeliveryCanceledEvent;
import br.com.bip.application.financial.messaging.FinancialTransactionEvent;
import br.com.bip.application.financial.messaging.FinancialTransactionType;
import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.infrastructure.messaging.kafka.delivery.DeliveryEventProducer;
import br.com.bip.infrastructure.messaging.kafka.financial.FinancialEventProducer;
import br.com.bip.shared.exception.BusinessException;
import br.com.bip.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryCancellationServiceTest {

    @Mock
    private DeliveryRequestRepositoryPort deliveryRepository;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private DeliveryEventProducer deliveryEventProducer;

    @Mock
    private FinancialEventProducer financialEventProducer;

    private DeliveryCancellationService deliveryCancellationService;

    @BeforeEach
    void setUp() {
        deliveryCancellationService = new DeliveryCancellationService(
                deliveryRepository,
                userRepositoryPort,
                deliveryEventProducer,
                financialEventProducer
        );
    }

    private User buildCliente(UUID id, BigDecimal saldo) {
        User user = new User();
        user.setId(id);
        user.setName("Cliente Teste");
        user.setEmail("cliente@teste.com");
        user.setRole(UserRole.BIP_CLIENTE);
        user.setStatus(UserStatus.APPROVED);
        user.setClientBalance(saldo);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }

    private User buildEntregador(UUID id, BigDecimal saldoDriver) {
        User user = new User();
        user.setId(id);
        user.setName("Entregador Teste");
        user.setEmail("entregador@teste.com");
        user.setRole(UserRole.BIP_ENTREGADOR);
        user.setStatus(UserStatus.APPROVED);
        user.setDriverBalance(saldoDriver);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }

    private DeliveryRequest buildDelivery(UUID id, UUID clientId, UUID driverId,
                                          DeliveryStatus status, BigDecimal price) {
        return DeliveryRequest.builder()
                .id(id)
                .clientId(clientId)
                .driverId(driverId)
                .pickupAddress("Origem")
                .deliveryAddress("Destino")
                .description("Pacote")
                .weightKg(BigDecimal.ONE)
                .offeredPrice(price)
                .status(status)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void cancelByClient_deveCancelarSemMultaQuandoStatusAvailable() {
        UUID clientId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        User client = buildCliente(clientId, BigDecimal.valueOf(500));
        DeliveryRequest delivery = buildDelivery(
                deliveryId, clientId, null,
                DeliveryStatus.AVAILABLE, BigDecimal.valueOf(100)
        );

        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(DeliveryRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = deliveryCancellationService
                .cancelByClient(deliveryId, clientId, "Cliente desistiu");

        assertThat(response.status()).isEqualTo(DeliveryStatus.CANCELED);

        ArgumentCaptor<DeliveryRequest> deliveryCaptor = ArgumentCaptor.forClass(DeliveryRequest.class);
        verify(deliveryRepository).save(deliveryCaptor.capture());

        DeliveryRequest saved = deliveryCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.CANCELED);
        assertThat(saved.getCancellationReason()).contains("Cliente desistiu");

        ArgumentCaptor<DeliveryCanceledEvent> eventCaptor =
                ArgumentCaptor.forClass(DeliveryCanceledEvent.class);
        verify(deliveryEventProducer).sendDeliveryCanceled(eventCaptor.capture());

        DeliveryCanceledEvent event = eventCaptor.getValue();
        assertThat(event.deliveryId()).isEqualTo(deliveryId);
        assertThat(event.penaltyAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        verifyNoInteractions(financialEventProducer);
    }

    @Test
    void cancelByClient_deveCancelarComMultaQuandoStatusInRoute() {
        UUID clientId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        BigDecimal price = BigDecimal.valueOf(100);
        BigDecimal expectedPenalty = price.multiply(BigDecimal.valueOf(0.30));

        User client = buildCliente(clientId, BigDecimal.valueOf(500));
        User driver = buildEntregador(driverId, BigDecimal.ZERO);

        DeliveryRequest delivery = buildDelivery(
                deliveryId, clientId, driverId,
                DeliveryStatus.IN_ROUTE, price
        );

        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(userRepositoryPort.findById(driverId)).thenReturn(Optional.of(driver));
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(DeliveryRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = deliveryCancellationService
                .cancelByClient(deliveryId, clientId, "Cliente mudou de ideia");

        assertThat(response.status()).isEqualTo(DeliveryStatus.CANCELED);

        assertThat(client.getClientBalance()).isEqualByComparingTo(BigDecimal.valueOf(500).subtract(expectedPenalty));
        assertThat(driver.getDriverBalance()).isEqualByComparingTo(expectedPenalty);

        ArgumentCaptor<FinancialTransactionEvent> finCaptor =
                ArgumentCaptor.forClass(FinancialTransactionEvent.class);
        verify(financialEventProducer).send(finCaptor.capture());

        FinancialTransactionEvent finEvent = finCaptor.getValue();
        assertThat(finEvent.type()).isEqualTo(FinancialTransactionType.CANCELLATION_PENALTY);
        assertThat(finEvent.amount()).isEqualByComparingTo(expectedPenalty);
        assertThat(finEvent.relatedDeliveryId()).isEqualTo(deliveryId);

        ArgumentCaptor<DeliveryCanceledEvent> cancelCaptor =
                ArgumentCaptor.forClass(DeliveryCanceledEvent.class);
        verify(deliveryEventProducer).sendDeliveryCanceled(cancelCaptor.capture());

        DeliveryCanceledEvent cancelEvent = cancelCaptor.getValue();
        assertThat(cancelEvent.penaltyAmount()).isEqualByComparingTo(expectedPenalty);
        assertThat(cancelEvent.cancellationReason()).contains("multa de 30% aplicada");
    }

    @Test
    void cancelByClient_deveLancarErroQuandoEntregaNaoPertenceAoCliente() {
        UUID clientId = UUID.randomUUID();
        UUID outroClienteId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        User client = buildCliente(clientId, BigDecimal.valueOf(500));
        DeliveryRequest delivery = buildDelivery(
                deliveryId, outroClienteId, null,
                DeliveryStatus.AVAILABLE, BigDecimal.TEN
        );

        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryCancellationService
                .cancelByClient(deliveryId, clientId, "teste"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Entrega não pertence a este cliente");

        verify(deliveryRepository, never()).save(any());
        verifyNoInteractions(deliveryEventProducer, financialEventProducer);
    }

    @Test
    void cancelByClient_deveLancarErroQuandoStatusInvalido() {
        UUID clientId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        User client = buildCliente(clientId, BigDecimal.valueOf(500));
        DeliveryRequest delivery = buildDelivery(
                deliveryId, clientId, null,
                DeliveryStatus.COMPLETED, BigDecimal.TEN
        );

        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryCancellationService
                .cancelByClient(deliveryId, clientId, "teste"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Entrega não pode ser cancelada");

        verify(deliveryRepository, never()).save(any());
        verifyNoInteractions(deliveryEventProducer, financialEventProducer);
    }

    @Test
    void cancelByClient_deveLancarNotFoundQuandoClienteNaoExiste() {
        UUID clientId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryCancellationService
                .cancelByClient(deliveryId, clientId, "teste"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cliente não encontrado");

        verifyNoInteractions(deliveryRepository, deliveryEventProducer, financialEventProducer);
    }
}
