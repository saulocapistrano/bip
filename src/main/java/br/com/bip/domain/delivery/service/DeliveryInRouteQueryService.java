package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.mapper.DeliveryMapper;
import br.com.bip.domain.delivery.repository.DeliveryInRouteCachePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DeliveryInRouteQueryService {

    private final DeliveryInRouteCachePort inRouteCachePort;

    public DeliveryInRouteQueryService(DeliveryInRouteCachePort inRouteCachePort) {
        this.inRouteCachePort = inRouteCachePort;
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> listByDriver(UUID driverId) {
        return inRouteCachePort.findByDriverId(driverId).stream()
                .map(DeliveryMapper::toResponse)
                .toList();
    }
}
