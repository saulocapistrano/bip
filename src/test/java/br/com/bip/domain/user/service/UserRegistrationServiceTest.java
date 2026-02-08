package br.com.bip.domain.user.service;

import br.com.bip.application.user.dto.UserRegistrationRequest;
import br.com.bip.application.user.dto.UserResponse;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserRegistrationServiceTest {

    private UserRepositoryPort userRepositoryPort;
    private UserRegistrationService userRegistrationService;

    @BeforeEach
    void setUp() {
        userRepositoryPort = mock(UserRepositoryPort.class);
        userRegistrationService = new UserRegistrationService(userRepositoryPort);
    }

    @Test
    void registerClient_deveCriarUsuarioClientePendenteComSucesso() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "Cliente Teste",
                "cliente@teste.com",
                "85999990000",
                UserRole.BIP_CLIENTE,
                null
        );

        when(userRepositoryPort.existsByEmail("cliente@teste.com")).thenReturn(false);

        User saved = new User();
        saved.setId(UUID.randomUUID());
        saved.setName("Cliente Teste");
        saved.setEmail("cliente@teste.com");
        saved.setPhone("85999990000");
        saved.setRole(UserRole.BIP_CLIENTE);
        saved.setStatus(UserStatus.PENDING_APPROVAL);

        when(userRepositoryPort.save(any(User.class))).thenReturn(saved);

        UserResponse response = userRegistrationService.registerClient(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(userCaptor.capture());

        User toSave = userCaptor.getValue();
        assertThat(toSave.getRole()).isEqualTo(UserRole.BIP_CLIENTE);
        assertThat(toSave.getStatus()).isEqualTo(UserStatus.PENDING_APPROVAL);

        assertThat(response.email()).isEqualTo("cliente@teste.com");
        assertThat(response.role()).isEqualTo(UserRole.BIP_CLIENTE);
        assertThat(response.status()).isEqualTo(UserStatus.PENDING_APPROVAL);
    }

    @Test
    void registerClient_deveLancarExcecaoQuandoRoleDiferente() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "Cliente Teste",
                "cliente@teste.com",
                "85999990000",
                UserRole.BIP_ENTREGADOR, // errado
                null
        );

        assertThatThrownBy(() -> userRegistrationService.registerClient(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tipo de usuário não permitido");
    }

    @Test
    void registerClient_deveLancarExcecaoQuandoEmailJaExiste() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "Cliente Teste",
                "cliente@teste.com",
                "85999990000",
                UserRole.BIP_CLIENTE,
                null
        );

        when(userRepositoryPort.existsByEmail("cliente@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> userRegistrationService.registerClient(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe usuário cadastrado");
    }

    @Test
    void registerDriver_deveCriarUsuarioEntregadorComSucesso() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "Entregador Teste",
                "entregador@teste.com",
                "85999990001",
                UserRole.BIP_ENTREGADOR,
                null
        );

        when(userRepositoryPort.existsByEmail("entregador@teste.com")).thenReturn(false);

        User saved = new User();
        saved.setId(UUID.randomUUID());
        saved.setName("Entregador Teste");
        saved.setEmail("entregador@teste.com");
        saved.setPhone("85999990001");
        saved.setRole(UserRole.BIP_ENTREGADOR);
        saved.setStatus(UserStatus.PENDING_APPROVAL);

        when(userRepositoryPort.save(any(User.class))).thenReturn(saved);

        UserResponse response = userRegistrationService.registerDriver(request);

        assertThat(response.role()).isEqualTo(UserRole.BIP_ENTREGADOR);
    }
}
