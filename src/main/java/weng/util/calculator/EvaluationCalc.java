package weng.util.calculator;

public class EvaluationCalc {

    public static double precision(double tp, double fp) {
        return (tp + fp) != 0d ? tp / (tp + fp) : 0d;
    }

    public static double recall(double tp, double fn) {
        return (tp + fn) != 0d ? tp / (tp + fn) : 0d;
    }

    public static double fMeasure(double precision, double recall) {
        return (precision + recall) != 0d ? 2 * precision * recall / (precision + recall) : 0d;
    }

    public static double weightedAvg(double a, double b,
                                     double tp, double tn, double fp, double fn) {
        return (a * (tp + fn) + b * (fp + tn)) / (tp + tn + fp + fn);
    }
}
