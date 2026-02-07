package br.com.bip.domain.user.service;

import br.com.bip.application.user.dto.UserRegistrationRequest;
import br.com.bip.application.user.dto.UserResponse;
import br.com.bip.application.user.mapper.UserMapper;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.shared.exception.BusinessException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {
    private final UserRepositoryPort userRepositoryPort;

    public UserRegistrationService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Transactional
    public UserResponse registerClient(UserRegistrationRequest request){
        return registerUserWithRole(request, UserRole.BIP_ENTREGADOR);
    }

    private UserResponse registerUserWithRole(UserRegistrationRequest request, UserRole userRole) {

        if (!userRole.equals(request.role())) {
            throw new BusinessException("Role inválido para este endpoint");
        }

        if (userRepositoryPort.existsByEmail(request.email())) {
            throw new BusinessException("Já existe entregador cadastrado com este email.");
        }

        User newUser = UserMapper.toNewUser(request);
        newUser.setRole(userRole);

        User saved = userRepositoryPort.save(newUser);
        return UserMapper.toResponse(saved);

    }

}
