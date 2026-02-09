package br.com.bip.domain.delivery.service;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.event.messaging.DeliveryCompletedEvent;
import br.com.bip.application.delivery.mapper.DeliveryMapper;
import br.com.bip.application.financial.messaging.FinancialTransactionEvent;
import br.com.bip.application.financial.messaging.FinancialTransactionType;
import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.infrastructure.messaging.kafka.delivery.DeliveryEventProducer;
import br.com.bip.infrastructure.messaging.kafka.financial.FinancialEventProducer;
import br.com.bip.shared.exception.BusinessException;
import br.com.bip.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DeliveryCompletionService {

    private final DeliveryRequestRepositoryPort deliveryRepository;
    private final UserRepositoryPort userRepositoryPort;
    private final DeliveryEventProducer eventProducer;
    private final FinancialEventProducer financialEventProducer;

    public DeliveryCompletionService(DeliveryRequestRepositoryPort deliveryRepository,
                                     UserRepositoryPort userRepositoryPort,
                                     DeliveryEventProducer eventProducer,
                                     FinancialEventProducer financialEventProducer) {
        this.deliveryRepository = deliveryRepository;
        this.userRepositoryPort = userRepositoryPort;
        this.eventProducer = eventProducer;
        this.financialEventProducer = financialEventProducer;
    }

    @Transactional
    public DeliveryResponse completeDelivery(UUID deliveryId, UUID driverId) {

        User driver = userRepositoryPort.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Entregador não encontrado: " + driverId));

        if (!UserRole.BIP_ENTREGADOR.equals(driver.getRole())) {
            throw new BusinessException("Somente usuários do tipo entregador podem concluir entregas.");
        }

        DeliveryRequest delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Entrega não encontrada: " + deliveryId));

        if (!DeliveryStatus.IN_ROUTE.equals(delivery.getStatus())) {
            throw new BusinessException("Somente entregas em rota podem ser concluídas.");
        }

        if (delivery.getDriverId() == null || !delivery.getDriverId().equals(driver.getId())) {
            throw new BusinessException("Entrega não está atribuída a este entregador.");
        }

        User client = userRepositoryPort.findById(delivery.getClientId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado: " + delivery.getClientId()));

        BigDecimal price = delivery.getOfferedPrice();
        if (price == null) {
            throw new BusinessException("Valor da entrega não informado.");
        }

        BigDecimal saldoCliente = client.getClientBalance() != null
                ? client.getClientBalance()
                : BigDecimal.ZERO;

        if (saldoCliente.compareTo(price) < 0) {
            throw new BusinessException("Saldo do cliente insuficiente para concluir a entrega.");
        }

        BigDecimal saldoDriver = driver.getDriverBalance() != null
                ? driver.getDriverBalance()
                : BigDecimal.ZERO;

        client.setClientBalance(saldoCliente.subtract(price));
        driver.setDriverBalance(saldoDriver.add(price));

        userRepositoryPort.save(client);
        userRepositoryPort.save(driver);

        delivery.setStatus(DeliveryStatus.COMPLETED);
        DeliveryRequest saved = deliveryRepository.save(delivery);

        DeliveryCompletedEvent deliveryEvent = DeliveryMapper.toCompletedEvent(saved);
        eventProducer.sendDeliveryCompleted(deliveryEvent);

        FinancialTransactionEvent financialEvent = new FinancialTransactionEvent(
                UUID.randomUUID(),
                FinancialTransactionType.DELIVERY_PAYMENT,
                client.getId(),
                driver.getId(),
                price,
                delivery.getId(),
                "Pagamento de entrega concluída",
                OffsetDateTime.now()
        );
        financialEventProducer.send(financialEvent);

        return DeliveryMapper.toResponse(saved);
    }
}
