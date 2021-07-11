package weng.model;

public class EvalResult {
    private String a;
    private String b;
    private String c;
    private String real;
    private String predict;

    public EvalResult(String a, String b, String c, String real, String predict) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.real = real;
        this.predict = predict;
    }

    public void setA(String a) {
        this.a = a;
    }

    public void setB(String b) {
        this.b = b;
    }

    public void setC(String c) {
        this.c = c;
    }

    public void setReal(String real) {
        this.real = real;
    }

    public void setPredict(String predict) {
        this.predict = predict;
    }

    public String getA() {
        return a;
    }

    public String getB() {
        return b;
    }

    public String getC() {
        return c;
    }

    public String getReal() {
        return real;
    }

    public String getPredict() {
        return predict;
    }
}
