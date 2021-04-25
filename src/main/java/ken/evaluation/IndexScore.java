package ken.evaluation;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by lbj23k on 2017/6/6.
 */
public class IndexScore implements Serializable {
    private static final long serialVersionUID = 8485068331171052471L;
    private int realScore;
    private String name;
    private double feature = 0;
    private double feature2 = 0;

    public IndexScore(int realScore, String name) {
        this.realScore = realScore;
        this.name = name;
    }
    public double getFeature(){return this.feature;}

    public double getFeature2(){return this.feature2;}


    public IndexScore(String name) {
        this.name = name;
    }

    public void setFeature(double feature) {
        this.feature = feature;
    }

    public void setFeature(double feature, double feature2) {
        this.feature = feature;
        this.feature2 = feature2;
    }

    public void setRealScore(int realScore) {
        this.realScore = realScore;
    }

    public String getName() {
        return name;
    }

    public int getRealScore() {
        return realScore;
    }

    @Override
    public String toString() {
        return String.format("name:%s, realScore:%d", name, realScore);
    }

    @Override
    public int hashCode() {
        return name.length();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IndexScore other = (IndexScore) obj;
        return name.equals(other.name);
    }

    public static class ScoreComparator implements Comparator<IndexScore> {
        public int compare(IndexScore p1, IndexScore p2) {
            if (p1.realScore > p2.realScore) return 1;
            else if (p1.realScore < p2.realScore) return -1;
            else {
                return p1.name.compareTo(p2.name);
            }
        }
    }

    public static class FeatureComparator implements Comparator<IndexScore> {
        public int compare(IndexScore p1, IndexScore p2) {
            if (p1.feature > p2.feature) return 1;
            else if (p1.feature < p2.feature) return -1;
            else {
                if (p1.feature2 > p2.feature2) return 1;
                else if (p1.feature2 < p2.feature2) return -1;
                else
                    return p1.name.compareTo(p2.name);
            }
        }
    }

}
