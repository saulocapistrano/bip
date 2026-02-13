package br.com.bip.application.security;

import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.shared.exception.BusinessException;
import br.com.bip.shared.exception.NotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AuthenticatedUserResolver {

    private final UserRepositoryPort userRepositoryPort;

    public AuthenticatedUserResolver(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Transactional
    public User resolveCurrentUser(Jwt jwt) {
        String subject = jwt.getSubject();

        if (subject != null && !subject.isBlank()) {
            Optional<User> byKeycloakId = userRepositoryPort.findByKeycloakId(subject);
            if (byKeycloakId.isPresent()) {
                return byKeycloakId.get();
            }
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new NotFoundException("Usuário autenticado sem email no token.");
        }

        Optional<User> byEmail = userRepositoryPort.findByEmail(email);

        if (byEmail.isPresent()) {
            User user = byEmail.get();

            if (user.getKeycloakId() == null || user.getKeycloakId().isBlank()) {
                user.setKeycloakId(subject);
                return userRepositoryPort.save(user);
            }

            return user;
        }

        return createUserFromJwt(jwt);
    }

    private User createUserFromJwt(Jwt jwt) {
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        if (email == null || email.isBlank()) {
            throw new BusinessException("Token de autenticação não contém email válido para criação de usuário.");
        }

        if (name == null || name.isBlank()) {
            name = email;
        }

        UserRole role = extractRoleFromJwt(jwt);

        OffsetDateTime now = OffsetDateTime.now();

        User.UserBuilder builder = User.builder()
                .keycloakId(subject)
                .name(name)
                .email(email)
                .phone(null)
                .role(role)
                .status(UserStatus.PENDING_APPROVAL)
                .clientBalance(BigDecimal.ZERO)
                .driverBalance(BigDecimal.ZERO)
                .driverScore(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now);

        if (UserRole.BIP_ENTREGADOR.equals(role)) {
            builder.driverScore(new BigDecimal("1000.00"));
        }

        return userRepositoryPort.save(builder.build());
    }

    private UserRole extractRoleFromJwt(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        List<String> roles = Collections.emptyList();

        if (realmAccess != null) {
            Object rawRoles = realmAccess.get("roles");
            if (rawRoles instanceof List<?> list) {
                roles = list.stream().map(String::valueOf).toList();
            } else if (rawRoles instanceof java.util.Collection<?> collection) {
                roles = collection.stream().map(String::valueOf).toList();
            }
        }

        if (roles.contains("bip-admin")) {
            return UserRole.BIP_ADMIN;
        }
        if (roles.contains("bip-entregador")) {
            return UserRole.BIP_ENTREGADOR;
        }
        if (roles.contains("bip-cliente")) {
            return UserRole.BIP_CLIENTE;
        }

        throw new BusinessException("Usuário autenticado não possui role de domínio válida no realm (bip-admin / bip-cliente / bip-entregador).");
    }
}
