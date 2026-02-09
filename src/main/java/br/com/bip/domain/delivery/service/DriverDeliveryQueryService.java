package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.mapper.DeliveryMapper;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DriverDeliveryQueryService {
    private  final DeliveryRequestRepositoryPort deliveryRepository;

    public DriverDeliveryQueryService(DeliveryRequestRepositoryPort deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public List<DeliveryResponse> listAvailableForDriver() {
        return deliveryRepository.findByStatus(DeliveryStatus.AVAILABLE).stream()
                .map(DeliveryMapper::toResponse)
                .toList();
    }

    public List<DeliveryResponse> listDeliveriesOfDriver(UUID driverId) {
        return deliveryRepository.findByDriverId(driverId).stream()
                .map(DeliveryMapper::toResponse)
                .toList();
    }
}
