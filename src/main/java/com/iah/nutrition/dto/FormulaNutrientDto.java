package com.iah.nutrition.dto;

import java.math.BigDecimal;

public record FormulaNutrientDto(
        Long nutrientId,
        String nutrientCode,
        String nutrientName,
        String unit,
        BigDecimal achievedValue,
        BigDecimal minValue,
        BigDecimal maxValue,
        BigDecimal targetValue
) {
}
