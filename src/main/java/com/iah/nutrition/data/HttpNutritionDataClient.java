package com.iah.nutrition.data;

import com.iah.nutrition.config.NutritionProperties;
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
import com.iah.nutrition.exception.BusinessException;
import com.iah.nutrition.exception.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class HttpNutritionDataClient implements NutritionDataClient {

    private static final ParameterizedTypeReference<List<NutrientDto>> NUTRIENTS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<IngredientDto>> INGREDIENTS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<IngredNutDto>> INGREDNUT = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<ClientDto>> CLIENTS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<SpeciesDto>> SPECIES = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<StageDto>> STAGES = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<RequirementSetDto>> REQUIREMENTS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<FormulaDto>> FORMULAS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<ClientFormulasHdr>> FORMULA_HDRS = new ParameterizedTypeReference<>() { };

    private final RestClient.Builder builder;
    private final NutritionProperties properties;
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private volatile RestClient restClient;

    public HttpNutritionDataClient(
            @Qualifier("nutritionRestClientBuilder") RestClient.Builder builder,
            NutritionProperties properties,
            Environment environment,
            ObjectMapper objectMapper
    ) {
        this.builder = builder;
        this.properties = properties;
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<NutrientDto> listNutrients() {
        return client().get().uri("/nutrients").retrieve().body(NUTRIENTS);
    }

    @Override
    public NutrientDto getNutrient(Long id) {
        return client().get().uri("/nutrients/{id}", id).retrieve().body(NutrientDto.class);
    }

    @Override
    public NutrientDto createNutrient(NutrientDto dto) {
        return post("/nutrients", dto, NutrientDto.class);
    }

    @Override
    public NutrientDto updateNutrient(Long id, NutrientDto dto) {
        return put("/nutrients/{id}", dto, NutrientDto.class, id);
    }

    @Override
    public void deleteNutrient(Long id) {
        client().delete().uri("/nutrients/{id}", id).retrieve().toBodilessEntity();
    }

    @Override
    public List<IngredientDto> listIngredients(boolean withComposition) {
        return client().get()
                .uri(uri -> uri.path("/ingredients").queryParam("view", withComposition ? "full" : "summary").build())
                .retrieve()
                .body(INGREDIENTS);
    }

    @Override
    public IngredientDto getIngredient(Long id) {
        return client().get().uri("/ingredients/{id}", id).retrieve().body(IngredientDto.class);
    }

    @Override
    public IngredientDto createIngredient(IngredientDto dto) {
        return post("/ingredients", dto, IngredientDto.class);
    }

    @Override
    public IngredientDto updateIngredient(Long id, IngredientDto dto) {
        return put("/ingredients/{id}", dto, IngredientDto.class, id);
    }

    @Override
    public void deleteIngredient(Long id) {
        client().delete().uri("/ingredients/{id}", id).retrieve().toBodilessEntity();
    }

    @Override
    public IngredientDto upsertIngredientNutrient(Long ingredientId, IngredientNutrientDto dto) {
        return client().put()
                .uri("/ingredients/{id}/nutrients", ingredientId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(IngredientDto.class);
    }

    @Override
    public void deleteIngredientNutrient(Long ingredientId, Long nutrientId) {
        client().delete().uri("/ingredients/{id}/nutrients/{nutrientId}", ingredientId, nutrientId)
                .retrieve().toBodilessEntity();
    }

    @Override
    public List<IngredNutDto> listIngredNut(Long ingredientId) {
        return client().get()
                .uri(uri -> {
                    var builder = uri.path("/ingrednut");
                    if (ingredientId != null && ingredientId != 0) {
                        builder.queryParam("ingredientId", ingredientId);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(INGREDNUT);
    }

    @Override
    public IngredientPriceDto addIngredientPrice(Long ingredientId, IngredientPriceDto dto) {
        return post("/ingredients/" + ingredientId + "/prices", dto, IngredientPriceDto.class);
    }

    @Override
    public List<ClientDto> listClients() {
        return client().get().uri("/clients").retrieve().body(CLIENTS);
    }

    @Override
    public ClientDto getClient(Long id) {
        return client().get().uri("/clients/{id}", id).retrieve().body(ClientDto.class);
    }

    @Override
    public ClientDto createClient(ClientDto dto) {
        return post("/clients", dto, ClientDto.class);
    }

    @Override
    public ClientDto updateClient(Long id, ClientDto dto) {
        return put("/clients/{id}", dto, ClientDto.class, id);
    }

    @Override
    public void deleteClient(Long id) {
        client().delete().uri("/clients/{id}", id).retrieve().toBodilessEntity();
    }

    @Override
    public List<SpeciesDto> listSpecies() {
        return client().get().uri("/species").retrieve().body(SPECIES);
    }

    @Override
    public SpeciesDto getSpecies(Long id) {
        return client().get().uri("/species/{id}", id).retrieve().body(SpeciesDto.class);
    }

    @Override
    public SpeciesDto createSpecies(SpeciesDto dto) {
        return post("/species", dto, SpeciesDto.class);
    }

    @Override
    public SpeciesDto updateSpecies(Long id, SpeciesDto dto) {
        return put("/species/{id}", dto, SpeciesDto.class, id);
    }

    @Override
    public List<StageDto> listStages(Long speciesId) {
        return client().get().uri("/species/{id}/stages", speciesId).retrieve().body(STAGES);
    }

    @Override
    public StageDto getStage(Long id) {
        return client().get().uri("/stages/{id}", id).retrieve().body(StageDto.class);
    }

    @Override
    public StageDto createStage(StageDto dto) {
        return post("/stages", dto, StageDto.class);
    }

    @Override
    public StageDto updateStage(Long id, StageDto dto) {
        return put("/stages/{id}", dto, StageDto.class, id);
    }

    @Override
    public List<RequirementSetDto> listRequirements(Long speciesId, Long stageId) {
        return client().get()
                .uri(uri -> {
                    var builder = uri.path("/requirements");
                    if (speciesId != null) {
                        builder.queryParam("speciesId", speciesId);
                    }
                    if (stageId != null) {
                        builder.queryParam("stageId", stageId);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(REQUIREMENTS);
    }

    @Override
    public RequirementSetDto getRequirement(Long id) {
        return client().get().uri("/requirements/{id}", id).retrieve().body(RequirementSetDto.class);
    }

    @Override
    public RequirementSetDto createRequirement(RequirementSetDto dto) {
        return post("/requirements", dto, RequirementSetDto.class);
    }

    @Override
    public RequirementSetDto updateRequirement(Long id, RequirementSetDto dto) {
        return put("/requirements/{id}", dto, RequirementSetDto.class, id);
    }

    @Override
    public void deleteRequirement(Long id) {
        client().delete().uri("/requirements/{id}", id).retrieve().toBodilessEntity();
    }

    @Override
    public List<FormulaDto> listFormulas(Long clientId, Long speciesId, Long stageId, String status) {
        return client().get()
                .uri(uri -> {
                    var builder = uri.path("/formulas");
                    if (clientId != null) {
                        builder.queryParam("clientId", clientId);
                    }
                    if (speciesId != null) {
                        builder.queryParam("speciesId", speciesId);
                    }
                    if (stageId != null) {
                        builder.queryParam("stageId", stageId);
                    }
                    if (status != null && !status.isBlank()) {
                        builder.queryParam("status", status);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(FORMULAS);
    }

    @Override
    public FormulaDto getFormula(Long id) {
        return client().get().uri("/formulas/{id}", id).retrieve().body(FormulaDto.class);
    }

    @Override
    public FormulaDto createFormula(FormulaDto dto) {
        return post("/formulas", dto, FormulaDto.class);
    }

    @Override
    public List<ClientFormulasHdr> listClientFormulasHdr(Long clientId) {
        return client().get()
                .uri(uri -> {
                    var builder = uri.path("/client-formulas-hdr");
                    if (clientId != null) {
                        builder.queryParam("clientId", clientId);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(FORMULA_HDRS);
    }

    @Override
    public ClientFormulasHdr getClientFormulasHdr(Long formulaId) {
        return client().get().uri("/client-formulas-hdr/{id}", formulaId).retrieve().body(ClientFormulasHdr.class);
    }

    private <T> T post(String path, Object body, Class<T> type) {
        return client().post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(type);
    }

    private <T> T put(String path, Object body, Class<T> type, Object... uriVars) {
        return client().put()
                .uri(path, uriVars)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(type);
    }

    private RestClient client() {
        RestClient current = restClient;
        if (current == null) {
            synchronized (this) {
                current = restClient;
                if (current == null) {
                    current = builder.baseUrl(resolveBaseUrl())
                            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                                String json = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                                String message = extractMessage(json);
                                int status = response.getStatusCode().value();
                                if (status == 404) {
                                    throw new NotFoundException(message);
                                }
                                throw new BusinessException(message);
                            })
                            .build();
                    restClient = current;
                }
            }
        }
        return current;
    }

    String resolveBaseUrl() {
        String template = properties.getData().getBaseUrl();
        String port = environment.getProperty("local.server.port");
        if (port == null || "0".equals(port)) {
            port = environment.getProperty("server.port", "8080");
        }
        if ("0".equals(port)) {
            port = "8080";
        }
        return template.replace("{port}", port);
    }

    private String extractMessage(String json) {
        if (json == null || json.isBlank()) {
            return "RPG data service error";
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.hasNonNull("message")) {
                return node.get("message").asText();
            }
        } catch (IOException ignored) {
            return json;
        }
        return json;
    }
}
