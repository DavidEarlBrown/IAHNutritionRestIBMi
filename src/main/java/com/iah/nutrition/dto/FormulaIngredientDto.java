package com.iah.nutrition.dto;

import java.math.BigDecimal;

public record FormulaIngredientDto(
        Long ingredientId,
        String ingredientCode,
        String ingredientName,
        BigDecimal inclusionFrac,
        BigDecimal inclusionPct,
        BigDecimal amountKg,
        BigDecimal priceUsed,
        BigDecimal cost
) {
}
