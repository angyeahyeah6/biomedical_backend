package weng.util.repurposing;

public class PredicateImportanceScore {
    private double score;
    private double totalPredicate;

    public PredicateImportanceScore(double score, double totalPredicate) {
        this.score = score;
        this.totalPredicate = totalPredicate;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setTotalPredicate(double totalPredicate) {
        this.totalPredicate = totalPredicate;
    }

    public double getScore() {
        return score;
    }

    public double getTotalPredicate() {
        return totalPredicate;
    }
}
