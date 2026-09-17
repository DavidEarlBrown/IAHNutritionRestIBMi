package com.iah.nutrition.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iah.nutrition")
public class NutritionProperties {

    private String schema = "IAHNUTR";
    private double defaultBatchKg = 1000;
    private final Optimizer optimizer = new Optimizer();
    private final Data data = new Data();

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public double getDefaultBatchKg() {
        return defaultBatchKg;
    }

    public void setDefaultBatchKg(double defaultBatchKg) {
        this.defaultBatchKg = defaultBatchKg;
    }

    public Optimizer getOptimizer() {
        return optimizer;
    }

    public Data getData() {
        return data;
    }

    public static class Data {
        /**
         * Base URL of the ILE RPG REST data service.
         * Use {port} as a placeholder for the local simulator port.
         */
        private String baseUrl = "http://127.0.0.1:{port}/data";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Optimizer {
        private int maxIterations = 20000;
        private int nonlinearPopulation = 40;
        private int nonlinearMaxEvaluations = 20000;
        private double constraintPenalty = 1_000_000;
        private double ratioPenalty = 250;
        private double quadraticMixPenalty = 0.15;

        public int getMaxIterations() {
            return maxIterations;
        }

        public void setMaxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
        }

        public int getNonlinearPopulation() {
            return nonlinearPopulation;
        }

        public void setNonlinearPopulation(int nonlinearPopulation) {
            this.nonlinearPopulation = nonlinearPopulation;
        }

        public int getNonlinearMaxEvaluations() {
            return nonlinearMaxEvaluations;
        }

        public void setNonlinearMaxEvaluations(int nonlinearMaxEvaluations) {
            this.nonlinearMaxEvaluations = nonlinearMaxEvaluations;
        }

        public double getConstraintPenalty() {
            return constraintPenalty;
        }

        public void setConstraintPenalty(double constraintPenalty) {
            this.constraintPenalty = constraintPenalty;
        }

        public double getRatioPenalty() {
            return ratioPenalty;
        }

        public void setRatioPenalty(double ratioPenalty) {
            this.ratioPenalty = ratioPenalty;
        }

        public double getQuadraticMixPenalty() {
            return quadraticMixPenalty;
        }

        public void setQuadraticMixPenalty(double quadraticMixPenalty) {
            this.quadraticMixPenalty = quadraticMixPenalty;
        }
    }
}
