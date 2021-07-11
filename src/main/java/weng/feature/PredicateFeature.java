package weng.feature;

import com.google.common.collect.Sets;
import weng.network.PredictInfo;
import weng.node.LiteratureNode;

import java.io.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PredicateFeature {
    /**
     * TOTAL_MEDLINE_JOURNAL
     * => publication year between 1809-2004 from SemMed_ver26 citations table
     * => Sql: SELECT COUNT(*) FROM `semmed_ver26`.`citations` WHERE `PYEAR` BETWEEN 1809 AND 2004;
     */
    private static final int TOTAL_MEDLINE_JOURNAL = 16174990;

    public PredicateFeature() {
    }

    public static double normalizedMedlineSimilarity(LiteratureNode nodeX, LiteratureNode nodeY, Connection connLit) {
        // something like this: <1016720,136114>
        final HashSet<String> xDocuments = nodeX.getDocuments(connLit);
        final HashSet<String> yDocuments = nodeY.getDocuments(connLit);
        int occurX = nodeX.df(connLit), occurY = nodeY.df(connLit);
        Set<String> interSet = Sets.intersection(xDocuments, yDocuments);
        int cooccurXY = interSet.size();

        if (cooccurXY == 0) return 0;
        double dividend = Math.max(Math.log(occurX), Math.log(occurY)) - Math.log(cooccurXY);
        double divisor = Math.log(TOTAL_MEDLINE_JOURNAL) - Math.min(Math.log(occurX), Math.log(occurY));
        double distance = dividend / divisor;

        if (distance > 1.0) {
            distance = 1.0;
        }
        return 1 - distance;
    }

    public static double jaccardSimilarity(int x, int y, int intersection) {
        int union = x + y - intersection;
        double result = (double) intersection / union;
        if (result == Double.NaN) {
            result = 0.0;
        }
        return result;
    }

//    public static double jaccardSimilarity(LiteratureNode nodeX, LiteratureNode nodeY, Connection connLit) {
//        final Set<String> xDocuments = nodeX.getDocuments(connLit);
//        final Set<String> yDocuments = nodeY.getDocuments(connLit);
//        Set<String> interSet = Sets.intersection(xDocuments, yDocuments);
//        int intersection = interSet.size();
//        int union = xDocuments.size() + yDocuments.size() - intersection;
//
//        double result = (double) intersection / union;
//        if (result == Double.NaN) {
//            result = 0.0;
//        }
//        return result;
//    }

    public HashMap<String, Integer> getPredicateFreq(ArrayList<PredictInfo> relations) throws IOException {
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

    public HashMap<String, Float> getPredicateImportance(String predicateInfoFilePath,
                                                         float predicateMiddleWeight) throws IOException {
        HashMap<String, Float> predicateImportances = new HashMap<>();
        File predicateInfos = new File(predicateInfoFilePath);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(predicateInfos)));
        String line;
        bufferedReader.readLine();
        while ((line = bufferedReader.readLine()) != null) {
            String[] predicateInfo = line.split(",");
            String predicate = predicateInfo[0];
            String predicateImportance = predicateInfo[2];
            float predicateWeight = setRelationWeight(predicate, predicateImportance, predicateMiddleWeight);
            predicateImportances.put(predicate, predicateWeight);
        }
        return predicateImportances;
    }

    public float setRelationWeight(String predicate, String predicateImportance, float predicateMiddleWeight) {
        float predicateWeight;
        if (predicateMiddleWeight == 1) { // 先不管 predicate 的重要程度
            predicateWeight = 1.0f;
            if (predicateImportance.equals("very important")) {
                predicateWeight = 1.0f;
            } else if (predicateImportance.equals("important")) {
                predicateWeight = 1.0f;
            }

//        // Exception rules
//        if (predicate.toLowerCase().equals("treats")) {
//            predicateWeight = 1.2f;
//        }
        } else { // 考慮 predicate 的重要程度
            predicateWeight = 0.0f;
            if (predicateImportance.equals("very important")) {
                predicateWeight = 1.0f;
            } else if (predicateImportance.equals("important")) {
                predicateWeight = predicateMiddleWeight;
            }

//        // Exception rules
//        if (predicate.toLowerCase().equals("treats")) {
//            predicateWeight = 1.2f;
//        }
        }
        return predicateWeight;
    }

    public HashMap<String, String> getPredicateESs(String predicateInfoFilePath) throws IOException {
        HashMap<String, String> predicateESs = new HashMap<>();
        File predicateInfos = new File(predicateInfoFilePath);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(predicateInfos)));
        String line;
        bufferedReader.readLine();
        while ((line = bufferedReader.readLine()) != null) {
            String[] predicateInfo = line.split(",");
            String predicate = predicateInfo[0];
            String ESClass = predicateInfo[1];
            predicateESs.put(predicate, ESClass);
        }
        return predicateESs;
    }

    public String ESClassifier(HashMap<String, Float> ESClassWeight,
                               float ESRelativeFreqThreshold) {
        float escalatingWeight = ESClassWeight.get("escalating");
        float suppressingWeight = ESClassWeight.get("suppressing");
        float undeterminedWeight = ESClassWeight.get("undetermined");
        String ESClass = determineESClass(escalatingWeight, suppressingWeight, undeterminedWeight, ESRelativeFreqThreshold);
        return ESClass;
    }

    public String determineESClass(float escalatingWeight, float suppressingWeight,
                                   float undeterminedWeight, float ESRelativeFreqThreshold) {
        // Method: Min/Max Ratio
        String ESClass;
        float maxWeight = Math.max(escalatingWeight, suppressingWeight);
        float minWeight = Math.min(escalatingWeight, suppressingWeight);
        if (maxWeight != 0) {
            float ESScore = minWeight / maxWeight;
            if (ESScore <= ESRelativeFreqThreshold) {
                if (escalatingWeight > suppressingWeight) {
                    ESClass = "escalating";
                } else {
                    ESClass = "suppressing";
                }
            } else {
                ESClass = "ambiguous";
            }
        } else {
            if (undeterminedWeight != 0) {
                ESClass = "undetermined";
            } else {
                ESClass = "unimportant";
            }
        }
        return ESClass;
    }

    public HashMap<String, Float> setAllESClass(HashMap<String, Float> ESClassWeight) {
        Set<String> ESClasses = ESClassWeight.keySet();
        if (!ESClasses.contains("escalating")) {
            ESClassWeight.put("escalating", 0.0f);
        }
        if (!ESClasses.contains("suppressing")) {
            ESClassWeight.put("suppressing", 0.0f);
        }
        if (!ESClasses.contains("ambiguous")) {
            ESClassWeight.put("ambiguous", 0.0f);
        }
        if (!ESClasses.contains("undetermined")) {
            ESClassWeight.put("undetermined", 0.0f);
        }
        if (!ESClasses.contains("unimportant")) {
            ESClassWeight.put("unimportant", 0.0f);
        }
        return ESClassWeight;
    }

    public HashMap<String, Float> getESClassWeight(HashMap<String, Integer> predicateFreq,
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
}
