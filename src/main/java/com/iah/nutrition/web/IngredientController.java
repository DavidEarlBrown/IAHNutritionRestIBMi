package com.iah.nutrition.web;

import com.iah.nutrition.dto.IngredNutDto;
import com.iah.nutrition.dto.IngredientDto;
import com.iah.nutrition.dto.IngredientNutrientDto;
import com.iah.nutrition.dto.IngredientPriceDto;
import com.iah.nutrition.service.IngredientService;
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
@RequestMapping("/api/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public List<IngredientDto> list() {
        return ingredientService.findAll();
    }

    @GetMapping("/{id}")
    public IngredientDto get(@PathVariable Long id) {
        return ingredientService.findById(id);
    }

    @GetMapping("/{id}/ingrednut")
    public List<IngredNutDto> listIngredientNutrients(@PathVariable Long id) {
        ingredientService.findById(id);
        return ingredientService.listIngredNut(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientDto create(@Valid @RequestBody IngredientDto dto) {
        return ingredientService.create(dto);
    }

    @PutMapping("/{id}")
    public IngredientDto update(@PathVariable Long id, @Valid @RequestBody IngredientDto dto) {
        return ingredientService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ingredientService.delete(id);
    }

    @PutMapping("/{id}/nutrients")
    public IngredientDto upsertNutrient(@PathVariable Long id, @Valid @RequestBody IngredientNutrientDto dto) {
        return ingredientService.upsertNutrient(id, dto);
    }

    @DeleteMapping("/{id}/nutrients/{nutrientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNutrient(@PathVariable Long id, @PathVariable Long nutrientId) {
        ingredientService.deleteNutrient(id, nutrientId);
    }

    @PostMapping("/{id}/prices")
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientPriceDto addPrice(@PathVariable Long id, @Valid @RequestBody IngredientPriceDto dto) {
        return ingredientService.addPrice(id, dto);
    }
}
