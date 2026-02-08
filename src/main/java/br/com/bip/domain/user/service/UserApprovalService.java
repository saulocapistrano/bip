package br.com.bip.domain.user.service;

import br.com.bip.application.user.dto.UserResponse;
import br.com.bip.application.user.mapper.UserMapper;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import br.com.bip.shared.exception.BusinessException;
import br.com.bip.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserApprovalService {

    private final UserRepositoryPort userRepositoryPort;

    public UserApprovalService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listPendingByRole(UserRole role){
        List<User> users = userRepositoryPort.findByRoleAndStatus(role, UserStatus.PENDING_APPROVAL);
        return  users.stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse approve(UUID userId) {
        User user = getUserOrThrow(userId);
        if (user.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new BusinessException("Usuário não está pendente de aprovação");
        }
        user.setStatus(UserStatus.APPROVED);
        User saved = userRepositoryPort.save(user);
        return UserMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse reject(UUID userId){
        User user = getUserOrThrow(userId);
        if (user.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new BusinessException("Usuário não está pendente de aprovação");
        }
        user.setStatus(UserStatus.REJECTED);
        User saved = userRepositoryPort.save(user);
        return UserMapper.toResponse(saved);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepositoryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + userId));
    }


}
