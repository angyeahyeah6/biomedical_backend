package com.example.demo;


import com.example.demo.inputClass.InEval;
import com.example.demo.inputClass.InPredicate;
import ken.evaluation.IndexScore;
import ken.util.Utils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin("http://localhost:8081")
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ToraController {
    @PostMapping("/get_predicate")
    public Map getPredictRank(@RequestBody InPredicate getPredicate) {
        String drug = getPredicate.drugName;
        ToraManager manager = new ToraManager();
        Integer displayCount = 1;
        // c ranking
        List<IndexScore> diseaseRank = manager.predictRank(drug);

        Map<String, Map<String, Map<String, Double>>> IntermediatePredicatwithDisease = new HashMap<>();

        // find intermidiate b
        for(IndexScore score : diseaseRank.subList(0, displayCount)) {
            System.out.println(score.getName());
            Map<String, Map<String, Map<String, Double>>> temp = new HashMap<>();
            // read pre caculate file or recaculate intermidiates
            try {
                String fileptah = "dat_file/intermediateMap/" + drug + "_" + score.getName() + ".dat";
                temp = (Map<String, Map<String, Map<String, Double>>>) Utils.readObjectFile(fileptah);
            } catch (Exception e) {
                temp = ToraManager.getIntermediates(drug, diseaseRank.get(0).getName());
            }

            // merge duplicate b
            for(String b : temp.keySet()){
                if(IntermediatePredicatwithDisease.containsKey(b)){
                    IntermediatePredicatwithDisease.get(b).putAll(temp.get(b));
                }
                else{
                    IntermediatePredicatwithDisease.put(b, temp.get(b));
                }
            }
            IntermediatePredicatwithDisease.remove(score.getName());
        }
        return IntermediatePredicatwithDisease;
    }
    @GetMapping("/all_drug")
    public List<String> getAllDrug() {
        List<String> drugsAll = Utils.readLineFile("vocabulary/allDrugs_seed.txt");
        return drugsAll;
    }
    @PostMapping("/get_eval")
    public Map<String, Map<String, Object>> getRankingDetail(@RequestBody InEval getEval) {
        String drug = getEval.drugName;
        ToraManager manager = new ToraManager();
        Integer cnt = 0;
        List<IndexScore> diseaseRank = manager.predictRank(drug);
        LinkedHashMap<String, Map<String, Object>> evaluationScore = new LinkedHashMap<>();
        for(IndexScore score : diseaseRank){
            Map<String, Object> tmp = new HashMap<>();
            tmp.put("feature1", score.getFeature());
            tmp.put("feature2", score.getFeature2());
            tmp.put("id", cnt.toString());
            cnt += 1;
            evaluationScore.put(score.getName(),tmp);
        }
        return evaluationScore;

    }
}
//    @PostMapping("/test")
//    public String test(@RequestBody GetPredicate test) {
//        return test.drugName;
//    }
//            instMap.get(score.getName())

//    // find b through c
//    Map<String, Map<String, Integer>> tmp = manager.findPredicateNeighborsCount(score.getName());
//    // find the union neighbors
//    Set<String> neighbors = tmp.keySet().stream().collect(Collectors.toSet());
//    Set<String> neighborsOfDrug = drugPredicateNeighborCount.keySet().stream().collect(Collectors.toSet());
//            neighbors.retainAll(neighborsOfDrug);
//
//                    // b -> c -> predicate -> integer
//                    for (String b : neighbors) {
//                    Map<String, Map<String, Integer>> c_predicate = new HashMap<>();
//        c_predicate.put(score.getName(), tmp.get(b));
//        if (IntermediatePredicatwithDisease.get(b) == null) {
//        IntermediatePredicatwithDisease.put(b, c_predicate);
//        } else {
//        Map<String, Map<String, Integer>> prev_c = IntermediatePredicatwithDisease.get(b);
//        prev_c.put(score.getName(), tmp.get(b));
//        IntermediatePredicatwithDisease.replace(b, prev_c);
//        }
//
//        }
//        // remove 1/3
//        List<String> removeList = new ArrayList<>();
//        for(Map.Entry<String, Map<String, Map<String, Integer>>> b_c : IntermediatePredicatwithDisease.entrySet()){
//        if(b_c.getValue().keySet().size() > displayCount/3){
//        removeList.add(b_c.getKey());
//        }
//        }
//        for(String rm : removeList){
//        IntermediatePredicatwithDisease.remove(rm);
//        }

