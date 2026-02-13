package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.mapper.DeliveryMapper;
import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.port.DeliveryRealtimeNotifierPort;
import br.com.bip.domain.delivery.repository.DeliveryInRouteCachePort;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.shared.exception.BusinessException;
import br.com.bip.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeliveryAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryAssignmentService.class);

    private final DeliveryRequestRepositoryPort deliveryRepository;
    private final UserRepositoryPort userRepositoryPort;
    private final DeliveryInRouteCachePort inRouteCachePort;
    private final DeliveryRealtimeNotifierPort realtimeNotifier;
    public DeliveryAssignmentService(DeliveryRequestRepositoryPort deliveryRepository,
                                     UserRepositoryPort userRepositoryPort,
                                     DeliveryInRouteCachePort inRouteCachePort,
                                     DeliveryRealtimeNotifierPort realtimeNotifier) {
        this.deliveryRepository = deliveryRepository;
        this.userRepositoryPort = userRepositoryPort;
        this.inRouteCachePort = inRouteCachePort;
        this.realtimeNotifier =realtimeNotifier;
    }

    @Transactional
    public DeliveryResponse acceptDelivery(UUID deliveryId, UUID driverId) {

        User driver = userRepositoryPort.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Entregador não encontrado: " + driverId));

            if (!UserRole.BIP_ENTREGADOR.equals(driver.getRole())) {
                throw new BusinessException("Somente usuários do tipo entregador podem aceitar entregas.");
            }

            if (!UserStatus.APPROVED.equals(driver.getStatus())) {
                throw new BusinessException("Entregador precisa ter cadastro aprovado para aceitar entregas.");
            }

        DeliveryRequest delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Entrega não encontrada: " + deliveryId));

            if (delivery.getStatus() != DeliveryStatus.AVAILABLE) {
                throw new BusinessException("Somente entregas disponíveis podem ser aceitas.");
            }

            if (delivery.getDriverId() != null) {
                throw new BusinessException("Entrega já atribuída a um entregador.");
            }

        delivery.setDriverId(driver.getId());
        delivery.setStatus(DeliveryStatus.IN_ROUTE);

        DeliveryRequest saved = deliveryRepository.save(delivery);

        try {
            inRouteCachePort.save(saved);
        } catch (RuntimeException ex) {
            log.warn("Falha ao salvar entrega em rota no cache (Redis).", ex);
        }

        DeliveryResponse response = DeliveryMapper.toResponse(saved);

        try {
            realtimeNotifier.notifyUpdateToDriver(driver.getId(), response);
        } catch (RuntimeException ex) {
            log.warn("Falha ao notificar atualização ao entregador via realtime.", ex);
        }

        try {
            realtimeNotifier.notifyUpdateDelivery(response);
        } catch (RuntimeException ex) {
            log.warn("Falha ao notificar atualização de entrega via realtime.", ex);
        }


        return DeliveryMapper.toResponse(saved);
    }
}
