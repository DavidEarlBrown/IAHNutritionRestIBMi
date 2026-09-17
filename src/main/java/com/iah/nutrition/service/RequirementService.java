package com.iah.nutrition.service;

import com.iah.nutrition.data.NutritionDataClient;
import com.iah.nutrition.dto.RequirementSetDto;
import com.iah.nutrition.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class RequirementService {

    private final NutritionDataClient dataClient;

    public RequirementService(NutritionDataClient dataClient) {
        this.dataClient = dataClient;
    }

    public List<RequirementSetDto> findAll(Long speciesId, Long stageId) {
        return dataClient.listRequirements(speciesId, stageId);
    }

    public RequirementSetDto findById(Long id) {
        return dataClient.getRequirement(id);
    }

    public RequirementSetDto create(RequirementSetDto dto) {
        return dataClient.createRequirement(dto);
    }

    public RequirementSetDto update(Long id, RequirementSetDto dto) {
        return dataClient.updateRequirement(id, dto);
    }

    public void delete(Long id) {
        dataClient.deleteRequirement(id);
    }

    public RequirementSetDto match(Long speciesId, Long stageId, String sex, String breed,
                                   String productionLevel, String housing, String environment,
                                   String activityLevel, String pregnancyStatus) {
        List<RequirementSetDto> candidates = dataClient.listRequirements(speciesId, stageId);
        if (candidates.isEmpty()) {
            throw new BusinessException("No nutritional requirement set found for that animal and age/stage");
        }
        return candidates.stream()
                .max(Comparator.comparingInt(set -> score(set, sex, breed, productionLevel, housing, environment,
                        activityLevel, pregnancyStatus)))
                .orElseThrow();
    }

    private int score(RequirementSetDto set, String sex, String breed, String productionLevel,
                      String housing, String environment, String activityLevel, String pregnancyStatus) {
        int score = 0;
        score += matchFactor(set.sex(), sex);
        score += matchFactor(set.breed(), breed);
        score += matchFactor(set.productionLevel(), productionLevel);
        score += matchFactor(set.housing(), housing);
        score += matchFactor(set.environment(), environment);
        score += matchFactor(set.activityLevel(), activityLevel);
        score += matchFactor(set.pregnancyStatus(), pregnancyStatus);
        return score;
    }

    private int matchFactor(String stored, String requested) {
        if (isBlank(stored) && isBlank(requested)) {
            return 1;
        }
        if (isBlank(requested) || isBlank(stored)) {
            return 0;
        }
        return Objects.equals(stored.trim().toUpperCase(Locale.ROOT), requested.trim().toUpperCase(Locale.ROOT)) ? 3 : -2;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
