package com.iah.nutrition.optimization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Two-phase simplex solver for least-cost linear feed formulation.
 * Pure Java so it can run on IBM i PASE without native solver libraries.
 */
public final class LinearProgramSolver {

    public enum Relation {
        LEQ, GEQ, EQ
    }

    public record Constraint(double[] coefficients, Relation relation, double rhs) {
    }

    public record Result(boolean feasible, boolean unbounded, double objective, double[] primal) {
        public static Result infeasible(int n) {
            return new Result(false, false, Double.NaN, new double[n]);
        }

        public static Result unbounded(int n) {
            return new Result(false, true, Double.NEGATIVE_INFINITY, new double[n]);
        }
    }

    private static final double EPS = 1e-8;
    private static final int MAX_PIVOTS = 50_000;

    private LinearProgramSolver() {
    }

    public static Result minimize(double[] cost, double[] lower, double[] upper, List<Constraint> constraints) {
        int n = cost.length;
        List<Constraint> all = new ArrayList<>(constraints);
        for (int i = 0; i < n; i++) {
            if (lower[i] > EPS) {
                double[] row = new double[n];
                row[i] = 1.0;
                all.add(new Constraint(row, Relation.GEQ, lower[i]));
            }
            if (Double.isFinite(upper[i])) {
                double[] row = new double[n];
                row[i] = 1.0;
                all.add(new Constraint(row, Relation.LEQ, upper[i]));
            }
        }
        return solve(cost, all);
    }

    private static Result solve(double[] cost, List<Constraint> constraints) {
        int n = cost.length;
        int m = constraints.size();
        if (m == 0) {
            double[] x = new double[n];
            return new Result(true, false, 0.0, x);
        }

        double[][] aeq = new double[m][n];
        double[] beq = new double[m];
        Relation[] rels = new Relation[m];
        for (int i = 0; i < m; i++) {
            Constraint c = constraints.get(i);
            double sign = 1.0;
            double rhs = c.rhs();
            Relation rel = c.relation();
            if (rhs < 0) {
                sign = -1.0;
                rhs = -rhs;
                rel = flip(rel);
            }
            for (int j = 0; j < n; j++) {
                aeq[i][j] = sign * c.coefficients()[j];
            }
            beq[i] = rhs;
            rels[i] = rel;
        }

        int slack = 0;
        int arts = 0;
        for (Relation rel : rels) {
            if (rel == Relation.LEQ || rel == Relation.GEQ) {
                slack++;
            }
            if (rel == Relation.GEQ || rel == Relation.EQ) {
                arts++;
            }
        }

        int nTotal = n + slack + arts;
        double[][] a = new double[m][nTotal];
        int[] basic = new int[m];
        boolean[] artificial = new boolean[nTotal];
        int s = 0;
        int ar = 0;
        for (int i = 0; i < m; i++) {
            System.arraycopy(aeq[i], 0, a[i], 0, n);
            if (rels[i] == Relation.LEQ) {
                int col = n + s++;
                a[i][col] = 1.0;
                basic[i] = col;
            } else if (rels[i] == Relation.GEQ) {
                int surplus = n + s++;
                a[i][surplus] = -1.0;
                int art = n + slack + ar++;
                a[i][art] = 1.0;
                basic[i] = art;
                artificial[art] = true;
            } else {
                int art = n + slack + ar++;
                a[i][art] = 1.0;
                basic[i] = art;
                artificial[art] = true;
            }
        }

        boolean[] blocked = new boolean[nTotal];
        if (arts > 0) {
            double[] phase1Cost = new double[nTotal];
            for (int j = 0; j < nTotal; j++) {
                if (artificial[j]) {
                    phase1Cost[j] = 1.0;
                }
            }
            SimplexOutcome phase1 = runSimplex(a, beq, basic, phase1Cost, blocked);
            if (phase1 == SimplexOutcome.UNBOUNDED) {
                return Result.unbounded(n);
            }
            double phase1Obj = basicObjective(a, beq, basic, phase1Cost);
            if (phase1Obj > 1e-6) {
                return Result.infeasible(n);
            }
            System.arraycopy(artificial, 0, blocked, 0, nTotal);
        }

        double[] phase2Cost = new double[nTotal];
        System.arraycopy(cost, 0, phase2Cost, 0, n);
        SimplexOutcome phase2 = runSimplex(a, beq, basic, phase2Cost, blocked);
        if (phase2 == SimplexOutcome.UNBOUNDED) {
            return Result.unbounded(n);
        }

        double[] x = new double[n];
        for (int i = 0; i < m; i++) {
            if (basic[i] < n) {
                x[basic[i]] = Math.max(0.0, beq[i]);
            }
        }
        return new Result(true, false, basicObjective(a, beq, basic, phase2Cost), x);
    }

