package weng.label;

import weng.util.file.Predicate;

/**
 * Created by lbj23k on 2017/4/12.
 */
public class LabeledInfo implements Comparable<LabeledInfo> {
    private String pmid;
    private int year;
    private int direction; // 1 = ->, 2 = <-
    private String predicate;

    public LabeledInfo(int direction, String predicate, String pmid, int year) {
        this.direction = direction;
        this.pmid = pmid;
        this.year = year;
        this.predicate = predicate;
    }

    public String getPredicateFiltered() {
        String predicate = "";
        if (Predicate.set.contains(this.predicate)) {
            predicate = this.predicate;
        }
        return predicate;
    }

    public String getPredicate() {
        return predicate;
    }

    public int getDirection() {
        return direction;
    }

    public String getPmid() {
        return pmid;
    }

    public int getYear() {
        return year;
    }

    @Override
    public String toString() {
        String directPresent = (direction == 1) ? "->" : "<-";
        return directPresent + predicate + "(" + year + ", " + pmid + ")";
    }


    @Override
    public boolean equals(Object that) {
        if (that instanceof LabeledInfo) {
            LabeledInfo p = (LabeledInfo) that;
            return this.predicate.equals(p.predicate);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    public String getString() {
        String directPresent = (direction == 1) ? "->" : "<-";
        return directPresent + predicate;
    }


    @Override
    public int compareTo(LabeledInfo o) {
        return this.year - o.year;
    }
}
