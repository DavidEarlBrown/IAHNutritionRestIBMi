package com.iah.nutrition.optimization;

import com.iah.nutrition.config.NutritionProperties;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer.PopulationSize;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer.Sigma;
import org.apache.commons.math3.random.MersenneTwister;

import java.util.Arrays;
import java.util.List;

/**
 * Nonlinear least-cost formulation: quadratic ingredient costs, mix-diversity
 * penalty, Ca:P and other nutrient-ratio targets, and exponential utilization
 * terms. Constraints are enforced with a quadratic penalty around a linear
 * feasible start when one exists.
 */
public final class NonlinearFeedOptimizer {

    public record RatioTarget(int numeratorNutrientIndex, int denominatorNutrientIndex, double targetRatio) {
    }

    public record Problem(
            double[] linearCost,
            double[] quadraticCost,
            double[] lower,
            double[] upper,
            double[][] nutrientMatrix,
            double[] nutrientMin,
            double[] nutrientMax,
            double[] nutrientTarget,
            List<RatioTarget> ratios,
            double mixPenalty,
            double constraintPenalty,
            double ratioPenalty
    ) {
    }

    public record Result(boolean feasible, double objective, double linearCost, double[] inclusion) {
    }

    private final NutritionProperties.Optimizer settings;

    public NonlinearFeedOptimizer(NutritionProperties.Optimizer settings) {
        this.settings = settings;
    }

    public Result solve(Problem problem, double[] start) {
        int n = problem.linearCost().length;
        double[] initial = project(start != null ? start : equalStart(n, problem), problem);
        double[] sigma = new double[n];
        for (int i = 0; i < n; i++) {
            double span = Math.max(1e-4, problem.upper()[i] - problem.lower()[i]);
            sigma[i] = Math.max(1e-4, span * 0.15);
        }

        PointValuePair best;
        try {
            CMAESOptimizer optimizer = new CMAESOptimizer(
                    settings.getMaxIterations(),
                    1e-9,
                    true,
                    0,
                    0,
                    new MersenneTwister(42),
                    false,
                    null
            );
            best = optimizer.optimize(
                    new MaxEval(settings.getNonlinearMaxEvaluations()),
                    new ObjectiveFunction(x -> objective(project(x, problem), problem)),
                    GoalType.MINIMIZE,
                    new InitialGuess(initial),
                    new SimpleBounds(problem.lower(), problem.upper()),
                    new Sigma(sigma),
                    new PopulationSize(Math.max(5, settings.getNonlinearPopulation()))
            );
        } catch (RuntimeException ex) {
            int interpolation = Math.min(2 * n + 1, (n + 1) * (n + 2) / 2);
            interpolation = Math.max(n + 2, interpolation);
            BOBYQAOptimizer fallback = new BOBYQAOptimizer(interpolation);
            best = fallback.optimize(
                    new MaxEval(settings.getNonlinearMaxEvaluations()),
                    new ObjectiveFunction(x -> objective(project(x, problem), problem)),
                    GoalType.MINIMIZE,
                    new InitialGuess(initial),
                    new SimpleBounds(problem.lower(), problem.upper())
            );
        }

        double[] x = project(best.getPoint(), problem);
        double violation = constraintViolation(x, problem);
        double linear = dot(problem.linearCost(), x);
        return new Result(violation <= 1e-4, objectiveWithoutPenalty(x, problem), linear, x);
    }

    private double[] equalStart(int n, Problem problem) {
        double[] x = new double[n];
        Arrays.fill(x, 1.0 / n);
        return project(x, problem);
    }

    private static double objective(double[] x, Problem problem) {
        return objectiveWithoutPenalty(x, problem) + problem.constraintPenalty() * constraintViolation(x, problem);
    }

    private static double objectiveWithoutPenalty(double[] x, Problem problem) {
        double cost = 0.0;
        double mix = 0.0;
        for (int i = 0; i < x.length; i++) {
            cost += problem.linearCost()[i] * x[i] + problem.quadraticCost()[i] * x[i] * x[i];
            mix += x[i] * x[i];
        }
        double ratioTerm = 0.0;
        for (RatioTarget ratio : problem.ratios()) {
            double num = nutrientValue(x, problem, ratio.numeratorNutrientIndex());
            double den = nutrientValue(x, problem, ratio.denominatorNutrientIndex());
            if (Math.abs(den) < 1e-9) {
                ratioTerm += 1.0;
            } else {
                double err = (num / den) - ratio.targetRatio();
                ratioTerm += err * err;
            }
        }
        double targetTerm = 0.0;
        for (int k = 0; k < problem.nutrientTarget().length; k++) {
            if (Double.isNaN(problem.nutrientTarget()[k])) {
                continue;
            }
            double achieved = nutrientValue(x, problem, k);
            double err = achieved - problem.nutrientTarget()[k];
            targetTerm += 0.05 * err * err;
        }
        return cost + problem.mixPenalty() * mix + problem.ratioPenalty() * ratioTerm + targetTerm;
    }

    private static double constraintViolation(double[] x, Problem problem) {
        double v = 0.0;
        double sum = 0.0;
        for (double xi : x) {
            sum += xi;
        }
        v += square(sum - 1.0);
        for (int k = 0; k < problem.nutrientMin().length; k++) {
            double achieved = nutrientValue(x, problem, k);
            if (!Double.isNaN(problem.nutrientMin()[k])) {
                v += square(Math.max(0.0, problem.nutrientMin()[k] - achieved));
            }
            if (!Double.isNaN(problem.nutrientMax()[k])) {
                v += square(Math.max(0.0, achieved - problem.nutrientMax()[k]));
            }
        }
        return v;
    }

    private static double nutrientValue(double[] x, Problem problem, int nutrientIndex) {
        double value = 0.0;
        double[] row = problem.nutrientMatrix()[nutrientIndex];
        for (int i = 0; i < x.length; i++) {
            value += row[i] * x[i];
        }
        return value;
    }

    static double[] project(double[] raw, Problem problem) {
        int n = raw.length;
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = clamp(raw[i], problem.lower()[i], problem.upper()[i]);
        }
        double sum = 0.0;
        for (double xi : x) {
            sum += xi;
        }
        if (sum <= 1e-12) {
            Arrays.fill(x, 0.0);
            x[0] = clamp(1.0, problem.lower()[0], problem.upper()[0]);
            return x;
        }
        for (int i = 0; i < n; i++) {
            x[i] /= sum;
            x[i] = clamp(x[i], problem.lower()[i], problem.upper()[i]);
        }
        sum = 0.0;
        for (double xi : x) {
            sum += xi;
        }
        if (Math.abs(sum - 1.0) > 1e-8 && sum > 1e-12) {
            for (int i = 0; i < n; i++) {
                x[i] /= sum;
            }
        }
        return x;
    }

    private static double clamp(double value, double lo, double hi) {
        return Math.max(lo, Math.min(hi, value));
    }

    private static double square(double v) {
        return v * v;
    }

    private static double dot(double[] a, double[] b) {
        double s = 0.0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }
}
