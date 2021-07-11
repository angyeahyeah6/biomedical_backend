package weng.model;

public class EvalPerformance {
    private double precision;
    private double recall;
    private double fMeasure;

    public EvalPerformance(double precision, double recall, double fMeasure) {
        this.precision = precision;
        this.recall = recall;
        this.fMeasure = fMeasure;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public void setFMeasure(double fMeasure) {
        this.fMeasure = fMeasure;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public double getFMeasure() {
        return fMeasure;
    }
}
