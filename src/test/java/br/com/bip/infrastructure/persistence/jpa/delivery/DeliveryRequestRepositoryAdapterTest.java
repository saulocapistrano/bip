package br.com.bip.infrastructure.persistence.jpa.delivery;

import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryRequestRepositoryAdapterTest {

    @Mock
    private DeliveryRequestJpaRepository jpaRepository;

    private DeliveryRequestRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DeliveryRequestRepositoryAdapter(jpaRepository);
    }

    @Test
    void save_deveDelegar() {
        DeliveryRequest request = new DeliveryRequest();
        when(jpaRepository.save(request)).thenReturn(request);

        DeliveryRequest saved = adapter.save(request);

        assertThat(saved).isSameAs(request);
        verify(jpaRepository).save(request);
        verifyNoMoreInteractions(jpaRepository);
    }

    @Test
    void findById_deveDelegar() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        Optional<DeliveryRequest> result = adapter.findById(id);

        assertThat(result).isEmpty();
        verify(jpaRepository).findById(id);
        verifyNoMoreInteractions(jpaRepository);
    }

    @Test
    void findByClientId_deveDelegar() {
        UUID clientId = UUID.randomUUID();
        when(jpaRepository.findByClientId(clientId)).thenReturn(List.of());

        List<DeliveryRequest> result = adapter.findByClientId(clientId);

        assertThat(result).isEmpty();
        verify(jpaRepository).findByClientId(clientId);
        verifyNoMoreInteractions(jpaRepository);
    }

    @Test
    void findByStatus_deveDelegar() {
        when(jpaRepository.findByStatus(DeliveryStatus.AVAILABLE)).thenReturn(List.of());

        List<DeliveryRequest> result = adapter.findByStatus(DeliveryStatus.AVAILABLE);

        assertThat(result).isEmpty();
        verify(jpaRepository).findByStatus(DeliveryStatus.AVAILABLE);
        verifyNoMoreInteractions(jpaRepository);
    }

    @Test
    void findByDriverId_deveDelegar() {
        UUID driverId = UUID.randomUUID();
        when(jpaRepository.findByDriverId(driverId)).thenReturn(List.of());

        List<DeliveryRequest> result = adapter.findByDriverId(driverId);

        assertThat(result).isEmpty();
        verify(jpaRepository).findByDriverId(driverId);
        verifyNoMoreInteractions(jpaRepository);
    }
}
