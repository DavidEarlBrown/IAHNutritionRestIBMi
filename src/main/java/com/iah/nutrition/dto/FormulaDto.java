package com.iah.nutrition.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public record FormulaDto(
        Long id,
        String code,
        String name,
        Long clientId,
        String clientName,
        String status,
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
    public static final String STATUS_OPTIMIZED = "OPTIMIZED";
    public static final String STATUS_PREFIXED = "PREFIXED";

    public FormulaDto {
        status = normalizeStatus(status, optimizationType, solverStatus);
    }

    private static String normalizeStatus(String status, String optimizationType, String solverStatus) {
        if (status != null && !status.isBlank()) {
            return status.trim().toUpperCase(Locale.ROOT);
        }
        if (solverStatus != null && !solverStatus.isBlank()
                && !"NONE".equalsIgnoreCase(solverStatus)) {
            return STATUS_OPTIMIZED;
        }
        if (optimizationType != null && !optimizationType.isBlank()
                && !"NONE".equalsIgnoreCase(optimizationType)) {
            return STATUS_OPTIMIZED;
        }
        return STATUS_PREFIXED;
    }
}
