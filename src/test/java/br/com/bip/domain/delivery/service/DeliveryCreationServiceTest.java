package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryCreateRequest;
import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.event.messaging.DeliveryRequestedEvent;
import br.com.bip.application.delivery.service.DeliveryCreationService;
import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.port.DeliveryRealtimeNotifierPort;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;

import br.com.bip.infrastructure.messaging.kafka.delivery.DeliveryEventProducer;
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
class DeliveryCreationServiceTest {

    @Mock
    private DeliveryRequestRepositoryPort deliveryRepository;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private DeliveryEventProducer deliveryEventProducer;

    @Mock
    private DeliveryRealtimeNotifierPort realtimeNotifier;

    private DeliveryCreationService deliveryCreationService;

    @BeforeEach
    void setUp() {
        deliveryCreationService = new DeliveryCreationService(
                deliveryRepository,
                userRepositoryPort,
                deliveryEventProducer,
                realtimeNotifier
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

    private DeliveryRequest buildDelivery(UUID id, UUID clientId, BigDecimal price) {
        return DeliveryRequest.builder()
                .id(id)
                .clientId(clientId)
                .pickupAddress("Origem")
                .deliveryAddress("Destino")
                .description("Teste")
                .weightKg(BigDecimal.ONE)
                .offeredPrice(price)
                .status(DeliveryStatus.AVAILABLE)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void createDelivery_deveCriarQuandoSaldoSuficienteEDispararEvento() {
        UUID clientId = UUID.randomUUID();
        BigDecimal preco = BigDecimal.valueOf(100);

        DeliveryCreateRequest request = new DeliveryCreateRequest(
                clientId,
                "Origem",
                "Destino",
                "Pacote",
                BigDecimal.ONE,
                preco
        );

        User cliente = buildCliente(clientId, BigDecimal.valueOf(500));

        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.of(cliente));
        when(deliveryRepository.save(any(DeliveryRequest.class)))
                .thenAnswer(inv -> {
                    DeliveryRequest d = inv.getArgument(0);
                    d.setId(UUID.randomUUID());
                    return d;
                });

        DeliveryResponse response = deliveryCreationService.create(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.clientId()).isEqualTo(clientId);
        assertThat(response.status()).isEqualTo(DeliveryStatus.AVAILABLE);

        ArgumentCaptor<DeliveryRequest> deliveryCaptor = ArgumentCaptor.forClass(DeliveryRequest.class);
        verify(deliveryRepository).save(deliveryCaptor.capture());

        DeliveryRequest saved = deliveryCaptor.getValue();
        assertThat(saved.getOfferedPrice()).isEqualTo(preco);
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.AVAILABLE);

        ArgumentCaptor<DeliveryRequestedEvent> eventCaptor = ArgumentCaptor.forClass(DeliveryRequestedEvent.class);
        verify(deliveryEventProducer).sendDeliveryRequested(eventCaptor.capture());

        DeliveryRequestedEvent event = eventCaptor.getValue();
        assertThat(event.deliveryId()).isEqualTo(saved.getId());
        assertThat(event.clientId()).isEqualTo(clientId);

        verify(realtimeNotifier).notifyNewDeliveryAvailable(response);

        verify(userRepositoryPort).findById(clientId);
        verifyNoMoreInteractions(userRepositoryPort);
    }

    @Test
    void createDelivery_deveLancarErroQuandoClienteNaoExiste() {
        UUID clientId = UUID.randomUUID();

        DeliveryCreateRequest request = new DeliveryCreateRequest(
                clientId,
                "Origem",
                "Destino",
                "Pacote",
                BigDecimal.ONE,
                BigDecimal.valueOf(100)
        );

        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryCreationService.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cliente não encontrado");

        verify(userRepositoryPort).findById(clientId);
        verifyNoInteractions(deliveryRepository, deliveryEventProducer);
        verifyNoInteractions(realtimeNotifier);
    }

    @Test
    void createDelivery_deveLancarErroQuandoSaldoInsuficiente() {
        UUID clientId = UUID.randomUUID();

        DeliveryCreateRequest request = new DeliveryCreateRequest(
                clientId,
                "Origem",
                "Destino",
                "Pacote",
                BigDecimal.ONE,
                BigDecimal.valueOf(100)
        );

        User cliente = buildCliente(clientId, BigDecimal.valueOf(150));

        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> deliveryCreationService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(userRepositoryPort).findById(clientId);
        verifyNoInteractions(deliveryRepository, deliveryEventProducer);
        verifyNoInteractions(realtimeNotifier);
    }
}