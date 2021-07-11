package weng.network;

/**
 * Created by lbj23k on 2017/6/2.
 */
public class PredictInfo implements Comparable<PredictInfo> {
    private String pmid;
    private String predicate;
    private int year;
    private int direction;

    public PredictInfo(String predicate, String pmid, int year, int direction) {
        this.pmid = pmid;
        this.year = year;
        this.direction = direction; // 1 = ->, 2 = <-
        this.predicate = predicate;
    }

    public String getPmid() {
        return pmid;
    }

    public String getContent(boolean isAB) {
        String label = (isAB) ? "_AB" : "_BC";
        return predicate + label;
    }

    public String getPredicate() {
        return predicate;
    }

    public int getYear() {
        return year;
    }

    public int getDirection() {
        return direction;
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof PredictInfo) {
            PredictInfo p = (PredictInfo) that;
            return this.predicate.equals(p.predicate);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public int compareTo(PredictInfo o) {
        if (this.year > o.year) return 1;
        else if (this.year < o.year) return -1;
        return 0;
    }
}
