package br.com.bip.application.user.mapper;

import br.com.bip.application.user.dto.UserRegistrationRequest;
import br.com.bip.application.user.dto.UserResponse;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserStatus;

public class UserMapper {

    private UserMapper(){

    }

    public static User toNewUser(UserRegistrationRequest request){
        return User.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.email())
                .role(request.role())
                .status(UserStatus.PENDING_APROVAL)
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
