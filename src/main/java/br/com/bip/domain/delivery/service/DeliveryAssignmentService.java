package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.mapper.DeliveryMapper;
import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.shared.exception.BusinessException;
import br.com.bip.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeliveryAssignmentService {

    private final DeliveryRequestRepositoryPort deliveryRepository;
    private final UserRepositoryPort userRepositoryPort;

    public DeliveryAssignmentService(DeliveryRequestRepositoryPort deliveryRepository,
                                     UserRepositoryPort userRepositoryPort) {
        this.deliveryRepository = deliveryRepository;
        this.userRepositoryPort = userRepositoryPort;
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
            throw new BusinessException("Entrega não está disponível para aceitação.");
        }

        delivery.setDriverId(driver.getId());
        delivery.setStatus(DeliveryStatus.IN_ROUTE);

        DeliveryRequest saved = deliveryRepository.save(delivery);

        // voltar aqui e disparar evento Kafka / WebSocket notificando o cliente

        return DeliveryMapper.toResponse(saved);
    }
}
