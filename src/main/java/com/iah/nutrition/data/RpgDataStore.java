package com.iah.nutrition.data;

import com.iah.nutrition.dto.ClientDto;
import com.iah.nutrition.dto.FormulaDto;
import com.iah.nutrition.dto.IngredNutDto;
import com.iah.nutrition.dto.IngredientDto;
import com.iah.nutrition.dto.IngredientNutrientDto;
import com.iah.nutrition.dto.IngredientPriceDto;
import com.iah.nutrition.dto.NutrientDto;
import com.iah.nutrition.dto.RequirementLineDto;
import com.iah.nutrition.dto.RequirementSetDto;
import com.iah.nutrition.dto.SpeciesDto;
import com.iah.nutrition.dto.StageDto;
import com.iah.nutrition.exception.BusinessException;
import com.iah.nutrition.exception.NotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-process stand-in for the ILE RPG + Db2 data service. Used only with the
 * local profile so Java can call the same REST contract without IBM i.
 */
@Component
@Profile("local")
public class RpgDataStore {

    private final AtomicLong nutrientSeq = new AtomicLong();
    private final AtomicLong ingredientSeq = new AtomicLong();
    private final AtomicLong priceSeq = new AtomicLong();
    private final AtomicLong clientSeq = new AtomicLong();
    private final AtomicLong speciesSeq = new AtomicLong();
    private final AtomicLong stageSeq = new AtomicLong();
    private final AtomicLong requirementSeq = new AtomicLong();
    private final AtomicLong requirementLineSeq = new AtomicLong();
    private final AtomicLong formulaSeq = new AtomicLong();

    private final Map<Long, NutrientDto> nutrients = new LinkedHashMap<>();
    private final Map<Long, IngredientDto> ingredients = new LinkedHashMap<>();
    private final Map<Long, ClientDto> clients = new LinkedHashMap<>();
    private final Map<Long, SpeciesDto> species = new LinkedHashMap<>();
    private final Map<Long, StageDto> stages = new LinkedHashMap<>();
    private final Map<Long, RequirementSetDto> requirements = new LinkedHashMap<>();
    private final Map<Long, FormulaDto> formulas = new LinkedHashMap<>();

    public synchronized List<NutrientDto> listNutrients() {
        return List.copyOf(nutrients.values());
    }

    public synchronized NutrientDto getNutrient(Long id) {
        NutrientDto dto = nutrients.get(id);
        if (dto == null) {
            throw new NotFoundException("Nutrient not found: " + id);
        }
        return dto;
    }

    public synchronized NutrientDto createNutrient(NutrientDto dto) {
        String code = upper(dto.code());
        assertUniqueNutrientCode(code, null);
        NutrientDto saved = new NutrientDto(nutrientSeq.incrementAndGet(), code, dto.name().trim(),
                dto.unit().trim(), dto.category(), dto.description(), active(dto.active()));
        nutrients.put(saved.id(), saved);
        for (IngredientDto ingredient : new ArrayList<>(ingredients.values())) {
            ingredients.put(ingredient.id(), copyIngredient(
                    ingredient.id(), ingredient, completeMatrix(ingredient.nutrients()), ingredient.prices()));
        }
        return saved;
    }

    public synchronized NutrientDto updateNutrient(Long id, NutrientDto dto) {
        getNutrient(id);
        String code = upper(dto.code());
        assertUniqueNutrientCode(code, id);
        NutrientDto saved = new NutrientDto(id, code, dto.name().trim(), dto.unit().trim(),
                dto.category(), dto.description(), active(dto.active()));
        nutrients.put(id, saved);
        for (IngredientDto ingredient : new ArrayList<>(ingredients.values())) {
            List<IngredientNutrientDto> rows = nvl(ingredient.nutrients()).stream()
                    .map(row -> row.nutrientId().equals(id)
                            ? new IngredientNutrientDto(saved.id(), saved.code(), saved.name(), saved.unit(), row.amount())
                            : row)
                    .toList();
            ingredients.put(ingredient.id(), copyIngredient(ingredient.id(), ingredient, rows, ingredient.prices()));
        }
        return saved;
    }

