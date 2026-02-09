package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryAssignmentServiceTest {

    @Mock
    private DeliveryRequestRepositoryPort deliveryRepository;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    private DeliveryAssignmentService deliveryAssignmentService;

    @BeforeEach
    void setUp() {
        deliveryAssignmentService = new DeliveryAssignmentService(
                deliveryRepository,
                userRepositoryPort
        );
    }

    private User buildEntregador(UUID id, UserStatus status) {
        User u = new User();
        u.setId(id);
        u.setName("Entregador Teste");
        u.setEmail("entregador@teste.com");
        u.setRole(UserRole.BIP_ENTREGADOR);
        u.setStatus(status);
        u.setCreatedAt(OffsetDateTime.now());
        u.setUpdatedAt(OffsetDateTime.now());
        return u;
    }

    private DeliveryRequest buildDelivery(UUID id, UUID clientId, DeliveryStatus status, UUID driverId) {
        return DeliveryRequest.builder()
                .id(id)
                .clientId(clientId)
                .driverId(driverId)
                .pickupAddress("Origem")
                .deliveryAddress("Destino")
                .description("Pacote")
                .weightKg(BigDecimal.ONE)
                .offeredPrice(BigDecimal.TEN)
                .status(status)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void accept_deveAtribuirEntregaQuandoDisponivel() {
        UUID driverId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        User driver = buildEntregador(driverId, UserStatus.APPROVED);
        DeliveryRequest delivery = buildDelivery(deliveryId, clientId, DeliveryStatus.AVAILABLE, null);

        when(userRepositoryPort.findById(driverId)).thenReturn(Optional.of(driver));
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(DeliveryRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeliveryResponse response = deliveryAssignmentService.acceptDelivery(deliveryId, driverId);

        assertThat(response.id()).isEqualTo(deliveryId);
        assertThat(response.driverId()).isEqualTo(driverId);
        assertThat(response.status()).isEqualTo(DeliveryStatus.IN_ROUTE);

        ArgumentCaptor<DeliveryRequest> captor = ArgumentCaptor.forClass(DeliveryRequest.class);
        verify(deliveryRepository).save(captor.capture());
        DeliveryRequest saved = captor.getValue();

        assertThat(saved.getDriverId()).isEqualTo(driverId);
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.IN_ROUTE);
    }

    @Test
    void accept_deveLancarErroQuandoEntregadorNaoExiste() {
        UUID driverId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        when(userRepositoryPort.findById(driverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryAssignmentService.acceptDelivery(deliveryId, driverId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Entregador não encontrado");

        verify(userRepositoryPort).findById(driverId);
        verifyNoInteractions(deliveryRepository);
    }

    @Test
    void accept_deveLancarErroQuandoUsuarioNaoEhEntregador() {
        UUID driverId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        User usuario = new User();
        usuario.setId(driverId);
        usuario.setRole(UserRole.BIP_CLIENTE);
        usuario.setStatus(UserStatus.APPROVED);

        when(userRepositoryPort.findById(driverId)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> deliveryAssignmentService.acceptDelivery(deliveryId, driverId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Somente usuários do tipo entregador");

        verify(userRepositoryPort).findById(driverId);
        verifyNoInteractions(deliveryRepository);
    }

    @Test
    void accept_deveLancarErroQuandoEntregaNaoDisponivel() {
        UUID driverId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        User driver = buildEntregador(driverId, UserStatus.APPROVED);
        DeliveryRequest delivery = buildDelivery(deliveryId, clientId, DeliveryStatus.IN_ROUTE, null);

        when(userRepositoryPort.findById(driverId)).thenReturn(Optional.of(driver));
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryAssignmentService.acceptDelivery(deliveryId, driverId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Somente entregas disponíveis podem ser aceitas");

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void accept_deveLancarErroQuandoEntregaJaPossuiEntregador() {
        UUID driverId = UUID.randomUUID();
        UUID outroDriverId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        User driver = buildEntregador(driverId, UserStatus.APPROVED);
        DeliveryRequest delivery = buildDelivery(deliveryId, clientId, DeliveryStatus.AVAILABLE, outroDriverId);

        when(userRepositoryPort.findById(driverId)).thenReturn(Optional.of(driver));
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryAssignmentService.acceptDelivery(deliveryId, driverId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Entrega já atribuída a um entregador");

        verify(deliveryRepository, never()).save(any());
    }
}
