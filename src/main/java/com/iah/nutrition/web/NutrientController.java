package com.iah.nutrition.web;

import com.iah.nutrition.dto.NutrientDto;
import com.iah.nutrition.service.NutrientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/nutrients")
public class NutrientController {

    private final NutrientService nutrientService;

    public NutrientController(NutrientService nutrientService) {
        this.nutrientService = nutrientService;
    }

    @GetMapping
    public List<NutrientDto> list() {
        return nutrientService.findAll();
    }

    @GetMapping("/{id}")
    public NutrientDto get(@PathVariable Long id) {
        return nutrientService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NutrientDto create(@Valid @RequestBody NutrientDto dto) {
        return nutrientService.create(dto);
    }

    @PutMapping("/{id}")
    public NutrientDto update(@PathVariable Long id, @Valid @RequestBody NutrientDto dto) {
        return nutrientService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        nutrientService.delete(id);
    }
}
