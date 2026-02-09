package br.com.bip.domain.delivery.repository;

import br.com.bip.domain.delivery.model.DeliveryRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryInRouteCachePort {

    void save(DeliveryRequest delivery);

    Optional<DeliveryRequest> findById(UUID deliveryId);
    List<DeliveryRequest> findByDriverId(UUID driverId);
    void deleteById(UUID deliveryId);
}
