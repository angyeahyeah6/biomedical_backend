package weng.labelSys;

public class Drug {
    private String name;
    private int startYear;
    private int endYear;

    public Drug(String name, int startYear, int endYear) {
        this.name = name;
        this.startYear = startYear;
        this.endYear = endYear;
    }

    public void setDrug(String drug) {
        this.name = drug;
    }

    public void setStartYear(int startYear) {
        this.startYear = startYear;
    }

    public void setEndYear(int endYear) {
        this.endYear = endYear;
    }

    public String getName() {
        return name;
    }

    public int getStartYear() {
        return startYear;
    }

    public int getEndYear() {
        return endYear;
    }
}
