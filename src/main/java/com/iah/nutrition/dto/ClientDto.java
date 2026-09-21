package com.iah.nutrition.dto;

import jakarta.validation.constraints.NotBlank;

public record ClientDto(
        Long id,
        @NotBlank String code,
        @NotBlank String name,
        String contactName,
        String phone,
        String email,
        String address,
        String city,
        String state,
        String postalCode,
        String country,
        String notes,
        Boolean isDefault,
        Boolean active
) {
    public ClientDto {
        isDefault = Boolean.TRUE.equals(isDefault);
        active = active == null || active;
    }
}
