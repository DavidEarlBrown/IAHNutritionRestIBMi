package com.iah.nutrition.web;

import com.iah.nutrition.dto.IngredNutDto;
import com.iah.nutrition.service.IngredientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ingrednut")
public class IngredNutController {

    private final IngredientService ingredientService;

    public IngredNutController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public List<IngredNutDto> list(@RequestParam(required = false) Long ingredientId) {
        return ingredientService.listIngredNut(ingredientId);
    }
}
