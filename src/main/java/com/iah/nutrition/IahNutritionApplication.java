package com.iah.nutrition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IahNutritionApplication {

    public static void main(String[] args) {
        SpringApplication.run(IahNutritionApplication.class, args);
    }
}
