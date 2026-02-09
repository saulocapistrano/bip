package br.com.bip.infrastructure.persistence.jpa.delivery;

import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryRequestJpaRepository extends JpaRepository<DeliveryRequest, UUID> {

    List<DeliveryRequest> findByClientId(UUID clientId);

    List<DeliveryRequest> findByStatus(DeliveryStatus status);

    List<DeliveryRequest> findByDriverId(UUID driverId);
}
