package weng.model;

public class IntermediateCount {
    private String drug;
    private String disease;
    private int intermediateNum;

    public IntermediateCount(String drug, String disease, int intermediateNum) {
        this.drug = drug;
        this.disease = disease;
        this.intermediateNum = intermediateNum;
    }

    public void setDrug(String drug) {
        this.drug = drug;
    }

    public void setDisease(String disease) {
        this.disease = disease;
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

    public int getIntermediateNum() {
        return intermediateNum;
    }
}
