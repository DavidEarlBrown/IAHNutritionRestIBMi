package com.iah.nutrition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record RequirementSetDto(
        Long id,
        @NotNull Long speciesId,
        @NotNull Long stageId,
        String speciesCode,
        String stageCode,
        @NotBlank String name,
        String sex,
        String breed,
        String productionLevel,
        BigDecimal productionValue,
        String productionUnit,
        BigDecimal bodyWeightKg,
        String pregnancyStatus,
        String housing,
        String environment,
        String activityLevel,
        String source,
        String notes,
        Boolean active,
        List<@Valid RequirementLineDto> lines
) {
}
