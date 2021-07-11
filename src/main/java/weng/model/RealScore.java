package weng.model;

public class RealScore {
    private String drug;
    private String disease;
    private int realScore;

    public RealScore(String drug, String disease, int realScore) {
        this.drug = drug;
        this.disease = disease;
        this.realScore = realScore;
    }

    public void setDrug(String drug) {
        this.drug = drug;
    }

    public void setDisease(String disease) {
        this.disease = disease;
    }

    public void setRealScore(int realScore) {
        this.realScore = realScore;
    }

    public String getDrug() {
        return drug;
    }

    public String getDisease() {
        return disease;
    }

    public int getRealScore() {
        return realScore;
    }
}
