package br.com.bip.domain.user.service;

import br.com.bip.application.user.dto.UserResponse;
import br.com.bip.application.user.dto.UserUpdateRequest;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        userManagementService = new UserManagementService(userRepositoryPort);
    }

    private User buildUser(UUID id, String name, String email, String phone,
                           UserRole role, UserStatus status) {

        return User.builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .role(role)
                .status(status)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }


    @Test
    void getById_deveRetornarUsuarioQuandoEncontrado() {
        UUID id = UUID.randomUUID();
        User user = buildUser(
                id,
                "Cliente Teste",
                "cliente@teste.com",
                "85999990000",
                UserRole.BIP_CLIENTE,
                UserStatus.APPROVED
        );

        when(userRepositoryPort.findById(id)).thenReturn(Optional.of(user));

        UserResponse response = userManagementService.getById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Cliente Teste");
        assertThat(response.email()).isEqualTo("cliente@teste.com");
        assertThat(response.phone()).isEqualTo("85999990000");
        assertThat(response.role()).isEqualTo(UserRole.BIP_CLIENTE);
        assertThat(response.status()).isEqualTo(UserStatus.APPROVED);

        verify(userRepositoryPort).findById(id);
        verifyNoMoreInteractions(userRepositoryPort);
    }

    @Test
    void getById_deveLancarNotFoundQuandoUsuarioNaoExiste() {
        UUID id = UUID.randomUUID();
        when(userRepositoryPort.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.getById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");

        verify(userRepositoryPort).findById(id);
        verifyNoMoreInteractions(userRepositoryPort);
    }


    @Test
    void list_semFiltrosDeveRetornarTodosUsuarios() {
        User user1 = buildUser(
                UUID.randomUUID(),
                "Cliente 1",
                "cliente1@teste.com",
                "85999990000",
                UserRole.BIP_CLIENTE,
                UserStatus.APPROVED
        );
        User user2 = buildUser(
                UUID.randomUUID(),
                "Entregador 1",
                "entregador1@teste.com",
                "85999990001",
                UserRole.BIP_ENTREGADOR,
                UserStatus.APPROVED
        );

        when(userRepositoryPort.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> responses = userManagementService.list(null, null);

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(UserResponse::email)
                .containsExactlyInAnyOrder("cliente1@teste.com", "entregador1@teste.com");

        verify(userRepositoryPort).findAll();
        verifyNoMoreInteractions(userRepositoryPort);
    }

    @Test
    void list_comRoleEStatusDeveFiltrarPorAmbos() {
        UserRole role = UserRole.BIP_ENTREGADOR;
        UserStatus status = UserStatus.PENDING_APPROVAL;

        User user1 = buildUser(
                UUID.randomUUID(),
                "Entregador Pendente",
                "entregador.pendente@teste.com",
                "85999990001",
                role,
                status
        );

        when(userRepositoryPort.findByRoleAndStatus(role, status))
                .thenReturn(List.of(user1));

        List<UserResponse> responses = userManagementService.list(role, status);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).role()).isEqualTo(role);
        assertThat(responses.get(0).status()).isEqualTo(status);

        verify(userRepositoryPort).findByRoleAndStatus(role, status);
        verifyNoMoreInteractions(userRepositoryPort);
    }


    @Test
    void update_deveAtualizarNomeETelefoneQuandoUsuarioExiste() {
        UUID id = UUID.randomUUID();

        User existing = buildUser(
                id,
                "Nome Antigo",
                "cliente@teste.com",
                "11111111111",
                UserRole.BIP_CLIENTE,
                UserStatus.APPROVED
        );

        UserUpdateRequest request = new UserUpdateRequest(
                "Nome Novo",
                "85999990000"
        );

        when(userRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userManagementService.update(id, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).findById(id);
        verify(userRepositoryPort).save(userCaptor.capture());
        verifyNoMoreInteractions(userRepositoryPort);

        User saved = userCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Nome Novo");
        assertThat(saved.getPhone()).isEqualTo("85999990000");

        assertThat(response.name()).isEqualTo("Nome Novo");
        assertThat(response.phone()).isEqualTo("85999990000");
    }

    @Test
    void update_deveLancarNotFoundQuandoUsuarioNaoExiste() {
        UUID id = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("Algum Nome", "85999990000");

        when(userRepositoryPort.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.update(id, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");

        verify(userRepositoryPort).findById(id);
        verifyNoMoreInteractions(userRepositoryPort);
    }


    @Test
    void delete_deveExcluirQuandoUsuarioExiste() {
        UUID id = UUID.randomUUID();
        User existing = buildUser(
                id,
                "Cliente Teste",
                "cliente@teste.com",
                "85999990000",
                UserRole.BIP_CLIENTE,
                UserStatus.APPROVED
        );

        when(userRepositoryPort.findById(id)).thenReturn(Optional.of(existing));

        userManagementService.delete(id);

        verify(userRepositoryPort).findById(id);
        verify(userRepositoryPort).deleteById(id);
        verifyNoMoreInteractions(userRepositoryPort);
    }

    @Test
    void delete_deveLancarNotFoundQuandoUsuarioNaoExiste() {
        UUID id = UUID.randomUUID();
        when(userRepositoryPort.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.delete(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");

        verify(userRepositoryPort).findById(id);
        verifyNoMoreInteractions(userRepositoryPort);
    }
}
