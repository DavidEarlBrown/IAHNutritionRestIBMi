package com.iah.nutrition.service;

import com.iah.nutrition.data.NutritionDataClient;
import com.iah.nutrition.dto.SpeciesDto;
import com.iah.nutrition.dto.StageDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnimalCatalogService {

    private final NutritionDataClient dataClient;

    public AnimalCatalogService(NutritionDataClient dataClient) {
        this.dataClient = dataClient;
    }

    public List<SpeciesDto> findSpecies() {
        return dataClient.listSpecies();
    }

    public SpeciesDto createSpecies(SpeciesDto dto) {
        return dataClient.createSpecies(dto);
    }

    public SpeciesDto updateSpecies(Long id, SpeciesDto dto) {
        return dataClient.updateSpecies(id, dto);
    }

    public List<StageDto> findStages(Long speciesId) {
        return dataClient.listStages(speciesId);
    }

    public StageDto createStage(StageDto dto) {
        return dataClient.createStage(dto);
    }

    public StageDto updateStage(Long id, StageDto dto) {
        return dataClient.updateStage(id, dto);
    }
}
