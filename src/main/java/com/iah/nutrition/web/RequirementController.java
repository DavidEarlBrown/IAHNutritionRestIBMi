package com.iah.nutrition.web;

import com.iah.nutrition.dto.RequirementSetDto;
import com.iah.nutrition.service.RequirementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @GetMapping
    public List<RequirementSetDto> list(
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) Long stageId
    ) {
        return requirementService.findAll(speciesId, stageId);
    }

    @GetMapping("/{id}")
    public RequirementSetDto get(@PathVariable Long id) {
        return requirementService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementSetDto create(@Valid @RequestBody RequirementSetDto dto) {
        return requirementService.create(dto);
    }

    @PutMapping("/{id}")
    public RequirementSetDto update(@PathVariable Long id, @Valid @RequestBody RequirementSetDto dto) {
        return requirementService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        requirementService.delete(id);
    }
}