    private enum SimplexOutcome {
        OPTIMAL, UNBOUNDED
    }

    private static SimplexOutcome runSimplex(double[][] a, double[] b, int[] basic, double[] cost, boolean[] blocked) {
        int m = a.length;
        int nTotal = cost.length;
        for (int iter = 0; iter < MAX_PIVOTS; iter++) {
            double[] reduced = reducedCosts(a, basic, cost);
            int entering = -1;
            double mostNeg = -EPS;
            for (int j = 0; j < nTotal; j++) {
                if (blocked[j]) {
                    continue;
                }
                if (reduced[j] < mostNeg) {
                    mostNeg = reduced[j];
                    entering = j;
                }
            }
            if (entering < 0) {
                return SimplexOutcome.OPTIMAL;
            }

            int leaving = -1;
            double minRatio = Double.POSITIVE_INFINITY;
            for (int i = 0; i < m; i++) {
                if (a[i][entering] > EPS) {
                    double ratio = b[i] / a[i][entering];
                    if (ratio < minRatio - EPS || (Math.abs(ratio - minRatio) <= EPS && (leaving < 0 || i < leaving))) {
                        minRatio = ratio;
                        leaving = i;
                    }
                }
            }
            if (leaving < 0) {
                return SimplexOutcome.UNBOUNDED;
            }
            pivot(a, b, leaving, entering);
            basic[leaving] = entering;
        }
        throw new IllegalStateException("Simplex exceeded maximum pivots");
    }

    private static double[] reducedCosts(double[][] a, int[] basic, double[] cost) {
        int m = a.length;
        int nTotal = cost.length;
        double[] cB = new double[m];
        for (int i = 0; i < m; i++) {
            cB[i] = cost[basic[i]];
        }
        double[] reduced = Arrays.copyOf(cost, nTotal);
        for (int j = 0; j < nTotal; j++) {
            double dot = 0.0;
            for (int i = 0; i < m; i++) {
                dot += cB[i] * a[i][j];
            }
            reduced[j] -= dot;
        }
        return reduced;
    }

    private static double basicObjective(double[][] a, double[] b, int[] basic, double[] cost) {
        double obj = 0.0;
        for (int i = 0; i < basic.length; i++) {
            obj += cost[basic[i]] * b[i];
        }
        return obj;
    }

    private static void pivot(double[][] a, double[] b, int row, int col) {
        int nTotal = a[0].length;
        double pivot = a[row][col];
        for (int j = 0; j < nTotal; j++) {
            a[row][j] /= pivot;
        }
        b[row] /= pivot;
        for (int i = 0; i < a.length; i++) {
            if (i == row) {
                continue;
            }
            double factor = a[i][col];
            if (Math.abs(factor) < EPS) {
                continue;
            }
            for (int j = 0; j < nTotal; j++) {
                a[i][j] -= factor * a[row][j];
            }
            b[i] -= factor * b[row];
        }
    }

    private static Relation flip(Relation rel) {
        return switch (rel) {
            case LEQ -> Relation.GEQ;
            case GEQ -> Relation.LEQ;
            case EQ -> Relation.EQ;
        };
    }

    public static String describe(Result result) {
        if (!result.feasible()) {
            return result.unbounded() ? "UNBOUNDED" : "INFEASIBLE";
        }
        return String.format(Locale.US, "OPTIMAL obj=%.6f", result.objective());
    }
}
