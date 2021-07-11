package weng.util.noSqlData;

public class PredicationCountInfo {
    private String drug;
    private String intermediate;
    private String disease;
    private int count_ab;
    private int count_bc;

    public PredicationCountInfo(String drug, String intermediate, String disease, int count_ab, int count_bc) {
        this.drug = drug;
        this.intermediate = intermediate;
        this.disease = disease;
        this.count_ab = count_ab;
        this.count_bc = count_bc;
    }

    public void setCount_ab(int count_ab) {
        this.count_ab = count_ab;
    }

    public void setCount_bc(int count_bc) {
        this.count_bc = count_bc;
    }

    public void setDisease(String disease) {
        this.disease = disease;
    }

    public void setDrug(String drug) {
        this.drug = drug;
    }

    public void setIntermediate(String intermediate) {
        this.intermediate = intermediate;
    }

    public int getCount_ab() {
        return count_ab;
    }

    public int getCount_bc() {
        return count_bc;
    }

    public String getDisease() {
        return disease;
    }

    public String getDrug() {
        return drug;
    }

    public String getIntermediate() {
        return intermediate;
    }
}
