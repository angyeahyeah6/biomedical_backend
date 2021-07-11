package weng.util.sqlData;

public class Predicate {
    private String name;
    private int freq;
    private String type;
    private String importance;
    private int importanceScore;

    public Predicate(String name,
                     int freq,
                     String type,
                     String importance,
                     int importanceScore) {
        this.name = name;
        this.freq = freq;
        this.type = type;
        this.importance = importance;
        this.importanceScore = importanceScore;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public void setImportanceScore(int importanceScore) {
        this.importanceScore = importanceScore;
    }

    public String getName() {
        return name;
    }

    public int getFreq() {
        return freq;
    }

    public String getType() {
        return type;
    }

    public String getImportance() {
        return importance;
    }

    public int getImportanceScore() {
        return importanceScore;
    }
}