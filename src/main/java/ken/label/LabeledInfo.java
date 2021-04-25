package ken.label;

import ken.util.Utils;

import java.util.List;

/**
 * Created by lbj23k on 2017/4/12.
 */
public class LabeledInfo implements Comparable<LabeledInfo> {
    public String pmid;
    public int year;
    // 1 = ->, 2 = <-
    public final int direction;
    public final String predicate;
    private static List<String> predicateMap;

    static {
        predicateMap = Utils.readLineFile("vocabulary/contentWord.txt");
    }

    public LabeledInfo(int direction, String predicate, String pmid, int year) {
        this.direction = direction;
        this.pmid = pmid;
        this.year = year;
        this.predicate = predicate;
    }

    public String getPredicate() {
        String result = "";
        if (predicateMap.contains(predicate)) {
            result = predicate;
        }
        return result;
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

    public String getPmid() {
        return pmid;
    }

}
