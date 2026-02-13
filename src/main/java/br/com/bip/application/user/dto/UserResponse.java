package br.com.bip.application.user.dto;

import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        BigDecimal clientBalance,
        BigDecimal driverBalance,
        BigDecimal driverScore,
        OffsetDateTime createdAt
) {
}
