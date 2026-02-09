package br.com.bip.application.user.mapper;

import br.com.bip.application.user.dto.UserRegistrationRequest;
import br.com.bip.application.user.dto.UserResponse;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserStatus;

import java.math.BigDecimal;

public class UserMapper {

    private UserMapper(){

    }

    public static User toNewUser(UserRegistrationRequest request){
        return User.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .role(request.role())
                .status(UserStatus.PENDING_APPROVAL)
                .clientBalance(BigDecimal.ZERO)
                .driverBalance(BigDecimal.ZERO)
                .driverScore(BigDecimal.ZERO)
                .build();
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
