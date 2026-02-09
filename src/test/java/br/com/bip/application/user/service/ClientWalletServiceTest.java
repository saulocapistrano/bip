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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientWalletServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private FinancialEventProducer financialEventProducer;

    private ClientWalletService clientWalletService;

    @BeforeEach
    void setUp() {
        clientWalletService = new ClientWalletService(
                userRepositoryPort,
                financialEventProducer
        );
    }

    private User buildCliente(UUID id, BigDecimal saldo, UserStatus status) {
        User u = new User();
        u.setId(id);
        u.setName("Cliente");
        u.setEmail("cliente@teste.com");
        u.setRole(UserRole.BIP_CLIENTE);
        u.setStatus(status);
        u.setClientBalance(saldo);
        u.setCreatedAt(OffsetDateTime.now());
        u.setUpdatedAt(OffsetDateTime.now());
        return u;
    }

    @Test
    void deposit_deveAtualizarSaldoEPublicarEvento() {
        UUID clientId = UUID.randomUUID();
        BigDecimal saldoInicial = BigDecimal.valueOf(100);
        BigDecimal deposito = BigDecimal.valueOf(50);

        User cliente = buildCliente(clientId, saldoInicial, UserStatus.APPROVED);

        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.of(cliente));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletDepositRequest request = new WalletDepositRequest(clientId, deposito);

        clientWalletService.deposit(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertThat(saved.getClientBalance()).isEqualByComparingTo(saldoInicial.add(deposito));

        ArgumentCaptor<FinancialTransactionEvent> eventCaptor =
                ArgumentCaptor.forClass(FinancialTransactionEvent.class);
        verify(financialEventProducer).send(eventCaptor.capture());

        FinancialTransactionEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(FinancialTransactionType.CLIENT_DEPOSIT);
        assertThat(event.toUserId()).isEqualTo(clientId);
        assertThat(event.amount()).isEqualByComparingTo(deposito);
    }

    @Test
    void deposit_deveLancarErroQuandoValorMenorOuIgualZero() {
        WalletDepositRequest request = new WalletDepositRequest(
                UUID.randomUUID(),
                BigDecimal.ZERO
        );

        assertThatThrownBy(() -> clientWalletService.deposit(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Valor de depósito deve ser maior que zero");

        verifyNoInteractions(userRepositoryPort, financialEventProducer);
    }

    @Test
    void deposit_deveLancarNotFoundQuandoClienteNaoExiste() {
        UUID clientId = UUID.randomUUID();
        WalletDepositRequest request = new WalletDepositRequest(clientId, BigDecimal.TEN);

        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientWalletService.deposit(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cliente não encontrado");

        verify(userRepositoryPort).findById(clientId);
        verifyNoMoreInteractions(userRepositoryPort);
        verifyNoInteractions(financialEventProducer);
    }

    @Test
    void deposit_deveLancarErroQuandoUsuarioNaoEhCliente() {
        UUID clientId = UUID.randomUUID();
        User user = new User();
        user.setId(clientId);
        user.setRole(UserRole.BIP_ENTREGADOR);
        user.setStatus(UserStatus.APPROVED);

        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.of(user));

        WalletDepositRequest request = new WalletDepositRequest(clientId, BigDecimal.TEN);

        assertThatThrownBy(() -> clientWalletService.deposit(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Somente usuários do tipo cliente");

        verify(userRepositoryPort).findById(clientId);
        verifyNoMoreInteractions(userRepositoryPort);
        verifyNoInteractions(financialEventProducer);
    }

    @Test
    void deposit_deveLancarErroQuandoClienteNaoAprovado() {
        UUID clientId = UUID.randomUUID();
        User user = buildCliente(clientId, BigDecimal.ZERO, UserStatus.PENDING_APPROVAL);

        when(userRepositoryPort.findById(clientId)).thenReturn(Optional.of(user));

        WalletDepositRequest request = new WalletDepositRequest(clientId, BigDecimal.TEN);

        assertThatThrownBy(() -> clientWalletService.deposit(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cadastro aprovado");

        verify(userRepositoryPort).findById(clientId);
        verifyNoMoreInteractions(userRepositoryPort);
        verifyNoInteractions(financialEventProducer);
    }
}
