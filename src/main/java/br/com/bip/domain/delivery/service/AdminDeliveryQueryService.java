package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.mapper.DeliveryMapper;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminDeliveryQueryService {

    private final DeliveryRequestRepositoryPort deliveryRepository;

    public AdminDeliveryQueryService(DeliveryRequestRepositoryPort deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> listAll(DeliveryStatus status) {
        if (status == null) {
            return deliveryRepository.findAll().stream()
                    .map(DeliveryMapper::toResponse)
                    .toList();
        }

        return deliveryRepository.findByStatus(status).stream()
                .map(DeliveryMapper::toResponse)
                .toList();
    }
}
