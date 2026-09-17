package com.iah.nutrition.dto;

import java.math.BigDecimal;

/**
 * One cell of the dense ingredient x nutrient matrix ({@code INGREDNUT}).
 */
public record IngredNutDto(
        Long ingredientId,
        String ingredientCode,
        Long nutrientId,
        String nutrientCode,
        String nutrientName,
        String unit,
        BigDecimal amount
) {
}
