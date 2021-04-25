package ken.network;

import ken.util.Utils;

import java.util.List;

/**
 * Created by lbj23k on 2017/6/2.
 */
public class PredicateInfo implements Comparable<PredicateInfo> {
    private String pmid;
    private String predicate;
    private int year;
    private static List<String> predicateMap;

    static {
        predicateMap = Utils.readLineFile("vocabulary/contentWord.txt");
    }
    public PredicateInfo(String predicate, String pmid, int year) {
        this.pmid = pmid;
        this.year = year;
        if (!predicateMap.contains(predicate)) {
            predicate = "others";
        }
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

    @Override
    public boolean equals(Object that) {
        if (that instanceof PredicateInfo) {
            PredicateInfo p = (PredicateInfo) that;
            return this.predicate.equals(p.predicate);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public int compareTo(PredicateInfo o) {
        if (this.year > o.year) return 1;
        else if (this.year < o.year) return -1;
        return 0;
    }
}
