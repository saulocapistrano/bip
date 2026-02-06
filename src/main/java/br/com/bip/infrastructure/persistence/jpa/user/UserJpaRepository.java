package br.com.bip.infrastructure.persistence.jpa.user;

import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRoleAndStatus(UserRole role, UserStatus status);
}
