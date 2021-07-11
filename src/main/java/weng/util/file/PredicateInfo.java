package weng.util.file;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class PredicateInfo {
    public static final String filePath = "dat_file/ESClassification/ESImportanceList_61Predicate.csv";
//    public static final String filePath = "dat_file/ESClassification/ESImportanceList_19Predicate.csv";
//    public static final String filePath = "dat_file/ESClassification/ESImportanceList_only12ImpotantPredicate.csv";

    public static HashMap<String, Float> predicateImportance(float predicateMiddleWeight) throws IOException {
        HashMap<String, Float> predicateImportances = new HashMap<>();
        File predicateInfos = new File(filePath);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(predicateInfos)));
        String line;
        bufferedReader.readLine();
        while ((line = bufferedReader.readLine()) != null) {
            String[] predicateInfo = line.split(",");
            String predicate = predicateInfo[0];
            String predicateImportance = predicateInfo[2];
            float predicateWeight = setRelationWeight(predicateImportance, predicateMiddleWeight);
            predicateImportances.put(predicate, predicateWeight);
        }
        return predicateImportances;
    }

    public static float setRelationWeight(String predicateImportance, float predicateMiddleWeight) {
        float predicateWeight;
        if (predicateMiddleWeight == 1) { // 先不管 predicate 的重要程度
            predicateWeight = 1.0f;
            if (predicateImportance.equals("very important")) {
                predicateWeight = 1.0f;
            } else if (predicateImportance.equals("important")) {
                predicateWeight = 1.0f;
            }
        } else { // 考慮 predicate 的重要程度
            predicateWeight = 0.0f;
            if (predicateImportance.equals("very important")) {
                predicateWeight = 1.0f;
            } else if (predicateImportance.equals("important")) {
                predicateWeight = predicateMiddleWeight;
            }
        }
        return predicateWeight;
    }

    public static HashMap<String, String> predicateESs() {
        HashMap<String, String> predicateESs = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            List<String> l = br.lines().collect(Collectors.toList());
            for (int i = 1; i < l.size(); i++) {
                String[] predicateInfo = l.get(i).split(",");
                String predicate = predicateInfo[0];
                String ESClass = predicateInfo[1];
                predicateESs.put(predicate, ESClass);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return predicateESs;
    }
}
