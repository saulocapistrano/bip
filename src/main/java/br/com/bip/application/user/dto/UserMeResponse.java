package br.com.bip.application.user.dto;

import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;

import java.util.UUID;

public record UserMeResponse(
        UUID id,
        UserRole role,
        UserStatus status,
        String keycloakId
) {
}
