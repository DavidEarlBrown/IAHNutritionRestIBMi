package com.iah.nutrition.web;

import com.iah.nutrition.data.RpgDataStore;
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
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
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

/**
 * Local stand-in for the ILE RPG CGI/IWS data API. Java always calls these
 * paths over HTTP; on IBM i the same paths are served by IAHRSTD.
 */
@RestController
@RequestMapping("/data")
@Profile("local")
public class LocalRpgDataController {

    private final RpgDataStore store;

    public LocalRpgDataController(RpgDataStore store) {
        this.store = store;
    }

    @GetMapping("/nutrients")
    public List<NutrientDto> listNutrients() {
        return store.listNutrients();
    }

    @GetMapping("/nutrients/{id}")
    public NutrientDto getNutrient(@PathVariable Long id) {
        return store.getNutrient(id);
    }

    @PostMapping("/nutrients")
    @ResponseStatus(HttpStatus.CREATED)
    public NutrientDto createNutrient(@Valid @RequestBody NutrientDto dto) {
        return store.createNutrient(dto);
    }

    @PutMapping("/nutrients/{id}")
    public NutrientDto updateNutrient(@PathVariable Long id, @Valid @RequestBody NutrientDto dto) {
        return store.updateNutrient(id, dto);
    }

    @DeleteMapping("/nutrients/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNutrient(@PathVariable Long id) {
        store.deleteNutrient(id);
    }

    @GetMapping("/ingredients")
    public List<IngredientDto> listIngredients(@RequestParam(defaultValue = "summary") String view) {
        return store.listIngredients("full".equalsIgnoreCase(view));
    }

    @GetMapping("/ingredients/{id}")
    public IngredientDto getIngredient(@PathVariable Long id) {
        return store.getIngredient(id);
    }

    @GetMapping("/ingrednut")
    public List<IngredNutDto> listIngredNut(@RequestParam(required = false) Long ingredientId) {
        return store.listIngredNut(ingredientId);
    }

    @GetMapping("/ingredients/{id}/ingrednut")
    public List<IngredNutDto> listIngredientIngredNut(@PathVariable Long id) {
        store.getIngredient(id);
        return store.listIngredNut(id);
    }

    @PostMapping("/ingredients")
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientDto createIngredient(@Valid @RequestBody IngredientDto dto) {
        return store.createIngredient(dto);
    }

    @PutMapping("/ingredients/{id}")
    public IngredientDto updateIngredient(@PathVariable Long id, @Valid @RequestBody IngredientDto dto) {
        return store.updateIngredient(id, dto);
    }

    @DeleteMapping("/ingredients/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIngredient(@PathVariable Long id) {
        store.deleteIngredient(id);
    }

    @PutMapping("/ingredients/{id}/nutrients")
    public IngredientDto upsertIngredientNutrient(@PathVariable Long id, @Valid @RequestBody IngredientNutrientDto dto) {
        return store.upsertIngredientNutrient(id, dto);
    }

    @DeleteMapping("/ingredients/{id}/nutrients/{nutrientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIngredientNutrient(@PathVariable Long id, @PathVariable Long nutrientId) {
        store.deleteIngredientNutrient(id, nutrientId);
    }

    @PostMapping("/ingredients/{id}/prices")
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientPriceDto addPrice(@PathVariable Long id, @Valid @RequestBody IngredientPriceDto dto) {
        return store.addIngredientPrice(id, dto);
    }

    @GetMapping("/clients")
    public List<ClientDto> listClients() {
        return store.listClients();
    }

    @GetMapping("/clients/{id}")
    public ClientDto getClient(@PathVariable Long id) {
        return store.getClient(id);
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientDto createClient(@Valid @RequestBody ClientDto dto) {
        return store.createClient(dto);
    }

    @PutMapping("/clients/{id}")
    public ClientDto updateClient(@PathVariable Long id, @Valid @RequestBody ClientDto dto) {
        return store.updateClient(id, dto);
    }

    @DeleteMapping("/clients/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClient(@PathVariable Long id) {
        store.deleteClient(id);
    }

    @GetMapping("/species")
    public List<SpeciesDto> listSpecies() {
        return store.listSpecies();
    }

    @GetMapping("/species/{id}")
    public SpeciesDto getSpecies(@PathVariable Long id) {
        return store.getSpecies(id);
    }

    @PostMapping("/species")
    @ResponseStatus(HttpStatus.CREATED)
    public SpeciesDto createSpecies(@Valid @RequestBody SpeciesDto dto) {
        return store.createSpecies(dto);
    }

    @PutMapping("/species/{id}")
    public SpeciesDto updateSpecies(@PathVariable Long id, @Valid @RequestBody SpeciesDto dto) {
        return store.updateSpecies(id, dto);
    }

    @GetMapping("/species/{speciesId}/stages")
    public List<StageDto> listStages(@PathVariable Long speciesId) {
        return store.listStages(speciesId);
    }

    @GetMapping("/stages/{id}")
    public StageDto getStage(@PathVariable Long id) {
        return store.getStage(id);
    }

    @PostMapping("/stages")
    @ResponseStatus(HttpStatus.CREATED)
    public StageDto createStage(@Valid @RequestBody StageDto dto) {
        return store.createStage(dto);
    }

    @PutMapping("/stages/{id}")
    public StageDto updateStage(@PathVariable Long id, @Valid @RequestBody StageDto dto) {
        return store.updateStage(id, dto);
    }

    @GetMapping("/requirements")
    public List<RequirementSetDto> listRequirements(
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) Long stageId
    ) {
        return store.listRequirements(speciesId, stageId);
    }

    @GetMapping("/requirements/{id}")
    public RequirementSetDto getRequirement(@PathVariable Long id) {
        return store.getRequirement(id);
    }

    @PostMapping("/requirements")
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementSetDto createRequirement(@Valid @RequestBody RequirementSetDto dto) {
        return store.createRequirement(dto);
    }

    @PutMapping("/requirements/{id}")
    public RequirementSetDto updateRequirement(@PathVariable Long id, @Valid @RequestBody RequirementSetDto dto) {
        return store.updateRequirement(id, dto);
    }

    @DeleteMapping("/requirements/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRequirement(@PathVariable Long id) {
        store.deleteRequirement(id);
    }

    @GetMapping("/formulas")
    public List<FormulaDto> listFormulas(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) String status
    ) {
        return store.listFormulas(clientId, speciesId, stageId, status);
    }

    @GetMapping("/formulas/{id}")
    public FormulaDto getFormula(@PathVariable Long id) {
        return store.getFormula(id);
    }

    @PostMapping("/formulas")
    @ResponseStatus(HttpStatus.CREATED)
    public FormulaDto createFormula(@RequestBody FormulaDto dto) {
        return store.createFormula(dto);
    }

    @GetMapping("/client-formulas-hdr")
    public List<ClientFormulasHdr> listClientFormulasHdr(@RequestParam(required = false) Long clientId) {
        return store.listClientFormulasHdr(clientId);
    }

    @GetMapping("/client-formulas-hdr/{id}")
    public ClientFormulasHdr getClientFormulasHdr(@PathVariable Long id) {
        return store.getClientFormulasHdr(id);
    }
}
