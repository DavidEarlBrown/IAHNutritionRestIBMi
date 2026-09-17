package com.iah.nutrition.service;

import com.iah.nutrition.data.NutritionDataClient;
import com.iah.nutrition.dto.NutrientDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NutrientService {

    private final NutritionDataClient dataClient;

    public NutrientService(NutritionDataClient dataClient) {
        this.dataClient = dataClient;
    }

    public List<NutrientDto> findAll() {
        return dataClient.listNutrients();
    }

    public NutrientDto findById(Long id) {
        return dataClient.getNutrient(id);
    }

    public NutrientDto create(NutrientDto dto) {
        return dataClient.createNutrient(dto);
    }

    public NutrientDto update(Long id, NutrientDto dto) {
        return dataClient.updateNutrient(id, dto);
    }

    public void delete(Long id) {
        dataClient.deleteNutrient(id);
    }
}