    public synchronized void deleteNutrient(Long id) {
        getNutrient(id);
        nutrients.remove(id);
        for (IngredientDto ingredient : new ArrayList<>(ingredients.values())) {
            List<IngredientNutrientDto> rows = nvl(ingredient.nutrients()).stream()
                    .filter(row -> !row.nutrientId().equals(id))
                    .toList();
            ingredients.put(ingredient.id(), copyIngredient(ingredient.id(), ingredient, rows, ingredient.prices()));
        }
    }

    public synchronized List<IngredientDto> listIngredients(boolean withComposition) {
        return ingredients.values().stream()
                .sorted(Comparator.comparing(IngredientDto::name, String.CASE_INSENSITIVE_ORDER))
                .map(item -> withComposition ? item : summary(item))
                .toList();
    }

    public synchronized IngredientDto getIngredient(Long id) {
        IngredientDto dto = ingredients.get(id);
        if (dto == null) {
            throw new NotFoundException("Ingredient not found: " + id);
        }
        return dto;
    }

    public synchronized IngredientDto createIngredient(IngredientDto dto) {
        String code = upper(dto.code());
        assertUniqueIngredientCode(code, null);
        Long id = ingredientSeq.incrementAndGet();
        IngredientDto saved = copyIngredient(id, dto, completeMatrix(enrichNutrients(dto.nutrients())), dto.prices());
        ingredients.put(id, saved);
        return saved;
    }

    public synchronized IngredientDto updateIngredient(Long id, IngredientDto dto) {
        IngredientDto existing = getIngredient(id);
        String code = upper(dto.code());
        assertUniqueIngredientCode(code, id);
        List<IngredientNutrientDto> composition = dto.nutrients() != null
                ? overlayMatrix(existing.nutrients(), enrichNutrients(dto.nutrients()))
                : completeMatrix(existing.nutrients());
        IngredientDto saved = copyIngredient(id, dto, composition, existing.prices());
        ingredients.put(id, saved);
        return saved;
    }

    public synchronized void deleteIngredient(Long id) {
        getIngredient(id);
        ingredients.remove(id);
    }

    public synchronized IngredientDto upsertIngredientNutrient(Long ingredientId, IngredientNutrientDto dto) {
        IngredientDto ingredient = getIngredient(ingredientId);
        NutrientDto nutrient = getNutrient(dto.nutrientId());
        List<IngredientNutrientDto> rows = overlayMatrix(
                completeMatrix(ingredient.nutrients()),
                List.of(new IngredientNutrientDto(nutrient.id(), nutrient.code(), nutrient.name(), nutrient.unit(), dto.amount()))
        );
        IngredientDto saved = copyIngredient(ingredient.id(), ingredient, rows, ingredient.prices());
        ingredients.put(ingredientId, saved);
        return saved;
    }

    public synchronized void deleteIngredientNutrient(Long ingredientId, Long nutrientId) {
        IngredientDto ingredient = getIngredient(ingredientId);
        List<IngredientNutrientDto> rows = completeMatrix(ingredient.nutrients()).stream()
                .map(row -> row.nutrientId().equals(nutrientId)
                        ? new IngredientNutrientDto(row.nutrientId(), row.nutrientCode(), row.nutrientName(),
                        row.unit(), BigDecimal.ZERO)
                        : row)
                .toList();
        ingredients.put(ingredientId, copyIngredient(ingredient.id(), ingredient, rows, ingredient.prices()));
    }

    public synchronized List<IngredNutDto> listIngredNut(Long ingredientId) {
        return ingredients.values().stream()
                .filter(ingredient -> ingredientId == null || ingredientId == 0 || ingredient.id().equals(ingredientId))
                .sorted(Comparator.comparing(IngredientDto::code, String.CASE_INSENSITIVE_ORDER))
                .flatMap(ingredient -> completeMatrix(ingredient.nutrients()).stream()
                        .map(row -> new IngredNutDto(
                                ingredient.id(),
                                ingredient.code(),
                                row.nutrientId(),
                                row.nutrientCode(),
                                row.nutrientName(),
                                row.unit(),
                                row.amount()
                        )))
                .toList();
    }

