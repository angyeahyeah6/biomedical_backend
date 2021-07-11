package weng.form;

public class MyForm {
    private String drug;
    private int endYear;
    private int classifierType;

    public MyForm(String drug, int endYear, int classifierType) {
        this.drug = drug;
        this.endYear = endYear;
        this.classifierType = classifierType;
    }

    public String getDrug() {
        return drug;
    }

    public void setDrug(String drug) {
        this.drug = drug;
    }

    public void setClassifierType(int classifierType) {
        this.classifierType = classifierType;
    }

    public int getEndYear() {
        return endYear;
    }

    public void setEndYear(int endYear) {
        this.endYear = endYear;
    }

    public int getClassifierType() {
        return classifierType;
    }
}
