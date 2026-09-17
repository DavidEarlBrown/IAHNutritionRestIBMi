package com.iah.nutrition.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record OptimizationRequest(
        Long clientId,
        Long speciesId,
        Long stageId,
        Long requirementSetId,
        Integer animalAgeDays,
        String sex,
        String breed,
        String productionLevel,
        String housing,
        String environment,
        String activityLevel,
        String pregnancyStatus,
        @DecimalMin("0.001") BigDecimal batchWeightKg,
        String optimizationType,
        List<Long> ingredientIds,
        Map<Long, Bound> ingredientBounds,
        Double calciumPhosphorusTarget,
        boolean saveFormula,
        String formulaName,
        String formulaCode,
        String notes
) {
    public record Bound(BigDecimal min, BigDecimal max) {
    }
}