    public synchronized IngredientPriceDto addIngredientPrice(Long ingredientId, IngredientPriceDto dto) {
        IngredientDto ingredient = getIngredient(ingredientId);
        IngredientPriceDto saved = new IngredientPriceDto(
                priceSeq.incrementAndGet(),
                dto.price(),
                dto.priceUnit() == null ? ingredient.priceUnit() : dto.priceUnit(),
                dto.effectiveDate() == null ? LocalDate.now() : dto.effectiveDate(),
                dto.source(),
                dto.notes()
        );
        List<IngredientPriceDto> prices = new ArrayList<>(nvl(ingredient.prices()));
        prices.add(0, saved);
        IngredientDto updated = new IngredientDto(
                ingredient.id(), ingredient.code(), ingredient.name(), ingredient.category(),
                saved.price(), saved.priceUnit(), ingredient.dryMatterPct(), ingredient.density(),
                ingredient.minInclusion(), ingredient.maxInclusion(), ingredient.quadraticCost(),
                ingredient.supplier(), ingredient.notes(), ingredient.active(),
                ingredient.nutrients(), prices
        );
        ingredients.put(ingredientId, updated);
        return saved;
    }

    public synchronized List<ClientDto> listClients() {
        return List.copyOf(clients.values());
    }

    public synchronized ClientDto getClient(Long id) {
        ClientDto dto = clients.get(id);
        if (dto == null) {
            throw new NotFoundException("Client not found: " + id);
        }
        return dto;
    }

    public synchronized ClientDto createClient(ClientDto dto) {
        String code = upper(dto.code());
        assertUniqueClientCode(code, null);
        ClientDto saved = copyClient(clientSeq.incrementAndGet(), dto, code);
        clients.put(saved.id(), saved);
        return saved;
    }

    public synchronized ClientDto updateClient(Long id, ClientDto dto) {
        getClient(id);
        String code = upper(dto.code());
        assertUniqueClientCode(code, id);
        ClientDto saved = copyClient(id, dto, code);
        clients.put(id, saved);
        return saved;
    }

    public synchronized void deleteClient(Long id) {
        getClient(id);
        clients.remove(id);
    }

    public synchronized List<SpeciesDto> listSpecies() {
        return List.copyOf(species.values());
    }

    public synchronized SpeciesDto getSpecies(Long id) {
        SpeciesDto dto = species.get(id);
        if (dto == null) {
            throw new NotFoundException("Species not found: " + id);
        }
        return dto;
    }

    public synchronized SpeciesDto createSpecies(SpeciesDto dto) {
        SpeciesDto saved = new SpeciesDto(speciesSeq.incrementAndGet(), upper(dto.code()), dto.name().trim(),
                dto.description(), active(dto.active()));
        species.put(saved.id(), saved);
        return saved;
    }

    public synchronized SpeciesDto updateSpecies(Long id, SpeciesDto dto) {
        getSpecies(id);
        SpeciesDto saved = new SpeciesDto(id, upper(dto.code()), dto.name().trim(), dto.description(), active(dto.active()));
        species.put(id, saved);
        return saved;
    }

    public synchronized List<StageDto> listStages(Long speciesId) {
        getSpecies(speciesId);
        return stages.values().stream()
                .filter(stage -> stage.speciesId().equals(speciesId))
                .sorted(Comparator.comparing(stage -> stage.ageMinDays() == null ? 0 : stage.ageMinDays()))
                .toList();
    }

    public synchronized StageDto getStage(Long id) {
        StageDto dto = stages.get(id);
        if (dto == null) {
            throw new NotFoundException("Animal stage/age group not found: " + id);
        }
        return dto;
    }

    public synchronized StageDto createStage(StageDto dto) {
        SpeciesDto speciesDto = getSpecies(dto.speciesId());
        StageDto saved = copyStage(stageSeq.incrementAndGet(), dto, speciesDto.code());
        stages.put(saved.id(), saved);
        return saved;
    }

    public synchronized StageDto updateStage(Long id, StageDto dto) {
        getStage(id);
        SpeciesDto speciesDto = getSpecies(dto.speciesId());
        StageDto saved = copyStage(id, dto, speciesDto.code());
        stages.put(id, saved);
        return saved;
    }

