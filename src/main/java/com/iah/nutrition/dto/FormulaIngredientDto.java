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
        BigDecimal lastPrice,
        BigDecimal cost
) {
    public FormulaIngredientDto {
        if (lastPrice == null) {
            lastPrice = priceUsed;
        }
        if (priceUsed == null) {
            priceUsed = lastPrice;
        }
    }
}
