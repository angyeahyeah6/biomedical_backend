package weng.util.calculator;

import weng.label.LabeledInfo;
import weng.network.PredictInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class PathEsClassCalc {
    public static HashMap<String, Float> labeledWeight(HashMap<String, String> es,
                                                HashMap<String, Float> importance,
                                                ArrayList<LabeledInfo> relations) {
        HashMap<String, Integer> freq = labeledPredicateFreq(relations);
        HashMap<String, Float> esWeight = esWeight(freq, es, importance);
        return allESWeight(esWeight);
    }

    public static HashMap<String, Integer> labeledPredicateFreq(ArrayList<LabeledInfo> relations) {
        // savePossibleMeshFile as: {predicate, frequency}
        HashMap<String, Integer> predicateFreq = new HashMap<>();
        for (LabeledInfo relation : relations) {
            String predicate = relation.getPredicate();
            if (predicateFreq.keySet().contains(predicate)) {
                int frequency = predicateFreq.get(predicate);
                frequency += 1;
                predicateFreq.put(predicate, frequency);
            } else {
                predicateFreq.put(predicate, 1);
            }
        }
        return predicateFreq;
    }

    public static HashMap<String, Float> predictWeight(HashMap<String, String> es,
                                                       HashMap<String, Float> importance,
                                                       ArrayList<PredictInfo> relations) {
        HashMap<String, Integer> freq = predictPredicateFreq(relations);
        HashMap<String, Float> esWeight = esWeight(freq, es, importance);
        return allESWeight(esWeight);
    }

    public static HashMap<String, Integer> predictPredicateFreq(ArrayList<PredictInfo> relations) {
        // savePossibleMeshFile as: {predicate, frequency}
        HashMap<String, Integer> predicateFreq = new HashMap<>();
        for (PredictInfo relation : relations) {
            String predicate = relation.getPredicate();
            if (predicateFreq.keySet().contains(predicate)) {
                int frequency = predicateFreq.get(predicate);
                frequency += 1;
                predicateFreq.put(predicate, frequency);
            } else {
                predicateFreq.put(predicate, 1);
            }
        }
        return predicateFreq;
    }

    public static HashMap<String, Float> esWeight(HashMap<String, Integer> predicateFreq,
                                                  HashMap<String, String> predicateESs,
                                                  HashMap<String, Float> predicateImportance) {
        HashMap<String, Float> weightESClass = new HashMap<>();
        for (String predicate : predicateFreq.keySet()) {
            int freq = predicateFreq.get(predicate);
            String ESClass = predicateESs.get(predicate);
            float importance = predicateImportance.get(predicate);
            float predicateWeight = importance * freq;
            if (weightESClass.keySet().contains(ESClass)) {
                predicateWeight += weightESClass.get(ESClass);
            }
            weightESClass.put(ESClass, predicateWeight);
        }
        return weightESClass;
    }

    public static HashMap<String, Float> allESWeight(HashMap<String, Float> ESClassWeight) {
        Set<String> ESClasses = ESClassWeight.keySet();
        if (!ESClasses.contains("escalating")) {
            ESClassWeight.put("escalating", 0.0f);
        }
        if (!ESClasses.contains("suppressing")) {
            ESClassWeight.put("suppressing", 0.0f);
        }
        if (!ESClasses.contains("undetermined")) {
            ESClassWeight.put("undetermined", 0.0f);
        }
        if (!ESClasses.contains("unimportant")) {
            ESClassWeight.put("unimportant", 0.0f);
        }
        return ESClassWeight;
    }
}
