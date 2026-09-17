package com.iah.nutrition.dto;

import jakarta.validation.constraints.NotBlank;

public record NutrientDto(
        Long id,
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String unit,
        String category,
        String description,
        Boolean active
) {
}
