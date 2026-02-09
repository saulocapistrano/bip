package br.com.bip.application.user.service;

import br.com.bip.application.financial.messaging.FinancialTransactionEvent;
import br.com.bip.application.financial.messaging.FinancialTransactionType;
import br.com.bip.application.user.dto.WalletDepositRequest;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.infrastructure.messaging.kafka.financial.FinancialEventProducer;
import br.com.bip.shared.exception.BusinessException;
import br.com.bip.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ClientWalletService {

    private final UserRepositoryPort userRepositoryPort;
    private final FinancialEventProducer financialEventProducer;

    public ClientWalletService(UserRepositoryPort userRepositoryPort,
                               FinancialEventProducer financialEventProducer) {
        this.userRepositoryPort = userRepositoryPort;
        this.financialEventProducer = financialEventProducer;
    }

    @Transactional
    public void deposit(WalletDepositRequest request) {
        deposit(request.clientId(), request.amount());
    }

    @Transactional
    public void deposit(UUID clientId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valor de depósito deve ser maior que zero.");
        }

        User client = userRepositoryPort.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado: " + clientId));

        if (!UserRole.BIP_CLIENTE.equals(client.getRole())) {
            throw new BusinessException("Somente usuários do tipo cliente podem realizar depósito de saldo.");
        }

        if (!UserStatus.APPROVED.equals(client.getStatus())) {
            throw new BusinessException("Cliente precisa ter cadastro aprovado para realizar depósito de saldo.");
        }

        BigDecimal saldoAtual = client.getClientBalance() != null
                ? client.getClientBalance()
                : BigDecimal.ZERO;

        BigDecimal novoSaldo = saldoAtual.add(amount);
        client.setClientBalance(novoSaldo);

        userRepositoryPort.save(client);

        FinancialTransactionEvent event = new FinancialTransactionEvent(
                UUID.randomUUID(),
                FinancialTransactionType.CLIENT_DEPOSIT,
                null,
                client.getId(),
                amount,
                null,
                "Depósito de saldo na carteira do cliente",
                OffsetDateTime.now()
        );
        financialEventProducer.send(event);
    }
}
