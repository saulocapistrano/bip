package br.com.bip.domain.user.service;

import br.com.bip.application.user.dto.UserResponse;
import br.com.bip.application.user.dto.UserUpdateRequest;
import br.com.bip.application.user.mapper.UserMapper;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserManagementService {

    private final UserRepositoryPort userRepositoryPort;

    public UserManagementService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        User user = getUserOrThrow(id);
        return UserMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(UserRole role, UserStatus status) {

        List<User> users;

        if (role != null && status != null) {
            users = userRepositoryPort.findByRoleAndStatus(role, status);
        } else {
            users = userRepositoryPort.findAll();
        }

        return users.stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = getUserOrThrow(id);

        user.setName(request.name());
        user.setPhone(request.phone());

        User saved = userRepositoryPort.save(user);
        return UserMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        User user = getUserOrThrow(id);
        userRepositoryPort.deleteById(user.getId());
    }

    private User getUserOrThrow(UUID id) {
        return userRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
    }
}
