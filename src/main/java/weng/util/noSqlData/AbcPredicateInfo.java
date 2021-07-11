package weng.util.noSqlData;

import java.util.Map;

public class AbcPredicateInfo {
    private String drug;
    private String disease;
    private String intermediate;
    private String abPredicate;
    private String bcPredicate;
    private Map<String, Integer> abPredicates;
    private Map<String, Integer> bcPredicates;

    public AbcPredicateInfo(String intermediate, String abPredicate, String bcPredicate) {
        this.intermediate = intermediate;
        this.abPredicate = abPredicate;
        this.bcPredicate = bcPredicate;
    }

    public AbcPredicateInfo(String drug, String intermediate, String disease,
                            Map<String, Integer> abPredicates,
                            Map<String, Integer> bcPredicates) {
        this.drug = drug;
        this.intermediate = intermediate;
        this.disease = disease;
        this.abPredicates = abPredicates;
        this.bcPredicates = bcPredicates;
    }

    public void setDrug(String drug) {
        this.drug = drug;
    }

    public void setDisease(String disease) {
        this.disease = disease;
    }

    public void setIntermediate(String intermediate) {
        this.intermediate = intermediate;
    }

    public void setAbPredicate(String abPredicate) {
        this.abPredicate = abPredicate;
    }

    public void setBcPredicate(String bcPredicate) {
        this.bcPredicate = bcPredicate;
    }

    public void setAbPredicates(Map<String, Integer> abPredicates) {
        this.abPredicates = abPredicates;
    }

    public void setBcPredicates(Map<String, Integer> bcPredicates) {
        this.bcPredicates = bcPredicates;
    }

    public String getDrug() {
        return drug;
    }

    public String getDisease() {
        return disease;
    }

    public String getIntermediate() {
        return intermediate;
    }

    public String getAbPredicate() {
        return abPredicate;
    }

    public String getBcPredicate() {
        return bcPredicate;
    }

    public Map<String, Integer> getAbPredicates() {
        return abPredicates;
    }

    public Map<String, Integer> getBcPredicates() {
        return bcPredicates;
    }
}
