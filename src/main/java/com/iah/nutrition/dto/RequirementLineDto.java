package com.iah.nutrition.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RequirementLineDto(
        Long id,
        @NotNull Long nutrientId,
        String nutrientCode,
        String nutrientName,
        String unit,
        BigDecimal minValue,
        BigDecimal maxValue,
        BigDecimal targetValue
) {
}
