package com.iah.nutrition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StageDto(
        Long id,
        @NotNull Long speciesId,
        String speciesCode,
        @NotBlank String code,
        @NotBlank String name,
        Integer ageMinDays,
        Integer ageMaxDays,
        BigDecimal weightMinKg,
        BigDecimal weightMaxKg,
        String description,
        Boolean active
) {
}
