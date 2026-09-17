package com.iah.nutrition.optimization;

import com.iah.nutrition.config.NutritionProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FeedOptimizer {

    private final NutritionProperties properties;
    private final NonlinearFeedOptimizer nonlinearFeedOptimizer;

    public FeedOptimizer(NutritionProperties properties) {
        this.properties = properties;
        this.nonlinearFeedOptimizer = new NonlinearFeedOptimizer(properties.getOptimizer());
    }

    public OptimizationOutcome solve(FeedProblem problem) {
        LinearProgramSolver.Result linear = solveLinear(problem);
        if ("NONLINEAR".equalsIgnoreCase(problem.getOptimizationType())) {
            NonlinearFeedOptimizer.Result nonlinear = nonlinearFeedOptimizer.solve(toNonlinear(problem), linear.primal());
            String status = nonlinear.feasible() ? "OPTIMAL" : (linear.feasible() ? "PENALIZED" : "INFEASIBLE");
            return new OptimizationOutcome(
                    "NONLINEAR",
                    status,
                    nonlinear.objective(),
                    nonlinear.linearCost(),
                    nonlinear.inclusion(),
                    linear.feasible()
            );
        }
        if (!linear.feasible()) {
            return new OptimizationOutcome("LINEAR", linear.unbounded() ? "UNBOUNDED" : "INFEASIBLE",
                    Double.NaN, Double.NaN, linear.primal(), false);
        }
        return new OptimizationOutcome("LINEAR", "OPTIMAL", linear.objective(), linear.objective(), linear.primal(), true);
    }

    private LinearProgramSolver.Result solveLinear(FeedProblem problem) {
        int n = problem.ingredientCount();
        List<LinearProgramSolver.Constraint> constraints = new ArrayList<>();
        double[] sum = new double[n];
        java.util.Arrays.fill(sum, 1.0);
        constraints.add(new LinearProgramSolver.Constraint(sum, LinearProgramSolver.Relation.EQ, 1.0));
        for (int k = 0; k < problem.nutrientCount(); k++) {
            if (!Double.isNaN(problem.getMin()[k])) {
                constraints.add(new LinearProgramSolver.Constraint(problem.getMatrix()[k],
                        LinearProgramSolver.Relation.GEQ, problem.getMin()[k]));
            }
            if (!Double.isNaN(problem.getMax()[k])) {
                constraints.add(new LinearProgramSolver.Constraint(problem.getMatrix()[k],
                        LinearProgramSolver.Relation.LEQ, problem.getMax()[k]));
            }
        }
        return LinearProgramSolver.minimize(problem.getPrice(), problem.getLower(), problem.getUpper(), constraints);
    }

    private NonlinearFeedOptimizer.Problem toNonlinear(FeedProblem problem) {
        NutritionProperties.Optimizer opt = properties.getOptimizer();
        return new NonlinearFeedOptimizer.Problem(
                problem.getPrice(),
                problem.getQuadraticCost(),
                problem.getLower(),
                problem.getUpper(),
                problem.getMatrix(),
                problem.getMin(),
                problem.getMax(),
                problem.getTarget(),
                problem.getRatios(),
                opt.getQuadraticMixPenalty(),
                opt.getConstraintPenalty(),
                opt.getRatioPenalty()
        );
    }

    public record OptimizationOutcome(
            String type,
            String status,
            double objective,
            double linearCost,
            double[] inclusion,
            boolean linearlyFeasible
    ) {
    }
}
