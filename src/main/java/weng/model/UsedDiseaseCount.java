package weng.model;

public class UsedDiseaseCount {
    private String drug;
    private int diseaseNum;

    public UsedDiseaseCount(String drug, int diseaseNum) {
        this.drug = drug;
        this.diseaseNum = diseaseNum;
    }

    public void setDrug(String drug) {
        this.drug = drug;
    }

    public void setDiseaseNum(int diseaseNum) {
        this.diseaseNum = diseaseNum;
    }

    public String getDrug() {
        return drug;
    }

    public int getDiseaseNum() {
        return diseaseNum;
    }
}
