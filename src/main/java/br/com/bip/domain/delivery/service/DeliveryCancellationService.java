package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.mapper.DeliveryMapper;
import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.repository.DeliveryInRouteCachePort;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.shared.exception.BusinessException;
import br.com.bip.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class DeliveryCancellationService {

    private final DeliveryRequestRepositoryPort deliveryRepository;
    private final UserRepositoryPort userRepositoryPort;
    private final DeliveryInRouteCachePort inRouteCachePort;

    public DeliveryCancellationService(DeliveryRequestRepositoryPort deliveryRepository,
                                       UserRepositoryPort userRepositoryPort,
                                       DeliveryInRouteCachePort inRouteCachePort) {
        this.deliveryRepository = deliveryRepository;
        this.userRepositoryPort = userRepositoryPort;
        this.inRouteCachePort = inRouteCachePort;
    }

    @Transactional
    public DeliveryResponse cancelByClient(UUID deliveryId, UUID clientId, String reason) {

        User client = userRepositoryPort.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado: " + clientId));

        if (!UserRole.BIP_CLIENTE.equals(client.getRole())) {
            throw new BusinessException("Somente usuários do tipo cliente podem cancelar entregas.");
        }

        if (!UserStatus.APPROVED.equals(client.getStatus())) {
            throw new BusinessException("Cliente precisa ter cadastro aprovado para cancelar entregas.");
        }

        DeliveryRequest delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Entrega não encontrada: " + deliveryId));

        if (!clientId.equals(delivery.getClientId())) {
            throw new BusinessException("Entrega não pertence a este cliente.");
        }

        String finalReason = (reason != null && !reason.isBlank())
                ? reason
                : "Cancelado pelo cliente";

        if (DeliveryStatus.AVAILABLE.equals(delivery.getStatus())) {
            delivery.setStatus(DeliveryStatus.CANCELED);
            delivery.setCancellationReason(finalReason);

            DeliveryRequest saved = deliveryRepository.save(delivery);
            return DeliveryMapper.toResponse(saved);
        }

        if (DeliveryStatus.IN_ROUTE.equals(delivery.getStatus())) {

            if (delivery.getDriverId() == null) {
                throw new BusinessException("Entrega em rota sem entregador associado.");
            }

            User driver = userRepositoryPort.findById(delivery.getDriverId())
                    .orElseThrow(() -> new NotFoundException("Entregador não encontrado: " + delivery.getDriverId()));

            BigDecimal price = delivery.getOfferedPrice();
            if (price == null) {
                throw new BusinessException("Valor da entrega não informado.");
            }

            BigDecimal penalty = price.multiply(BigDecimal.valueOf(0.30));

            if (client.getClientBalance() == null || client.getClientBalance().compareTo(penalty) < 0) {
                throw new BusinessException("Saldo insuficiente para cancelamento com multa de 30%.");
            }

            client.setClientBalance(client.getClientBalance().subtract(penalty));
            driver.setDriverBalance(driver.getDriverBalance().add(penalty));

            userRepositoryPort.save(client);
            userRepositoryPort.save(driver);

            delivery.setStatus(DeliveryStatus.CANCELED);
            delivery.setCancellationReason(finalReason + " (multa de 30% aplicada)");

            DeliveryRequest saved = deliveryRepository.save(delivery);

            // Saiu de IN_ROUTE -> remover do Redis
            inRouteCachePort.deleteById(saved.getId());

            return DeliveryMapper.toResponse(saved);
        }

        // outros status: nao pode cancelar
        throw new BusinessException("Entrega não pode ser cancelada no status atual: " + delivery.getStatus());
    }
}
