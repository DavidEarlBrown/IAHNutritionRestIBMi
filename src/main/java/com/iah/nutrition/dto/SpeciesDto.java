package com.iah.nutrition.dto;

import jakarta.validation.constraints.NotBlank;

public record SpeciesDto(
        Long id,
        @NotBlank String code,
        @NotBlank String name,
        String description,
        Boolean active
) {
}
