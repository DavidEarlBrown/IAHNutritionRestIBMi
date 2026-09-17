package com.iah.nutrition.web;

import com.iah.nutrition.dto.SpeciesDto;
import com.iah.nutrition.dto.StageDto;
import com.iah.nutrition.service.AnimalCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnimalCatalogController {

    private final AnimalCatalogService animalCatalogService;

    public AnimalCatalogController(AnimalCatalogService animalCatalogService) {
        this.animalCatalogService = animalCatalogService;
    }

    @GetMapping("/species")
    public List<SpeciesDto> listSpecies() {
        return animalCatalogService.findSpecies();
    }

    @PostMapping("/species")
    @ResponseStatus(HttpStatus.CREATED)
    public SpeciesDto createSpecies(@Valid @RequestBody SpeciesDto dto) {
        return animalCatalogService.createSpecies(dto);
    }

    @PutMapping("/species/{id}")
    public SpeciesDto updateSpecies(@PathVariable Long id, @Valid @RequestBody SpeciesDto dto) {
        return animalCatalogService.updateSpecies(id, dto);
    }

    @GetMapping("/species/{speciesId}/stages")
    public List<StageDto> listStages(@PathVariable Long speciesId) {
        return animalCatalogService.findStages(speciesId);
    }

    @PostMapping("/stages")
    @ResponseStatus(HttpStatus.CREATED)
    public StageDto createStage(@Valid @RequestBody StageDto dto) {
        return animalCatalogService.createStage(dto);
    }

    @PutMapping("/stages/{id}")
    public StageDto updateStage(@PathVariable Long id, @Valid @RequestBody StageDto dto) {
        return animalCatalogService.updateStage(id, dto);
    }
}
