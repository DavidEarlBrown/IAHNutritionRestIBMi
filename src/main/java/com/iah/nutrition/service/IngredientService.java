package com.iah.nutrition.service;

import com.iah.nutrition.data.NutritionDataClient;
import com.iah.nutrition.dto.IngredNutDto;
import com.iah.nutrition.dto.IngredientDto;
import com.iah.nutrition.dto.IngredientNutrientDto;
import com.iah.nutrition.dto.IngredientPriceDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngredientService {

    private final NutritionDataClient dataClient;

    public IngredientService(NutritionDataClient dataClient) {
        this.dataClient = dataClient;
    }

    public List<IngredientDto> findAll() {
        return dataClient.listIngredients(false);
    }

    public IngredientDto findById(Long id) {
        return dataClient.getIngredient(id);
    }

    public IngredientDto create(IngredientDto dto) {
        return dataClient.createIngredient(dto);
    }

    public IngredientDto update(Long id, IngredientDto dto) {
        return dataClient.updateIngredient(id, dto);
    }

    public void delete(Long id) {
        dataClient.deleteIngredient(id);
    }

    public IngredientDto upsertNutrient(Long ingredientId, IngredientNutrientDto dto) {
        return dataClient.upsertIngredientNutrient(ingredientId, dto);
    }

    public void deleteNutrient(Long ingredientId, Long nutrientId) {
        dataClient.deleteIngredientNutrient(ingredientId, nutrientId);
    }

    public List<IngredNutDto> listIngredNut(Long ingredientId) {
        return dataClient.listIngredNut(ingredientId);
    }

    public IngredientPriceDto addPrice(Long ingredientId, IngredientPriceDto dto) {
        return dataClient.addIngredientPrice(ingredientId, dto);
    }
}
