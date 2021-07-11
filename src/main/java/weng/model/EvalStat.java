package weng.model;

public class EvalStat {
    private String a;
    private String b;
    private String c;
    private String attributes;

    public EvalStat(String a, String b, String c, String attributes) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.attributes = attributes;
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

    public void setAttributes(String attributes) {
        this.attributes = attributes;
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

    public String getAttributes() {
        return attributes;
    }
}
