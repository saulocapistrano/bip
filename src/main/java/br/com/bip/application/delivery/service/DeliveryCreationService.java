package br.com.bip.application.delivery.service;


import br.com.bip.application.delivery.dto.DeliveryCreateRequest;
import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.event.messaging.DeliveryRequestedEvent;
import br.com.bip.application.delivery.mapper.DeliveryMapper;
import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.infrastructure.messaging.kafka.delivery.DeliveryEventProducer;
import br.com.bip.shared.exception.BusinessException;
import br.com.bip.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class DeliveryCreationService {

    private final DeliveryRequestRepositoryPort deliveryRepository;
    private final UserRepositoryPort userRepositoryPort;
    private final DeliveryEventProducer eventProducer;

    public DeliveryCreationService(DeliveryRequestRepositoryPort deliveryRepository,
                                   UserRepositoryPort userRepositoryPort,
                                   DeliveryEventProducer eventProducer) {
        this.deliveryRepository = deliveryRepository;
        this.userRepositoryPort = userRepositoryPort;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public DeliveryResponse create(DeliveryCreateRequest request) {

        User client = userRepositoryPort.findById(request.clientId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado: " + request.clientId()));

        if (!UserRole.BIP_CLIENTE.equals(client.getRole())) {
            throw new BusinessException("Somente usuários do tipo cliente podem solicitar entregas.");
        }

        if (!UserStatus.APPROVED.equals(client.getStatus())) {
            throw new BusinessException("Cliente precisa ter cadastro aprovado para solicitar entregas.");
        }

        BigDecimal required = request.offeredPrice().multiply(BigDecimal.valueOf(2));

        if (client.getClientBalance() == null ||
                client.getClientBalance().compareTo(required) < 0) {
            throw new BusinessException("Saldo insuficiente para solicitar esta entrega. É necessário ter pelo menos duas vezes o valor ofertado.");
        }


        DeliveryRequest delivery = DeliveryMapper.toNewDelivery(request);

        DeliveryRequest saved = deliveryRepository.save(delivery);

        DeliveryRequestedEvent event = DeliveryMapper.toRequestedEvent(saved);
        eventProducer.sendDeliveryRequested(event);

        return DeliveryMapper.toResponse(saved);
    }

}
