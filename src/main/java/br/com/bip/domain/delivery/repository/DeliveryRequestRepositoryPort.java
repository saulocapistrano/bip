package br.com.bip.domain.delivery.repository;


import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRequestRepositoryPort {

    DeliveryRequest save(DeliveryRequest deliveryRequest);

    Optional<DeliveryRequest> findById(UUID id);

    List<DeliveryRequest> findByClientId(UUID clientId);

    List<DeliveryRequest> findByStatus(DeliveryStatus status);

    List<DeliveryRequest> findByDriverId(UUID driverId);
}
