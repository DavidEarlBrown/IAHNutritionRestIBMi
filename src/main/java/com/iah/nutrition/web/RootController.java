package com.iah.nutrition.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "name", "IAH Nutrition REST",
                "platform", "IBM i / Java 17",
                "docs", "/api",
                "data", "/data (local RPG simulator; IBM i uses ILE RPG CGI /iahdata)",
                "health", "/actuator/health"
        );
    }

    @GetMapping("/api")
    public Map<String, String> api() {
        return Map.of(
                "nutrients", "/api/nutrients",
                "ingredients", "/api/ingredients",
                "ingrednut", "/api/ingrednut",
                "clients", "/api/clients",
                "species", "/api/species",
                "requirements", "/api/requirements",
                "optimize", "POST /api/optimize",
                "formulas", "/api/formulas"
        );
    }
}
