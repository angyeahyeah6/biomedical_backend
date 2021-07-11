package weng.util.predict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Path {
    private String intermediateName;
    private String targetName;
    private Map<String, Integer> Predicates;

    public Path(String intermediateName, String targetName){
        this.intermediateName = intermediateName;
        this.targetName = targetName;
        this.Predicates = new HashMap<>();
    }
    public void addPredicate(String predicate){

        if (!this.Predicates.keySet().contains(predicate)){
            this.Predicates.put(predicate, 1);
        }
        else{
            this.Predicates.compute(predicate, (key, value) -> value += 1);
        }
    }

    public void setPredicates(Map<String, Integer> predicates) {
        Predicates = predicates;
    }

    public String getIntermediateName() {
        return intermediateName;
    }

    public String getTargetName() {
        return targetName;
    }

    public Map<String, Integer> getPredicates() {
        return Predicates;
    }
}
