package com.iah.nutrition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record IngredientNutrientDto(
        Long nutrientId,
        String nutrientCode,
        String nutrientName,
        String unit,
        @NotNull @DecimalMin("0") BigDecimal amount
) {
}
