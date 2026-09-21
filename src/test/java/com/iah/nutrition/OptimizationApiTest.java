package com.iah.nutrition;

import com.iah.nutrition.dto.ClientDto;
import com.iah.nutrition.dto.ClientFormulasHdr;
import com.iah.nutrition.dto.FormulaDto;
import com.iah.nutrition.dto.FormulaIngredientDto;
import com.iah.nutrition.dto.IngredNutDto;
import com.iah.nutrition.dto.IngredientDto;
import com.iah.nutrition.dto.NutrientDto;
import com.iah.nutrition.dto.OptimizationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class OptimizationApiTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void linearOptimizeSavesDairyFormula() {
        OptimizationRequest request = new OptimizationRequest(
                1L,
                null,
                null,
                1L,
                1200,
                "F",
                "Holstein",
                "HIGH",
                "FREE_STALL",
                "TEMPERATE",
                null,
                null,
                BigDecimal.valueOf(1000),
                "LINEAR",
                null,
                null,
                2.0,
                true,
                "Demo lactating formula",
                "DEMO-LAC-1",
                "integration test"
        );

        ResponseEntity<FormulaDto> response = restTemplate.postForEntity("/api/optimize", request, FormulaDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        FormulaDto body = response.getBody();
        assertNotNull(body);
        assertEquals("OPTIMAL", body.solverStatus());
        assertEquals("OPTIMIZED", body.status());
        assertNotNull(body.id());
        assertTrue(body.ingredients() != null && !body.ingredients().isEmpty());
        assertTrue(body.ingredients().stream().allMatch(item -> item.lastPrice() != null));
        assertTrue(body.costPerKg().doubleValue() > 0);
        double inclusion = body.ingredients().stream()
                .map(item -> item.inclusionFrac())
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
        assertEquals(1.0, inclusion, 0.02);
    }

    @Test
    void listsSeededIngredientsAndClients() {
        ResponseEntity<Object[]> ingredients = restTemplate.getForEntity("/api/ingredients", Object[].class);
        ResponseEntity<ClientDto[]> clients = restTemplate.getForEntity("/api/clients", ClientDto[].class);
        assertEquals(HttpStatus.OK, ingredients.getStatusCode());
        assertTrue(ingredients.getBody() != null && ingredients.getBody().length >= 8);
        assertNotNull(clients.getBody());
        assertTrue(clients.getBody().length >= 3);
        assertTrue(Arrays.stream(clients.getBody()).anyMatch(client ->
                "DEFAULT".equals(client.code()) && Boolean.TRUE.equals(client.isDefault())));
    }

    @Test
    void storesPrefixedFormulasOnDefaultClient() {
        ResponseEntity<FormulaDto[]> seeded = restTemplate.getForEntity(
                "/api/formulas?status=PREFIXED", FormulaDto[].class);
        assertEquals(HttpStatus.OK, seeded.getStatusCode());
        assertNotNull(seeded.getBody());
        assertTrue(Arrays.stream(seeded.getBody()).anyMatch(formula ->
                "PREFIXED".equals(formula.status()) && "PFX-LAC-1".equals(formula.code())));

        FormulaDto prefixed = new FormulaDto(
                null,
                "PFX-TEST-1",
                "Hand-built test mix",
                null,
                null,
                FormulaDto.STATUS_PREFIXED,
                1L,
                null,
                1L,
                null,
                1200,
                "F",
                "Holstein",
                "HIGH",
                "FREE_STALL",
                "TEMPERATE",
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(220),
                BigDecimal.valueOf(0.22),
                "NONE",
                "NONE",
                null,
                "created from API",
                null,
                List.of(new FormulaIngredientDto(
                        1L, null, null,
                        BigDecimal.valueOf(1), BigDecimal.valueOf(100), BigDecimal.valueOf(1000),
                        BigDecimal.valueOf(0.22), BigDecimal.valueOf(0.22), BigDecimal.valueOf(220)
                )),
                List.of()
        );

        ResponseEntity<FormulaDto> created = restTemplate.postForEntity("/api/formulas", prefixed, FormulaDto.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        FormulaDto body = created.getBody();
        assertNotNull(body);
        assertEquals("PREFIXED", body.status());
        ClientDto owner = restTemplate.getForEntity("/api/clients/" + body.clientId(), ClientDto.class).getBody();
        assertNotNull(owner);
        assertEquals("DEFAULT", owner.code());
        assertTrue(owner.isDefault());
        assertEquals(1L, body.speciesId());
        assertNotNull(body.ingredients());
        assertEquals(0, body.ingredients().get(0).lastPrice().compareTo(BigDecimal.valueOf(0.22)));

        ResponseEntity<FormulaDto[]> forDemo = restTemplate.getForEntity(
                "/api/formulas?clientId=1", FormulaDto[].class);
        assertNotNull(forDemo.getBody());
        assertTrue(Arrays.stream(forDemo.getBody()).anyMatch(formula ->
                "PFX-LAC-1".equals(formula.code()) || "PFX-TEST-1".equals(formula.code())));
    }

    @Test
    void listsClientFormulasHdr() {
        ResponseEntity<ClientFormulasHdr[]> headers = restTemplate.getForEntity(
                "/api/client-formulas-hdr", ClientFormulasHdr[].class);
        assertEquals(HttpStatus.OK, headers.getStatusCode());
        assertNotNull(headers.getBody());
        ClientFormulasHdr prefixed = Arrays.stream(headers.getBody())
                .filter(row -> "Prefixed lactating mix".equals(row.formulaDescription()))
                .findFirst()
                .orElse(null);
        assertNotNull(prefixed);
        assertNotNull(prefixed.clientId());
        assertNotNull(prefixed.formulaId());
        assertNotNull(prefixed.lastPrice());
        assertEquals(1L, prefixed.animalId());
        assertEquals("NONE", prefixed.optimizationTechnique());

        ResponseEntity<ClientFormulasHdr> one = restTemplate.getForEntity(
                "/api/client-formulas-hdr/" + prefixed.formulaId(), ClientFormulasHdr.class);
        assertEquals(HttpStatus.OK, one.getStatusCode());
        assertNotNull(one.getBody());
        assertEquals(prefixed.formulaId(), one.getBody().formulaId());
        assertEquals(prefixed.clientId(), one.getBody().clientId());
    }

    @Test
    void ingredNutHasARowForEveryIngredientAndNutrient() {
        ResponseEntity<Object[]> ingredients = restTemplate.getForEntity("/api/ingredients", Object[].class);
        ResponseEntity<NutrientDto[]> nutrients = restTemplate.getForEntity("/api/nutrients", NutrientDto[].class);
        ResponseEntity<IngredNutDto[]> matrix = restTemplate.getForEntity("/api/ingrednut", IngredNutDto[].class);
        assertEquals(HttpStatus.OK, ingredients.getStatusCode());
        assertNotNull(ingredients.getBody());
        assertNotNull(nutrients.getBody());
        assertNotNull(matrix.getBody());
        int ingredientCount = ingredients.getBody().length;
        int nutrientCount = nutrients.getBody().length;
        assertTrue(ingredientCount >= 8);
        assertTrue(nutrientCount >= 8);
        assertEquals(ingredientCount * nutrientCount, matrix.getBody().length);

        ResponseEntity<IngredientDto> corn = restTemplate.getForEntity("/api/ingredients/1", IngredientDto.class);
        assertEquals(HttpStatus.OK, corn.getStatusCode());
        assertNotNull(corn.getBody());
        assertNotNull(corn.getBody().nutrients());
        assertEquals(nutrientCount, corn.getBody().nutrients().size());

        ResponseEntity<IngredNutDto[]> oneIngredient = restTemplate.getForEntity(
                "/data/ingredients/1/ingrednut", IngredNutDto[].class);
        assertEquals(HttpStatus.OK, oneIngredient.getStatusCode());
        assertNotNull(oneIngredient.getBody());
        assertEquals(nutrientCount, oneIngredient.getBody().length);

        NutrientDto created = restTemplate.postForObject(
                "/api/nutrients",
                new NutrientDto(null, "ZN", "Zinc", "ppm", "MINERAL", null, true),
                NutrientDto.class);
        assertNotNull(created);
        ResponseEntity<IngredNutDto[]> after = restTemplate.getForEntity("/api/ingrednut", IngredNutDto[].class);
        assertNotNull(after.getBody());
        assertEquals(ingredientCount * (nutrientCount + 1), after.getBody().length);
        ResponseEntity<IngredientDto> cornAfter = restTemplate.getForEntity("/api/ingredients/1", IngredientDto.class);
        assertNotNull(cornAfter.getBody());
        assertNotNull(cornAfter.getBody().nutrients());
        assertEquals(nutrientCount + 1, cornAfter.getBody().nutrients().size());
        assertTrue(cornAfter.getBody().nutrients().stream()
                .anyMatch(row -> "ZN".equals(row.nutrientCode()) && row.amount().compareTo(BigDecimal.ZERO) == 0));
    }

    @Test
    void healthIsUp() {
        ResponseEntity<Map> health = restTemplate.getForEntity("/actuator/health", Map.class);
        assertEquals(HttpStatus.OK, health.getStatusCode());
        assertEquals("UP", health.getBody().get("status"));
    }
}
