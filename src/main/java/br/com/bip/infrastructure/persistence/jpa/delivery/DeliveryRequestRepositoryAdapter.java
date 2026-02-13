package br.com.bip.infrastructure.persistence.jpa.delivery;

import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeliveryRequestRepositoryAdapter implements DeliveryRequestRepositoryPort {

    private final DeliveryRequestJpaRepository jpaRepository;

    public DeliveryRequestRepositoryAdapter(DeliveryRequestJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DeliveryRequest save(DeliveryRequest deliveryRequest) {
        return jpaRepository.save(deliveryRequest);
    }

    @Override
    public Optional<DeliveryRequest> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<DeliveryRequest> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<DeliveryRequest> findByClientId(UUID clientId) {
        return jpaRepository.findByClientId(clientId);
    }

    @Override
    public List<DeliveryRequest> findByStatus(DeliveryStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<DeliveryRequest> findByDriverId(UUID driverId) {
        return jpaRepository.findByDriverId(driverId);
    }
}
