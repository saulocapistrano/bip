package br.com.bip.domain.user.service;

import br.com.bip.application.user.dto.UserRegistrationRequest;
import br.com.bip.application.user.dto.UserResponse;
import br.com.bip.application.user.mapper.UserMapper;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationService.class);

    private final UserRepositoryPort userRepositoryPort;

    public UserRegistrationService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Transactional
    public UserResponse registerClient(UserRegistrationRequest request) {
        return registerUserWithRole(request, UserRole.BIP_CLIENTE);
    }

    @Transactional
    public UserResponse registerDriver(UserRegistrationRequest request) {
        return registerUserWithRole(request, UserRole.BIP_ENTREGADOR);
    }

    private UserResponse registerUserWithRole(UserRegistrationRequest request, UserRole requiredRole) {

        log.info(
                "Registrando usuario: requiredRole={}, requestRole={}, email={}, phone='{}', phoneLength={}",
                requiredRole,
                request.role(),
                request.email(),
                request.phone(),
                request.phone() != null ? request.phone().length() : null
        );

        if (!requiredRole.equals(request.role())) {
            throw new BusinessException("Tipo de usuário não permitido para esta operação. Em caso de dúvida, entre em contato pelo SAC.");
        }

        if (userRepositoryPort.existsByEmail(request.email())) {
            throw new BusinessException("Já existe usuário cadastrado com este email.");
        }

        User newUser = UserMapper.toNewUser(request);
        newUser.setRole(requiredRole);

        log.info(
                "Entidade User antes de salvar: role={}, email={}, phone='{}', phoneLength={}",
                newUser.getRole(),
                newUser.getEmail(),
                newUser.getPhone(),
                newUser.getPhone() != null ? newUser.getPhone().length() : null
        );

        User saved = userRepositoryPort.save(newUser);
        return UserMapper.toResponse(saved);
    }
}
