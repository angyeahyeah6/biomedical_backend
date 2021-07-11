package weng.evaluation;

public class ConfusionMatrix {
    private double tp;
    private double tn;
    private double fp;
    private double fn;

    public ConfusionMatrix(double tp, double tn, double fp, double fn) {
        this.tp = tp;
        this.tn = tn;
        this.fp = fp;
        this.fn = fn;
    }

    public void setTp(double tp) {
        this.tp = tp;
    }

    public void setTn(double tn) {
        this.tn = tn;
    }

    public void setFp(double fp) {
        this.fp = fp;
    }

    public void setFn(double fn) {
        this.fn = fn;
    }

    public double getTp() {
        return tp;
    }

    public double getTn() {
        return tn;
    }

    public double getFp() {
        return fp;
    }

    public double getFn() {
        return fn;
    }
}
