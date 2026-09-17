package com.iah.nutrition.service;

import com.iah.nutrition.config.NutritionProperties;
import com.iah.nutrition.data.NutritionDataClient;
import com.iah.nutrition.dto.ClientDto;
import com.iah.nutrition.dto.FormulaDto;
import com.iah.nutrition.dto.FormulaIngredientDto;
import com.iah.nutrition.dto.FormulaNutrientDto;
import com.iah.nutrition.dto.IngredientDto;
import com.iah.nutrition.dto.IngredientNutrientDto;
import com.iah.nutrition.dto.OptimizationRequest;
import com.iah.nutrition.dto.RequirementLineDto;
import com.iah.nutrition.dto.RequirementSetDto;
import com.iah.nutrition.dto.SpeciesDto;
import com.iah.nutrition.dto.StageDto;
import com.iah.nutrition.exception.BusinessException;
import com.iah.nutrition.optimization.FeedOptimizer;
import com.iah.nutrition.optimization.FeedProblem;
import com.iah.nutrition.optimization.NonlinearFeedOptimizer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OptimizationService {

    private final NutritionDataClient dataClient;
    private final RequirementService requirementService;
    private final FeedOptimizer feedOptimizer;
    private final NutritionProperties properties;

    public OptimizationService(
            NutritionDataClient dataClient,
            RequirementService requirementService,
            FeedOptimizer feedOptimizer,
            NutritionProperties properties
    ) {
        this.dataClient = dataClient;
        this.requirementService = requirementService;
        this.feedOptimizer = feedOptimizer;
        this.properties = properties;
    }

    public FormulaDto optimize(OptimizationRequest request) {
        RequirementSetDto requirementSet = resolveRequirements(request);
        List<IngredientDto> ingredients = loadIngredients(request);
        FeedProblem problem = buildProblem(request, requirementSet, ingredients);
        FeedOptimizer.OptimizationOutcome outcome = feedOptimizer.solve(problem);
        FormulaDto formula = toFormula(request, requirementSet, ingredients, problem, outcome);
        if (request.saveFormula()) {
            formula = dataClient.createFormula(formula);
        }
        return formula;
    }

    public FormulaDto findFormula(Long id) {
        return dataClient.getFormula(id);
    }

    public List<FormulaDto> listFormulas(Long clientId, Long speciesId, Long stageId) {
        return dataClient.listFormulas(clientId, speciesId, stageId);
    }

    private RequirementSetDto resolveRequirements(OptimizationRequest request) {
        if (request.requirementSetId() != null) {
            return requirementService.findById(request.requirementSetId());
        }
        if (request.speciesId() == null || request.stageId() == null) {
            throw new BusinessException("speciesId and stageId, or requirementSetId, are required");
        }
        return requirementService.match(
                request.speciesId(),
                request.stageId(),
                request.sex(),
                request.breed(),
                request.productionLevel(),
                request.housing(),
                request.environment(),
                request.activityLevel(),
                request.pregnancyStatus()
        );
    }

    private List<IngredientDto> loadIngredients(OptimizationRequest request) {
        List<IngredientDto> ingredients = dataClient.listIngredients(true);
        if (request.ingredientIds() != null && !request.ingredientIds().isEmpty()) {
            ingredients = ingredients.stream()
                    .filter(item -> request.ingredientIds().contains(item.id()))
                    .toList();
        }
        if (ingredients.isEmpty()) {
            throw new BusinessException("No active ingredients available for optimization");
        }
        return ingredients;
    }

    private FeedProblem buildProblem(OptimizationRequest request, RequirementSetDto requirementSet, List<IngredientDto> ingredients) {
        FeedProblem problem = new FeedProblem();
        problem.setOptimizationType(request.optimizationType() == null ? "LINEAR" : request.optimizationType().toUpperCase(Locale.ROOT));
        problem.setBatchKg(request.batchWeightKg() == null
                ? properties.getDefaultBatchKg()
                : request.batchWeightKg().doubleValue());

        int n = ingredients.size();
        double[] price = new double[n];
        double[] quadratic = new double[n];
        double[] lower = new double[n];
        double[] upper = new double[n];
        for (int i = 0; i < n; i++) {
            IngredientDto ingredient = ingredients.get(i);
            problem.getIngredientIds().add(ingredient.id());
            problem.getIngredientCodes().add(ingredient.code());
            problem.getIngredientNames().add(ingredient.name());
            price[i] = d(ingredient.price());
            quadratic[i] = d(ingredient.quadraticCost());
            lower[i] = d(ingredient.minInclusion());
            upper[i] = d(ingredient.maxInclusion());
            if (request.ingredientBounds() != null && request.ingredientBounds().containsKey(ingredient.id())) {
                OptimizationRequest.Bound bound = request.ingredientBounds().get(ingredient.id());
                if (bound.min() != null) {
                    lower[i] = bound.min().doubleValue();
                }
                if (bound.max() != null) {
                    upper[i] = bound.max().doubleValue();
                }
            }
            if (upper[i] < lower[i]) {
                throw new BusinessException("Invalid inclusion bounds for " + ingredient.code());
            }
        }
        problem.setPrice(price);
        problem.setQuadraticCost(quadratic);
        problem.setLower(lower);
        problem.setUpper(upper);

        List<RequirementLineDto> lines = requirementSet.lines() == null ? List.of() : requirementSet.lines();
        int m = lines.size();
        double[][] matrix = new double[m][n];
        double[] min = new double[m];
        double[] max = new double[m];
        double[] target = new double[m];
        Map<String, Integer> nutrientIndex = new HashMap<>();
        for (int k = 0; k < m; k++) {
            RequirementLineDto line = lines.get(k);
            problem.getNutrientIds().add(line.nutrientId());
            problem.getNutrientCodes().add(line.nutrientCode());
            problem.getNutrientNames().add(line.nutrientName());
            problem.getNutrientUnits().add(line.unit());
            nutrientIndex.put(line.nutrientCode().toUpperCase(Locale.ROOT), k);
            min[k] = line.minValue() == null ? Double.NaN : line.minValue().doubleValue();
            max[k] = line.maxValue() == null ? Double.NaN : line.maxValue().doubleValue();
            target[k] = line.targetValue() == null ? Double.NaN : line.targetValue().doubleValue();
            for (int i = 0; i < n; i++) {
                matrix[k][i] = amountOf(ingredients.get(i), line.nutrientId());
            }
        }
        problem.setMatrix(matrix);
        problem.setMin(min);
        problem.setMax(max);
        problem.setTarget(target);

        if (request.calciumPhosphorusTarget() != null
                && nutrientIndex.containsKey("CA")
                && nutrientIndex.containsKey("P")) {
            problem.getRatios().add(new NonlinearFeedOptimizer.RatioTarget(
                    nutrientIndex.get("CA"),
                    nutrientIndex.get("P"),
                    request.calciumPhosphorusTarget()
            ));
        }
        return problem;
    }

    private FormulaDto toFormula(
            OptimizationRequest request,
            RequirementSetDto requirementSet,
            List<IngredientDto> ingredients,
            FeedProblem problem,
            FeedOptimizer.OptimizationOutcome outcome
    ) {
        String clientName = null;
        if (request.clientId() != null) {
            ClientDto client = dataClient.getClient(request.clientId());
            clientName = client.name();
        }
        SpeciesDto species = dataClient.getSpecies(requirementSet.speciesId());
        StageDto stage = dataClient.getStage(requirementSet.stageId());
        BigDecimal batch = BigDecimal.valueOf(problem.getBatchKg());
        double[] x = outcome.inclusion();
        BigDecimal totalCost = BigDecimal.ZERO;
        List<FormulaIngredientDto> formulaIngredients = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); i++) {
            if (x[i] < 1e-6) {
                continue;
            }
            IngredientDto ingredient = ingredients.get(i);
            BigDecimal frac = BigDecimal.valueOf(x[i]);
            BigDecimal amount = frac.multiply(batch);
            BigDecimal price = ingredient.price();
            BigDecimal cost = amount.multiply(price);
            totalCost = totalCost.add(cost);
            formulaIngredients.add(new FormulaIngredientDto(
                    ingredient.id(), ingredient.code(), ingredient.name(),
                    frac, frac.multiply(BigDecimal.valueOf(100)), amount, price, cost
            ));
        }

        List<FormulaNutrientDto> formulaNutrients = new ArrayList<>();
        List<RequirementLineDto> lines = requirementSet.lines() == null ? List.of() : requirementSet.lines();
        for (int k = 0; k < problem.nutrientCount(); k++) {
            double achieved = 0.0;
            for (int i = 0; i < ingredients.size(); i++) {
                achieved += problem.getMatrix()[k][i] * x[i];
            }
            RequirementLineDto line = lines.get(k);
            formulaNutrients.add(new FormulaNutrientDto(
                    line.nutrientId(), line.nutrientCode(), line.nutrientName(), line.unit(),
                    BigDecimal.valueOf(achieved), nanToNull(problem.getMin()[k]),
                    nanToNull(problem.getMax()[k]), nanToNull(problem.getTarget()[k])
            ));
        }

        String name = request.formulaName() == null || request.formulaName().isBlank()
                ? species.code() + " " + stage.code() + " " + problem.getOptimizationType() + " formula"
                : request.formulaName();

        return new FormulaDto(
                null,
                request.formulaCode(),
                name,
                request.clientId(),
                clientName,
                species.id(),
                species.code(),
                stage.id(),
                stage.code(),
                request.animalAgeDays() != null
                        ? request.animalAgeDays()
                        : midpoint(stage.ageMinDays(), stage.ageMaxDays()),
                requirementSet.sex(),
                request.breed() != null ? request.breed() : requirementSet.breed(),
                request.productionLevel() != null ? request.productionLevel() : requirementSet.productionLevel(),
                request.housing() != null ? request.housing() : requirementSet.housing(),
                request.environment() != null ? request.environment() : requirementSet.environment(),
                batch,
                totalCost.setScale(6, RoundingMode.HALF_UP),
                batch.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : totalCost.divide(batch, 6, RoundingMode.HALF_UP),
                outcome.type(),
                outcome.status(),
                Double.isFinite(outcome.objective())
                        ? BigDecimal.valueOf(outcome.objective()).setScale(8, RoundingMode.HALF_UP)
                        : null,
                request.notes(),
                Instant.now(),
                formulaIngredients,
                formulaNutrients
        );
    }

    private double amountOf(IngredientDto ingredient, Long nutrientId) {
        if (ingredient.nutrients() == null) {
            return 0.0;
        }
        return ingredient.nutrients().stream()
                .filter(row -> row.nutrientId().equals(nutrientId))
                .map(IngredientNutrientDto::amount)
                .mapToDouble(this::d)
                .findFirst()
                .orElse(0.0);
    }

    private double d(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private BigDecimal nanToNull(double value) {
        return Double.isNaN(value) ? null : BigDecimal.valueOf(value);
    }

    private Integer midpoint(Integer min, Integer max) {
        if (min == null && max == null) {
            return null;
        }
        if (min == null) {
            return max;
        }
        if (max == null) {
            return min;
        }
        return (min + max) / 2;
    }
}
