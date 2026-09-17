package com.iah.nutrition.optimization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FeedProblem {

    private final List<Long> ingredientIds = new ArrayList<>();
    private final List<String> ingredientCodes = new ArrayList<>();
    private final List<String> ingredientNames = new ArrayList<>();
    private double[] price;
    private double[] quadraticCost;
    private double[] lower;
    private double[] upper;
    private final List<Long> nutrientIds = new ArrayList<>();
    private final List<String> nutrientCodes = new ArrayList<>();
    private final List<String> nutrientNames = new ArrayList<>();
    private final List<String> nutrientUnits = new ArrayList<>();
    private double[][] matrix;
    private double[] min;
    private double[] max;
    private double[] target;
    private final List<NonlinearFeedOptimizer.RatioTarget> ratios = new ArrayList<>();
    private double batchKg = 1000;
    private String optimizationType = "LINEAR";

    public int ingredientCount() {
        return ingredientIds.size();
    }

    public int nutrientCount() {
        return nutrientIds.size();
    }

    public List<Long> getIngredientIds() {
        return ingredientIds;
    }

    public List<String> getIngredientCodes() {
        return ingredientCodes;
    }

    public List<String> getIngredientNames() {
        return ingredientNames;
    }

    public double[] getPrice() {
        return price;
    }

    public void setPrice(double[] price) {
        this.price = price;
    }

    public double[] getQuadraticCost() {
        return quadraticCost;
    }

    public void setQuadraticCost(double[] quadraticCost) {
        this.quadraticCost = quadraticCost;
    }

    public double[] getLower() {
        return lower;
    }

    public void setLower(double[] lower) {
        this.lower = lower;
    }

    public double[] getUpper() {
        return upper;
    }

    public void setUpper(double[] upper) {
        this.upper = upper;
    }

    public List<Long> getNutrientIds() {
        return nutrientIds;
    }

    public List<String> getNutrientCodes() {
        return nutrientCodes;
    }

    public List<String> getNutrientNames() {
        return nutrientNames;
    }

    public List<String> getNutrientUnits() {
        return nutrientUnits;
    }

    public double[][] getMatrix() {
        return matrix;
    }

    public void setMatrix(double[][] matrix) {
        this.matrix = matrix;
    }

    public double[] getMin() {
        return min;
    }

    public void setMin(double[] min) {
        this.min = min;
    }

    public double[] getMax() {
        return max;
    }

    public void setMax(double[] max) {
        this.max = max;
    }

    public double[] getTarget() {
        return target;
    }

    public void setTarget(double[] target) {
        this.target = target;
    }

    public List<NonlinearFeedOptimizer.RatioTarget> getRatios() {
        return ratios;
    }

    public double getBatchKg() {
        return batchKg;
    }

    public void setBatchKg(double batchKg) {
        this.batchKg = batchKg;
    }

    public String getOptimizationType() {
        return optimizationType;
    }

    public void setOptimizationType(String optimizationType) {
        this.optimizationType = optimizationType;
    }

    public Map<String, Double> nutrientProfile(double[] x) {
        Map<String, Double> profile = new LinkedHashMap<>();
        for (int k = 0; k < nutrientCount(); k++) {
            double value = 0.0;
            for (int i = 0; i < ingredientCount(); i++) {
                value += matrix[k][i] * x[i];
            }
            profile.put(nutrientCodes.get(k), value);
        }
        return profile;
    }
}
