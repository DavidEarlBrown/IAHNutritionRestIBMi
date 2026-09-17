package com.iah.nutrition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IngredientPriceDto(
        Long id,
        @NotNull @DecimalMin("0") BigDecimal price,
        String priceUnit,
        @NotNull LocalDate effectiveDate,
        String source,
        String notes
) {
}
