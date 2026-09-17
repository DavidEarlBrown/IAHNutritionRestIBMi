package com.iah.nutrition;

import com.iah.nutrition.dto.FormulaDto;
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
        assertNotNull(body.id());
        assertTrue(body.ingredients() != null && !body.ingredients().isEmpty());
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
        ResponseEntity<Object[]> clients = restTemplate.getForEntity("/api/clients", Object[].class);
        assertEquals(HttpStatus.OK, ingredients.getStatusCode());
        assertTrue(ingredients.getBody() != null && ingredients.getBody().length >= 8);
        assertTrue(clients.getBody() != null && clients.getBody().length >= 2);
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