    public synchronized List<RequirementSetDto> listRequirements(Long speciesId, Long stageId) {
        return requirements.values().stream()
                .filter(set -> speciesId == null || set.speciesId().equals(speciesId))
                .filter(set -> stageId == null || set.stageId().equals(stageId))
                .toList();
    }

    public synchronized RequirementSetDto getRequirement(Long id) {
        RequirementSetDto dto = requirements.get(id);
        if (dto == null) {
            throw new NotFoundException("Requirement set not found: " + id);
        }
        return dto;
    }

    public synchronized RequirementSetDto createRequirement(RequirementSetDto dto) {
        RequirementSetDto saved = copyRequirement(requirementSeq.incrementAndGet(), dto);
        requirements.put(saved.id(), saved);
        return saved;
    }

    public synchronized RequirementSetDto updateRequirement(Long id, RequirementSetDto dto) {
        getRequirement(id);
        RequirementSetDto saved = copyRequirement(id, dto);
        requirements.put(id, saved);
        return saved;
    }

    public synchronized void deleteRequirement(Long id) {
        getRequirement(id);
        requirements.remove(id);
    }

    public synchronized List<FormulaDto> listFormulas(Long clientId, Long speciesId, Long stageId) {
        return formulas.values().stream()
                .filter(formula -> clientId == null || Objects.equals(formula.clientId(), clientId))
                .filter(formula -> speciesId == null || Objects.equals(formula.speciesId(), speciesId))
                .filter(formula -> stageId == null || Objects.equals(formula.stageId(), stageId))
                .sorted(Comparator.comparing(FormulaDto::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::summary)
                .toList();
    }

    public synchronized FormulaDto getFormula(Long id) {
        FormulaDto dto = formulas.get(id);
        if (dto == null) {
            throw new NotFoundException("Formula not found: " + id);
        }
        return dto;
    }

    public synchronized FormulaDto createFormula(FormulaDto dto) {
        Long id = formulaSeq.incrementAndGet();
        String clientName = dto.clientId() == null ? dto.clientName() : getClient(dto.clientId()).name();
        SpeciesDto speciesDto = getSpecies(dto.speciesId());
        StageDto stageDto = getStage(dto.stageId());
        FormulaDto saved = new FormulaDto(
                id, dto.code(), dto.name(), dto.clientId(), clientName,
                speciesDto.id(), speciesDto.code(), stageDto.id(), stageDto.code(),
                dto.animalAgeDays(), dto.sex(), dto.breed(), dto.productionLevel(),
                dto.housing(), dto.environment(), dto.batchWeightKg(), dto.totalCost(),
                dto.costPerKg(), dto.optimizationType(), dto.solverStatus(), dto.objectiveValue(),
                dto.notes(), dto.createdAt() == null ? Instant.now() : dto.createdAt(),
                dto.ingredients(), dto.nutrients()
        );
        formulas.put(id, saved);
        return saved;
    }

    public synchronized void seedIfEmpty() {
        if (!nutrients.isEmpty()) {
            return;
        }
        Map<String, NutrientDto> n = new LinkedHashMap<>();
        n.put("DM", createNutrient(new NutrientDto(null, "DM", "Dry Matter", "%", "PROXIMATE", null, true)));
        n.put("CP", createNutrient(new NutrientDto(null, "CP", "Crude Protein", "%", "PROTEIN", null, true)));
        n.put("ME", createNutrient(new NutrientDto(null, "ME", "Metabolizable Energy", "kcal/kg", "ENERGY", null, true)));
        n.put("NEL", createNutrient(new NutrientDto(null, "NEL", "Net Energy Lactation", "Mcal/kg", "ENERGY", null, true)));
        n.put("NDF", createNutrient(new NutrientDto(null, "NDF", "Neutral Detergent Fiber", "%", "FIBER", null, true)));
        n.put("ADF", createNutrient(new NutrientDto(null, "ADF", "Acid Detergent Fiber", "%", "FIBER", null, true)));
        n.put("EE", createNutrient(new NutrientDto(null, "EE", "Ether Extract (Fat)", "%", "FAT", null, true)));
        n.put("CA", createNutrient(new NutrientDto(null, "CA", "Calcium", "%", "MINERAL", null, true)));
        n.put("P", createNutrient(new NutrientDto(null, "P", "Phosphorus", "%", "MINERAL", null, true)));
        n.put("NA", createNutrient(new NutrientDto(null, "NA", "Sodium", "%", "MINERAL", null, true)));
        n.put("SALT", createNutrient(new NutrientDto(null, "SALT", "Salt", "%", "MINERAL", null, true)));
        n.put("LYS", createNutrient(new NutrientDto(null, "LYS", "Lysine", "%", "AMINO_ACID", null, true)));
        n.put("MET", createNutrient(new NutrientDto(null, "MET", "Methionine", "%", "AMINO_ACID", null, true)));

        SpeciesDto cattle = createSpecies(new SpeciesDto(null, "CATTLE", "Cattle", "Dairy and beef cattle", true));
        SpeciesDto swine = createSpecies(new SpeciesDto(null, "SWINE", "Swine", "Pigs", true));
        SpeciesDto poultry = createSpecies(new SpeciesDto(null, "POULTRY", "Poultry", "Broilers, layers, turkeys", true));
        createSpecies(new SpeciesDto(null, "EQUINE", "Equine", "Horses", true));
        createSpecies(new SpeciesDto(null, "SHEEP", "Sheep", "Sheep", true));
        createSpecies(new SpeciesDto(null, "GOAT", "Goat", "Goats", true));

        StageDto lactating = createStage(stage(cattle, "LACTATING", "Lactating Cow", 700, 4000, 500, 800));
        createStage(stage(cattle, "CALF", "Calf", 0, 90, 40, 120));
        StageDto grower = createStage(stage(swine, "GROWER", "Grower", 71, 120, 25, 60));
        StageDto broiler = createStage(stage(poultry, "BROILER_GR", "Broiler Grower", 15, 28, 0.45, 1.5));

        createClient(new ClientDto(null, "DEMO01", "Demo Dairy LLC", "Maria Lopez", null, null, null,
                "Ames", "IA", null, "USA", null, true));
        createClient(new ClientDto(null, "DEMO02", "Prairie Swine Co", "James Chen", null, null, null,
                "Sioux Falls", "SD", null, "USA", null, true));

        seedIngredient(n, "CORN", "Corn grain", "ENERGY", 0.22, 0, 0.70, 0.02,
                Map.of("DM", 86, "CP", 8.3, "ME", 3350, "NEL", 1.98, "NDF", 9.5, "EE", 3.6, "CA", 0.03, "P", 0.28, "LYS", 0.24, "MET", 0.17));
        seedIngredient(n, "SBM48", "Soybean meal 48%", "PROTEIN", 0.48, 0, 0.40, 0.04,
                Map.of("DM", 89, "CP", 47.5, "ME", 2450, "NEL", 2.10, "NDF", 9.8, "EE", 1.5, "CA", 0.35, "P", 0.69, "LYS", 2.96, "MET", 0.67));
        seedIngredient(n, "WHEATBR", "Wheat bran", "FIBER", 0.18, 0, 0.25, 0.01,
                Map.of("DM", 89, "CP", 15.5, "ME", 2300, "NEL", 1.54, "NDF", 42, "EE", 4.0, "CA", 0.13, "P", 1.18, "LYS", 0.64));
        seedIngredient(n, "ALFALFA", "Alfalfa hay", "FORAGE", 0.26, 0, 0.50, 0.01,
                Map.of("DM", 90, "CP", 18.0, "ME", 2100, "NEL", 1.30, "NDF", 42, "CA", 1.40, "P", 0.24));
        seedIngredient(n, "CORNSIL", "Corn silage", "FORAGE", 0.08, 0, 0.55, 0.005,
                Map.of("DM", 35, "CP", 8.5, "ME", 1050, "NEL", 0.72, "NDF", 45, "CA", 0.25, "P", 0.22));
        seedIngredient(n, "SOYOIL", "Soybean oil", "FAT", 1.10, 0, 0.06, 0.20,
                Map.of("DM", 99, "ME", 8800, "NEL", 5.65, "EE", 99));
        seedIngredient(n, "LIMEST", "Limestone", "MINERAL", 0.06, 0, 0.04, 0,
                Map.of("DM", 99, "CA", 38, "P", 0.02));
        seedIngredient(n, "DICAL", "Dicalcium phosphate", "MINERAL", 0.55, 0, 0.03, 0,
                Map.of("DM", 96, "CA", 22, "P", 19.3));
        seedIngredient(n, "SALT", "Salt", "MINERAL", 0.12, 0, 0.02, 0,
                Map.of("DM", 99, "NA", 39.3, "SALT", 100));
        seedIngredient(n, "PREMIX", "Vitamin/mineral premix", "PREMIX", 2.40, 0.002, 0.01, 0, Map.of("DM", 98));
        seedIngredient(n, "BARLEY", "Barley grain", "ENERGY", 0.24, 0, 0.40, 0.02,
                Map.of("DM", 88, "CP", 11.5, "ME", 3050, "NEL", 1.86, "NDF", 18, "CA", 0.06, "P", 0.35, "LYS", 0.40));
        seedIngredient(n, "FISHML", "Fish meal", "PROTEIN", 1.35, 0, 0.08, 0.10,
                Map.of("DM", 92, "CP", 62, "ME", 3100, "EE", 9, "CA", 5, "P", 3, "LYS", 4.80, "MET", 1.70));

        createRequirement(req(cattle, lactating, "Dairy lactating high, Holstein, free stall, temperate",
                "F", "Holstein", "HIGH", bd(40), "kg milk/d", bd(650), "FREE_STALL", "TEMPERATE",
                List.of(line(n, "CP", 16.5, 18.5, 17.5), line(n, "NEL", 1.55, 1.75, 1.65),
                        line(n, "NDF", 28, 40, 32), line(n, "CA", 0.65, 1.10, 0.80),
                        line(n, "P", 0.35, 0.55, 0.40), line(n, "EE", 3.0, 6.5, 4.5))));
        createRequirement(req(swine, grower, "Swine grower mixed, confinement, temperate",
                "MIXED", "Commercial", "MEDIUM", null, null, bd(40), "CONFINEMENT", "TEMPERATE",
                List.of(line(n, "CP", 16, 20, 18), line(n, "ME", 3200, 3400, 3300),
                        line(n, "LYS", 0.95, 1.20, 1.05), line(n, "CA", 0.60, 0.90, 0.70),
                        line(n, "P", 0.50, 0.70, 0.55))));
        createRequirement(req(poultry, broiler, "Broiler grower mixed, confinement, temperate",
                "MIXED", "Commercial", "HIGH", null, null, null, "CONFINEMENT", "TEMPERATE",
                List.of(line(n, "CP", 19, 22, 20.5), line(n, "ME", 3000, 3200, 3100),
                        line(n, "LYS", 1.05, 1.30, 1.15), line(n, "MET", 0.45, 0.60, 0.50),
                        line(n, "CA", 0.80, 1.10, 0.90), line(n, "P", 0.40, 0.65, 0.45))));
    }

    private void seedIngredient(
            Map<String, NutrientDto> nutrientMap,
            String code,
            String name,
            String category,
            double price,
            double min,
            double max,
            double quadratic,
            Map<String, Number> composition
    ) {
        List<IngredientNutrientDto> rows = composition.entrySet().stream()
                .map(entry -> {
                    NutrientDto nutrient = nutrientMap.get(entry.getKey());
                    return new IngredientNutrientDto(nutrient.id(), nutrient.code(), nutrient.name(),
                            nutrient.unit(), BigDecimal.valueOf(entry.getValue().doubleValue()));
                })
                .toList();
        createIngredient(new IngredientDto(null, code, name, category, BigDecimal.valueOf(price), "USD/kg",
                null, null, BigDecimal.valueOf(min), BigDecimal.valueOf(max), BigDecimal.valueOf(quadratic),
                null, null, true, rows, List.of()));
    }

    private StageDto stage(SpeciesDto speciesDto, String code, String name, int ageMin, int ageMax, double wMin, double wMax) {
        return new StageDto(null, speciesDto.id(), speciesDto.code(), code, name, ageMin, ageMax,
                bd(wMin), bd(wMax), null, true);
    }

    private RequirementSetDto req(
            SpeciesDto speciesDto, StageDto stage, String name, String sex, String breed, String production,
            BigDecimal productionValue, String productionUnit, BigDecimal bodyWeight, String housing, String environment,
            List<RequirementLineDto> lines
    ) {
        return new RequirementSetDto(null, speciesDto.id(), stage.id(), speciesDto.code(), stage.code(), name,
                sex, breed, production, productionValue, productionUnit, bodyWeight, null, housing, environment,
                null, "NRC-like demo", null, true, lines);
    }

    private RequirementLineDto line(Map<String, NutrientDto> nutrientMap, String code, double min, double max, double target) {
        NutrientDto nutrient = nutrientMap.get(code);
        return new RequirementLineDto(requirementLineSeq.incrementAndGet(), nutrient.id(), nutrient.code(),
                nutrient.name(), nutrient.unit(), bd(min), bd(max), bd(target));
    }

    private RequirementSetDto copyRequirement(Long id, RequirementSetDto dto) {
        SpeciesDto speciesDto = getSpecies(dto.speciesId());
        StageDto stage = getStage(dto.stageId());
        if (!stage.speciesId().equals(speciesDto.id())) {
            throw new BusinessException("Stage does not belong to the selected species");
        }
        List<RequirementLineDto> lines = new ArrayList<>();
        if (dto.lines() != null) {
            for (RequirementLineDto line : dto.lines()) {
                NutrientDto nutrient = getNutrient(line.nutrientId());
                lines.add(new RequirementLineDto(
                        line.id() == null ? requirementLineSeq.incrementAndGet() : line.id(),
                        nutrient.id(), nutrient.code(), nutrient.name(), nutrient.unit(),
                        line.minValue(), line.maxValue(), line.targetValue()
                ));
            }
        }
        return new RequirementSetDto(
                id, speciesDto.id(), stage.id(), speciesDto.code(), stage.code(), dto.name().trim(),
                normalize(dto.sex()), dto.breed(), normalize(dto.productionLevel()), dto.productionValue(),
                dto.productionUnit(), dto.bodyWeightKg(), normalize(dto.pregnancyStatus()),
                normalize(dto.housing()), normalize(dto.environment()), normalize(dto.activityLevel()),
                dto.source(), dto.notes(), active(dto.active()), lines
        );
    }

    private IngredientDto copyIngredient(
            Long id,
            IngredientDto dto,
            List<IngredientNutrientDto> nutrients,
            List<IngredientPriceDto> prices
    ) {
        return new IngredientDto(
                id, upper(dto.code()), dto.name().trim(), dto.category(),
                dto.price() == null ? BigDecimal.ZERO : dto.price(),
                dto.priceUnit() == null ? "USD/kg" : dto.priceUnit(),
                dto.dryMatterPct(), dto.density(),
                dto.minInclusion() == null ? BigDecimal.ZERO : dto.minInclusion(),
                dto.maxInclusion() == null ? BigDecimal.ONE : dto.maxInclusion(),
                dto.quadraticCost() == null ? BigDecimal.ZERO : dto.quadraticCost(),
                dto.supplier(), dto.notes(), active(dto.active()),
                nutrients, prices
        );
    }

    private List<IngredientNutrientDto> overlayMatrix(
            List<IngredientNutrientDto> existing,
            List<IngredientNutrientDto> updates
    ) {
        Map<Long, IngredientNutrientDto> byNutrient = new LinkedHashMap<>();
        for (IngredientNutrientDto row : completeMatrix(existing)) {
            byNutrient.put(row.nutrientId(), row);
        }
        for (IngredientNutrientDto row : nvl(updates)) {
            byNutrient.put(row.nutrientId(), row);
        }
        return completeMatrix(new ArrayList<>(byNutrient.values()));
    }

    private List<IngredientNutrientDto> completeMatrix(List<IngredientNutrientDto> provided) {
        Map<Long, BigDecimal> amounts = new LinkedHashMap<>();
        for (IngredientNutrientDto row : nvl(provided)) {
            amounts.put(row.nutrientId(), row.amount() == null ? BigDecimal.ZERO : row.amount());
        }
        return nutrients.values().stream()
                .sorted(Comparator.comparing(NutrientDto::code, String.CASE_INSENSITIVE_ORDER))
                .map(nutrient -> new IngredientNutrientDto(
                        nutrient.id(),
                        nutrient.code(),
                        nutrient.name(),
                        nutrient.unit(),
                        amounts.getOrDefault(nutrient.id(), BigDecimal.ZERO)
                ))
                .toList();
    }

    private List<IngredientNutrientDto> enrichNutrients(List<IngredientNutrientDto> rows) {
        if (rows == null) {
            return List.of();
        }
        List<IngredientNutrientDto> enriched = new ArrayList<>();
        for (IngredientNutrientDto row : rows) {
            NutrientDto nutrient = getNutrient(row.nutrientId());
            enriched.add(new IngredientNutrientDto(nutrient.id(), nutrient.code(), nutrient.name(),
                    nutrient.unit(), row.amount()));
        }
        return enriched;
    }

    private ClientDto copyClient(Long id, ClientDto dto, String code) {
        return new ClientDto(id, code, dto.name().trim(), dto.contactName(), dto.phone(), dto.email(),
                dto.address(), dto.city(), dto.state(), dto.postalCode(), dto.country(), dto.notes(),
                active(dto.active()));
    }

    private StageDto copyStage(Long id, StageDto dto, String speciesCode) {
        return new StageDto(id, dto.speciesId(), speciesCode, upper(dto.code()), dto.name().trim(),
                dto.ageMinDays(), dto.ageMaxDays(), dto.weightMinKg(), dto.weightMaxKg(),
                dto.description(), active(dto.active()));
    }

    private IngredientDto summary(IngredientDto dto) {
        return new IngredientDto(dto.id(), dto.code(), dto.name(), dto.category(), dto.price(), dto.priceUnit(),
                dto.dryMatterPct(), dto.density(), dto.minInclusion(), dto.maxInclusion(), dto.quadraticCost(),
                dto.supplier(), dto.notes(), dto.active(), null, null);
    }

    private FormulaDto summary(FormulaDto dto) {
        return new FormulaDto(dto.id(), dto.code(), dto.name(), dto.clientId(), dto.clientName(),
                dto.speciesId(), dto.speciesCode(), dto.stageId(), dto.stageCode(), dto.animalAgeDays(),
                dto.sex(), dto.breed(), dto.productionLevel(), dto.housing(), dto.environment(),
                dto.batchWeightKg(), dto.totalCost(), dto.costPerKg(), dto.optimizationType(),
                dto.solverStatus(), dto.objectiveValue(), dto.notes(), dto.createdAt(), null, null);
    }

    private void assertUniqueNutrientCode(String code, Long ignoreId) {
        nutrients.values().stream()
                .filter(item -> item.code().equalsIgnoreCase(code))
                .filter(item -> ignoreId == null || !item.id().equals(ignoreId))
                .findAny()
                .ifPresent(item -> {
                    throw new BusinessException("Nutrient code already exists: " + code);
                });
    }

    private void assertUniqueIngredientCode(String code, Long ignoreId) {
        ingredients.values().stream()
                .filter(item -> item.code().equalsIgnoreCase(code))
                .filter(item -> ignoreId == null || !item.id().equals(ignoreId))
                .findAny()
                .ifPresent(item -> {
                    throw new BusinessException("Ingredient code already exists: " + code);
                });
    }

    private void assertUniqueClientCode(String code, Long ignoreId) {
        clients.values().stream()
                .filter(item -> item.code().equalsIgnoreCase(code))
                .filter(item -> ignoreId == null || !item.id().equals(ignoreId))
                .findAny()
                .ifPresent(item -> {
                    throw new BusinessException("Client code already exists: " + code);
                });
    }

    private static String upper(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean active(Boolean value) {
        return value == null || value;
    }

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }

    private static <T> List<T> nvl(List<T> value) {
        return value == null ? List.of() : value;
    }
}
