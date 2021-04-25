package com.example.demo;


import com.example.demo.inputClass.GetEval;
import com.example.demo.inputClass.GetPredicate;
import ken.evaluation.IndexScore;
import ken.network.PredicateInterm;
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
    public Map getPredictRank(@RequestBody GetPredicate getPredicate) {
        String drug = getPredicate.drugName;
        ToraManager manager = new ToraManager();
        Integer displayCount = 20;
        // c ranking
        List<IndexScore> diseaseRank = manager.predictRank(drug);

        // find b through drug (a)
        Map<String, Map<String, Integer>> drugPredicateNeighborCount = manager.findPredicateNeighborsCount(drug);
        Map<String, Map<String, Map<String, Integer>>> IntermediatePredicatwithDisease = new HashMap<>();

        for(IndexScore score : diseaseRank.subList(0, displayCount)) {
//            instMap.get(score.getName())
            // find b through c
            Map<String, Map<String, Integer>> tmp = manager.findPredicateNeighborsCount(score.getName());
            // find the union neighbors
            Set<String> neighbors = tmp.keySet().stream().collect(Collectors.toSet());
            Set<String> neighborsOfDrug = drugPredicateNeighborCount.keySet().stream().collect(Collectors.toSet());
            neighbors.retainAll(neighborsOfDrug);

            // b -> c -> predicate -> integer
            for (String b : neighbors) {
                Map<String, Map<String, Integer>> c_predicate = new HashMap<>();
                c_predicate.put(score.getName(), tmp.get(b));
                if (IntermediatePredicatwithDisease.get(b) == null) {
                    IntermediatePredicatwithDisease.put(b, c_predicate);
                } else {
                    Map<String, Map<String, Integer>> prev_c = IntermediatePredicatwithDisease.get(b);
                    prev_c.put(score.getName(), tmp.get(b));
                    IntermediatePredicatwithDisease.replace(b, prev_c);
                }

            }
            List<String> removeList = new ArrayList<>();
            for(Map.Entry<String, Map<String, Map<String, Integer>>> b_c : IntermediatePredicatwithDisease.entrySet()){
                if(b_c.getValue().keySet().size() > displayCount/3){
                    removeList.add(b_c.getKey());
                }
            }
            for(String rm : removeList){
                IntermediatePredicatwithDisease.remove(rm);
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
    public Map<String, Map<String, Object>> getRankingDetail(@RequestBody GetEval getEval) {
        String drug = "Cobalt";
        ToraManager manager = new ToraManager();
        Integer cnt = 0;
        List<IndexScore> diseaseRank = manager.predictRank(drug);
        Map<String, Map<String, Object>> evaluationScore = new HashMap<>();
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
//    @PostMapping("/test")
//    public String test(@RequestBody GetPredicate test) {
//        return test.drugName;
//    }
}

