package com.iah.nutrition.data;

import com.iah.nutrition.dto.ClientDto;
import com.iah.nutrition.dto.ClientFormulasHdr;
import com.iah.nutrition.dto.FormulaDto;
import com.iah.nutrition.dto.IngredNutDto;
import com.iah.nutrition.dto.IngredientDto;
import com.iah.nutrition.dto.IngredientNutrientDto;
import com.iah.nutrition.dto.IngredientPriceDto;
import com.iah.nutrition.dto.NutrientDto;
import com.iah.nutrition.dto.RequirementSetDto;
import com.iah.nutrition.dto.SpeciesDto;
import com.iah.nutrition.dto.StageDto;

import java.util.List;

/**
 * HTTP contract implemented by ILE RPG on IBM i ({@code /data} locally).
 * Java never reads or writes Db2 directly.
 */
public interface NutritionDataClient {

    List<NutrientDto> listNutrients();

    NutrientDto getNutrient(Long id);

    NutrientDto createNutrient(NutrientDto dto);

    NutrientDto updateNutrient(Long id, NutrientDto dto);

    void deleteNutrient(Long id);

    List<IngredientDto> listIngredients(boolean withComposition);

    IngredientDto getIngredient(Long id);

    IngredientDto createIngredient(IngredientDto dto);

    IngredientDto updateIngredient(Long id, IngredientDto dto);

    void deleteIngredient(Long id);

    IngredientDto upsertIngredientNutrient(Long ingredientId, IngredientNutrientDto dto);

    void deleteIngredientNutrient(Long ingredientId, Long nutrientId);

    List<IngredNutDto> listIngredNut(Long ingredientId);

    IngredientPriceDto addIngredientPrice(Long ingredientId, IngredientPriceDto dto);

    List<ClientDto> listClients();

    ClientDto getClient(Long id);

    ClientDto createClient(ClientDto dto);

    ClientDto updateClient(Long id, ClientDto dto);

    void deleteClient(Long id);

    List<SpeciesDto> listSpecies();

    SpeciesDto getSpecies(Long id);

    SpeciesDto createSpecies(SpeciesDto dto);

    SpeciesDto updateSpecies(Long id, SpeciesDto dto);

    List<StageDto> listStages(Long speciesId);

    StageDto getStage(Long id);

    StageDto createStage(StageDto dto);

    StageDto updateStage(Long id, StageDto dto);

    List<RequirementSetDto> listRequirements(Long speciesId, Long stageId);

    RequirementSetDto getRequirement(Long id);

    RequirementSetDto createRequirement(RequirementSetDto dto);

    RequirementSetDto updateRequirement(Long id, RequirementSetDto dto);

    void deleteRequirement(Long id);

    List<FormulaDto> listFormulas(Long clientId, Long speciesId, Long stageId, String status);

    FormulaDto getFormula(Long id);

    FormulaDto createFormula(FormulaDto dto);

    List<ClientFormulasHdr> listClientFormulasHdr(Long clientId);

    ClientFormulasHdr getClientFormulasHdr(Long formulaId);
}
