package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.mapper.DeliveryMapper;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ClientDeliveryQueryService {

    private final DeliveryRequestRepositoryPort deliveryRepository;

    public ClientDeliveryQueryService(DeliveryRequestRepositoryPort deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public List<DeliveryResponse> listDeliveriesOfClient(UUID clientId) {
        return deliveryRepository.findByClientId(clientId).stream()
                .map(DeliveryMapper::toResponse)
                .toList();
    }
}
