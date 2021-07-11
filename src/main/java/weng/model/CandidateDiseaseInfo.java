package weng.model;

public class CandidateDiseaseInfo {
    private String drug;
    private String disease;
    private int realScore;
    private int intermediateNum;

    public CandidateDiseaseInfo(String drug, String disease, int realScore, int intermediateNum) {
        this.drug = drug;
        this.disease = disease;
        this.realScore = realScore;
        this.intermediateNum = intermediateNum;
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

    public void setIntermediateNum(int intermediateNum) {
        this.intermediateNum = intermediateNum;
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

    public int getIntermediateNum() {
        return intermediateNum;
    }
}
