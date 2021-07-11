package weng.util.predict;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class DiseaseScore implements Comparable<DiseaseScore>{
    private String name;
    private Double score;
    public DiseaseScore(String name, Double score){
        this.name = name;
        this.score = score;
    }
    @Override
    public int compareTo(DiseaseScore o) {
        if (this.score >= o.score){
            return -1;
        }
        else {
            return 1;
        }
    }

    public String getName() {
        return name;
    }

    public Double getScore() {
        return score;
    }
}
