package br.com.bip.application.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 20)
        String phone
) {
}
