package com.iah.nutrition.optimization;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinearProgramSolverTest {

    @Test
    void solvesClassicLeastCostDiet() {
        // min 0.22 corn + 0.48 sbm
        // protein: 8.3 c + 47.5 s >= 16
        // energy:  1.98 c + 2.10 s >= 1.65
        // c + s = 1, 0 <= c,s <= 1
        double[] cost = {0.22, 0.48};
        double[] lower = {0, 0};
        double[] upper = {1, 1};
        List<LinearProgramSolver.Constraint> constraints = List.of(
                new LinearProgramSolver.Constraint(new double[]{1, 1}, LinearProgramSolver.Relation.EQ, 1.0),
                new LinearProgramSolver.Constraint(new double[]{8.3, 47.5}, LinearProgramSolver.Relation.GEQ, 16.0),
                new LinearProgramSolver.Constraint(new double[]{1.98, 2.10}, LinearProgramSolver.Relation.GEQ, 1.65)
        );

        LinearProgramSolver.Result result = LinearProgramSolver.minimize(cost, lower, upper, constraints);

        assertTrue(result.feasible());
        assertEquals(1.0, result.primal()[0] + result.primal()[1], 1e-6);
        double protein = 8.3 * result.primal()[0] + 47.5 * result.primal()[1];
        assertTrue(protein + 1e-6 >= 16.0);
        assertTrue(result.objective() < 0.48);
        assertTrue(result.objective() > 0.22);
    }

    @Test
    void detectsInfeasibleNutrientDemand() {
        double[] cost = {1, 1};
        double[] lower = {0, 0};
        double[] upper = {1, 1};
        List<LinearProgramSolver.Constraint> constraints = List.of(
                new LinearProgramSolver.Constraint(new double[]{1, 1}, LinearProgramSolver.Relation.EQ, 1.0),
                new LinearProgramSolver.Constraint(new double[]{1, 1}, LinearProgramSolver.Relation.GEQ, 50.0)
        );

        LinearProgramSolver.Result result = LinearProgramSolver.minimize(cost, lower, upper, constraints);
        assertTrue(!result.feasible());
    }
}
