package br.com.bip.infrastructure.persistence.jpa.user;

import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    private UserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserRepositoryAdapter(userJpaRepository);
    }

    @Test
    void save_deveDelegarParaJpaRepository() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Nome")
                .email("user@teste.com")
                .role(UserRole.BIP_CLIENTE)
                .status(UserStatus.APPROVED)
                .build();

        when(userJpaRepository.save(user)).thenReturn(user);

        User saved = adapter.save(user);

        assertThat(saved).isSameAs(user);
        verify(userJpaRepository).save(user);
        verifyNoMoreInteractions(userJpaRepository);
    }

    @Test
    void findAll_deveDelegar() {
        when(userJpaRepository.findAll()).thenReturn(List.of());

        List<User> users = adapter.findAll();

        assertThat(users).isEmpty();
        verify(userJpaRepository).findAll();
        verifyNoMoreInteractions(userJpaRepository);
    }

    @Test
    void findById_deveDelegar() {
        UUID id = UUID.randomUUID();
        when(userJpaRepository.findById(id)).thenReturn(Optional.empty());

        Optional<User> result = adapter.findById(id);

        assertThat(result).isEmpty();
        verify(userJpaRepository).findById(id);
        verifyNoMoreInteractions(userJpaRepository);
    }

    @Test
    void findByEmail_deveDelegar() {
        when(userJpaRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

        Optional<User> result = adapter.findByEmail("a@b.com");

        assertThat(result).isEmpty();
        verify(userJpaRepository).findByEmail("a@b.com");
        verifyNoMoreInteractions(userJpaRepository);
    }

    @Test
    void existsByEmail_deveDelegar() {
        when(userJpaRepository.existsByEmail("a@b.com")).thenReturn(true);

        boolean exists = adapter.existsByEmail("a@b.com");

        assertThat(exists).isTrue();
        verify(userJpaRepository).existsByEmail("a@b.com");
        verifyNoMoreInteractions(userJpaRepository);
    }

    @Test
    void findByRoleAndStatus_deveDelegar() {
        when(userJpaRepository.findByRoleAndStatus(UserRole.BIP_ENTREGADOR, UserStatus.PENDING_APPROVAL))
                .thenReturn(List.of());

        List<User> users = adapter.findByRoleAndStatus(UserRole.BIP_ENTREGADOR, UserStatus.PENDING_APPROVAL);

        assertThat(users).isEmpty();
        verify(userJpaRepository).findByRoleAndStatus(UserRole.BIP_ENTREGADOR, UserStatus.PENDING_APPROVAL);
        verifyNoMoreInteractions(userJpaRepository);
    }

    @Test
    void deleteById_deveDelegar() {
        UUID id = UUID.randomUUID();

        adapter.deleteById(id);

        verify(userJpaRepository).deleteById(id);
        verifyNoMoreInteractions(userJpaRepository);
    }
}
