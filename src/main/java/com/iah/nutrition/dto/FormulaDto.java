package com.iah.nutrition.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FormulaDto(
        Long id,
        String code,
        String name,
        Long clientId,
        String clientName,
        Long speciesId,
        String speciesCode,
        Long stageId,
        String stageCode,
        Integer animalAgeDays,
        String sex,
        String breed,
        String productionLevel,
        String housing,
        String environment,
        BigDecimal batchWeightKg,
        BigDecimal totalCost,
        BigDecimal costPerKg,
        String optimizationType,
        String solverStatus,
        BigDecimal objectiveValue,
        String notes,
        Instant createdAt,
        List<FormulaIngredientDto> ingredients,
        List<FormulaNutrientDto> nutrients
) {
}
