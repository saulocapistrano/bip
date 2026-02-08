package br.com.bip.application.user.dto;

import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @Size(max = 20)
        String phone,

        @NotNull
        UserRole role,

        UserStatus status
) {
}
