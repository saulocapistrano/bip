package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.event.messaging.DeliveryCompletedEvent;
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
class DeliveryCompletionServiceTest {

    @Mock
    private DeliveryRequestRepositoryPort deliveryRepository;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private DeliveryEventProducer deliveryEventProducer;

    @Mock
    private FinancialEventProducer financialEventProducer;

    private DeliveryCompletionService deliveryCompletionService;

    @BeforeEach
    void setUp() {
        deliveryCompletionService = new DeliveryCompletionService(
                deliveryRepository,
                userRepositoryPort,
                deliveryEventProducer,
                financialEventProducer
        );
    }

    private User buildEntregador(UUID id, BigDecimal saldo) {
        User u = new User();
        u.setId(id);
        u.setName("Entregador");
        u.setEmail("entregador@teste.com");
        u.setRole(UserRole.BIP_ENTREGADOR);
        u.setStatus(UserStatus.APPROVED);
        u.setDriverBalance(saldo);
        u.setCreatedAt(OffsetDateTime.now());
        u.setUpdatedAt(OffsetDateTime.now());
        return u;
    }

    private User buildCliente(UUID id, BigDecimal saldo) {
        User u = new User();
        u.setId(id);
        u.setName("Cliente");
        u.setEmail("cliente@teste.com");
        u.setRole(UserRole.BIP_CLIENTE);
        u.setStatus(UserStatus.APPROVED);
        u.setClientBalance(saldo);
        u.setCreatedAt(OffsetDateTime.now());
        u.setUpdatedAt(OffsetDateTime.now());
        return u;
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
    void completeDelivery_deveConcluirEntregaComSucesso() {
        UUID deliveryId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        BigDecimal price = BigDecimal.valueOf(50);

        User client = buildCliente(clientId, BigDecimal.valueOf(200));
        User driver = buildEntregador(driverId, BigDecimal.ZERO);

        DeliveryRequest delivery = buildDelivery(
                deliveryId, clientId, driverId,
                DeliveryStatus.IN_ROUTE, price
        );

        when(userRepositoryPort.findById(driverId)).thenReturn(Optional.of(driver));
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(deliveryRepository.save(any(DeliveryRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = deliveryCompletionService
                .completeDelivery(deliveryId, driverId);

        assertThat(response.status()).isEqualTo(DeliveryStatus.COMPLETED);

        assertThat(client.getClientBalance()).isEqualByComparingTo(BigDecimal.valueOf(150)); // 200 - 50
        assertThat(driver.getDriverBalance()).isEqualByComparingTo(price);

        ArgumentCaptor<DeliveryCompletedEvent> deliveryCaptor =
                ArgumentCaptor.forClass(DeliveryCompletedEvent.class);
        verify(deliveryEventProducer).sendDeliveryCompleted(deliveryCaptor.capture());

        DeliveryCompletedEvent event = deliveryCaptor.getValue();
        assertThat(event.deliveryId()).isEqualTo(deliveryId);

        ArgumentCaptor<FinancialTransactionEvent> finCaptor =
                ArgumentCaptor.forClass(FinancialTransactionEvent.class);
        verify(financialEventProducer).send(finCaptor.capture());

        FinancialTransactionEvent finEvent = finCaptor.getValue();
        assertThat(finEvent.type()).isEqualTo(FinancialTransactionType.DELIVERY_PAYMENT);
        assertThat(finEvent.amount()).isEqualByComparingTo(price);
        assertThat(finEvent.relatedDeliveryId()).isEqualTo(deliveryId);
    }

    @Test
    void completeDelivery_deveLancarErroQuandoEntregadorNaoExiste() {
        UUID deliveryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        when(userRepositoryPort.findById(driverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryCompletionService
                .completeDelivery(deliveryId, driverId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Entregador não encontrado");

        verifyNoInteractions(deliveryRepository, deliveryEventProducer, financialEventProducer);
    }

    @Test
    void completeDelivery_deveLancarErroQuandoEntregaNaoEstaEmRota() {
        UUID deliveryId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        User driver = buildEntregador(driverId, BigDecimal.ZERO);
        DeliveryRequest delivery = buildDelivery(
                deliveryId, clientId, driverId,
                DeliveryStatus.AVAILABLE, BigDecimal.TEN
        );

        when(userRepositoryPort.findById(driverId)).thenReturn(Optional.of(driver));
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryCompletionService
                .completeDelivery(deliveryId, driverId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Somente entregas em rota");

        verify(deliveryRepository, never()).save(any());
        verifyNoInteractions(deliveryEventProducer, financialEventProducer);
    }

    @Test
    void completeDelivery_deveLancarErroQuandoEntregaNaoPertenceAoEntregador() {
        UUID deliveryId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID outroDriverId = UUID.randomUUID();

        User driver = buildEntregador(driverId, BigDecimal.ZERO);
        DeliveryRequest delivery = buildDelivery(
                deliveryId, clientId, outroDriverId,
                DeliveryStatus.IN_ROUTE, BigDecimal.TEN
        );

        when(userRepositoryPort.findById(driverId)).thenReturn(Optional.of(driver));
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryCompletionService
                .completeDelivery(deliveryId, driverId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Entrega não está atribuída a este entregador");

        verify(deliveryRepository, never()).save(any());
        verifyNoInteractions(deliveryEventProducer, financialEventProducer);
    }

    @Test
    void completeDelivery_deveLancarErroQuandoSaldoClienteInsuficiente() {
        UUID deliveryId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        BigDecimal price = BigDecimal.valueOf(100);

        User client = buildCliente(clientId, BigDecimal.valueOf(50)); // < price
        User driver = buildEntregador(driverId, BigDecimal.ZERO);

        DeliveryRequest delivery = buildDelivery(
                deliveryId, clientId, driverId,
                DeliveryStatus.IN_ROUTE, price
        );

        when(userRepositoryPort.findById(driverId)).thenReturn(Optional.of(driver));
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> deliveryCompletionService
                .completeDelivery(deliveryId, driverId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Saldo do cliente insuficiente");

        verify(deliveryRepository, never()).save(any());
        verifyNoInteractions(deliveryEventProducer, financialEventProducer);
    }
}
