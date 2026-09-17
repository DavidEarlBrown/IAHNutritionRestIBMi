package com.iah.nutrition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record IngredientDto(
        Long id,
        @NotBlank String code,
        @NotBlank String name,
        String category,
        @NotNull BigDecimal price,
        String priceUnit,
        BigDecimal dryMatterPct,
        BigDecimal density,
        BigDecimal minInclusion,
        BigDecimal maxInclusion,
        BigDecimal quadraticCost,
        String supplier,
        String notes,
        Boolean active,
        List<IngredientNutrientDto> nutrients,
        List<IngredientPriceDto> prices
) {
}
