package com.iah.nutrition.web;

import com.iah.nutrition.dto.ClientFormulasHdr;
import com.iah.nutrition.dto.FormulaDto;
import com.iah.nutrition.dto.OptimizationRequest;
import com.iah.nutrition.service.OptimizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OptimizationController {

    private final OptimizationService optimizationService;

    public OptimizationController(OptimizationService optimizationService) {
        this.optimizationService = optimizationService;
    }

    @PostMapping("/optimize")
    public FormulaDto optimize(@Valid @RequestBody OptimizationRequest request) {
        return optimizationService.optimize(request);
    }

    @PostMapping("/optimize/linear")
    public FormulaDto optimizeLinear(@Valid @RequestBody OptimizationRequest request) {
        return optimizationService.optimize(withType(request, "LINEAR"));
    }

    @PostMapping("/optimize/nonlinear")
    public FormulaDto optimizeNonlinear(@Valid @RequestBody OptimizationRequest request) {
        return optimizationService.optimize(withType(request, "NONLINEAR"));
    }

    @GetMapping("/formulas")
    public List<FormulaDto> listFormulas(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) String status
    ) {
        return optimizationService.listFormulas(clientId, speciesId, stageId, status);
    }

    @GetMapping("/formulas/{id}")
    public FormulaDto getFormula(@PathVariable Long id) {
        return optimizationService.findFormula(id);
    }

    @PostMapping("/formulas")
    @ResponseStatus(HttpStatus.CREATED)
    public FormulaDto createFormula(@RequestBody FormulaDto dto) {
        return optimizationService.createFormula(dto);
    }

    @GetMapping("/client-formulas-hdr")
    public List<ClientFormulasHdr> listClientFormulasHdr(@RequestParam(required = false) Long clientId) {
        return optimizationService.listClientFormulasHdr(clientId);
    }

    @GetMapping("/client-formulas-hdr/{id}")
    public ClientFormulasHdr getClientFormulasHdr(@PathVariable Long id) {
        return optimizationService.findClientFormulasHdr(id);
    }

    private OptimizationRequest withType(OptimizationRequest request, String type) {
        return new OptimizationRequest(
                request.clientId(),
                request.speciesId(),
                request.stageId(),
                request.requirementSetId(),
                request.animalAgeDays(),
                request.sex(),
                request.breed(),
                request.productionLevel(),
                request.housing(),
                request.environment(),
                request.activityLevel(),
                request.pregnancyStatus(),
                request.batchWeightKg(),
                type,
                request.ingredientIds(),
                request.ingredientBounds(),
                request.calciumPhosphorusTarget(),
                request.saveFormula(),
                request.formulaName(),
                request.formulaCode(),
                request.notes()
        );
    }
}
