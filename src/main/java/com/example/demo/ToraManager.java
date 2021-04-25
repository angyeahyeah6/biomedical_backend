package com.example.demo;

import com.google.common.collect.Multiset;
import ken.evaluation.IndexScore;
import ken.network.PredicateInterm;
import ken.network.PredicateNetwork;
import ken.node.LiteratureNode;
import ken.prepare.MeshConceptObject;
import ken.util.DbConnector;
import ken.util.JDBCHelper;
import ken.util.Utils;
import weka.core.Instances;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ToraManager {
    Map<String, Map<String, IndexScore>> perfectRank;
//    List<String> allDrugs;
    private static Logger logger = Logger.getLogger("LearningModel");
    public static void main(String[] args) {
        String drug = "Keratin-19";
//        String drug = getEval.drugName;
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
//        return evaluationScore;

    }
    // intermidiates : target : predicate num

    public ToraManager() {
        String rankPath = "dat_file/pre/goldenRank.dat";
        perfectRank = (Map<String, Map<String, IndexScore>>) Utils.readObjectFile(rankPath);
//        allDrugs = Utils.readLineFile("vocabulary/allDrugs_seed.txt");
    }

    /*
        predict rank
        return : a drug's ranking disease with two feature value
        data structure: List<IndexScore>
     */
    public List<IndexScore> predictRank(String drug) {
        Map<String, List<String>> ndcgMap = new HashMap<>();
        List<String> drugs = new ArrayList<>(perfectRank.keySet());
        List<IndexScore> perfectScoreLs = new ArrayList<>();
        List<IndexScore> evalScoreLs = new ArrayList<>();
        int diseaseC = 0;
        try {
            Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);
            Map<String, Instances> instMap = (Map<String, Instances>)
                    Utils.readObjectFile("dat_file/preInstMap/" + drug + ".dat");
            for (String disease : perfectDrugRank.keySet()) {
                PredicateNetwork predicateNet = new PredicateNetwork(drug, disease, instMap.get(disease));
                //TODO setting feature type

                predicateNet.setOnlyStatInsts();
//                predicateNet.setOnlyContentInsts();
//                predicateNet.setOnlyConcept();
//                predicateNet.setOnlyPredicationInsts();
//                predicateNet.setFeatureSelectionInsts();
                predicateNet.predictInterm();
                IndexScore item = new IndexScore(disease);
                IndexScore perfectItem = perfectDrugRank.get(disease);
                //TODO setting ranking method

//                item.setFeature(predicateNet.getExpdectProb());
                item.setFeature(predicateNet.getImportCount(), predicateNet.getExpdectProb());
                item.setRealScore(perfectItem.getRealScore());
                evalScoreLs.add(item);
                perfectScoreLs.add(perfectItem);
            }
            Collections.sort(evalScoreLs, Collections.reverseOrder(new IndexScore.FeatureComparator()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return evalScoreLs;
    }

    /*
        get Intermediates between pivot and target
        return : b -> c -> predicate name -> cnt
        data structure: Map<String, Map<String, Map<String, Double>>>
     */
    public static Map<String, Map<String, Map<String, Double>>> getIntermediates(String pivotName, String targetName) {
        int startYear = 1809;
        int endYear = 2015;

        LiteratureNode pivotNode = new LiteratureNode(pivotName, startYear, endYear);
        LiteratureNode targetNode = new LiteratureNode(targetName, startYear, endYear);

        HashMap<String, LiteratureNode> allNodes = LiteratureNode.getAllNodes(startYear, endYear);

        ArrayList<PredicateInterm> intermediates = new ArrayList<>();
        Set<Integer> neighborSet = new HashSet<>();
        String sql_ab = "SELECT o_mesh_id as neighbor, predicate, pmid, year FROM mesh_predication_aggregate " +
                "WHERE s_mesh_id=? and o_mesh_id in " +
                "(SELECT s_mesh_id FROM mesh_predication_aggregate WHERE o_mesh_id=? and year between ? and ?) " +
                "and year between ? and ?";
        Object[] params_ab = {pivotNode.getMeshId(), targetNode.getMeshId(), startYear, endYear, startYear, endYear};
        List<Map<String, Object>> result_ab = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql_ab, params_ab);

        String sql_bc = "SELECT s_mesh_id as neighbor, predicate, pmid, year FROM mesh_predication_aggregate " +
                "WHERE o_mesh_id=? and s_mesh_id in " +
                "(SELECT o_mesh_id FROM mesh_predication_aggregate WHERE s_mesh_id=? and year between ? and ?) " +
                "and year between ? and ?";

        Object[] params_bc = {targetNode.getMeshId(), pivotNode.getMeshId(), startYear, endYear, startYear, endYear};
        List<Map<String, Object>> result_bc = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql_bc, params_bc);
        for (Map row : result_ab) {
            neighborSet.add((int) row.get("neighbor"));
        }
        for (int neighbor : neighborSet) {
            String neighborName = MeshConceptObject.getMeshIdNameMap().get(neighbor);
            LiteratureNode intermNode = allNodes.get(neighborName);
            PredicateInterm pi = new PredicateInterm(pivotNode, intermNode, targetNode);
            if (pivotNode.getFrequency() == -1 || targetNode.getFrequency() == -1) {
                continue;
            }
            pi.setPredicateInfo(result_ab, result_bc);
            intermediates.add(pi);
        }
        Map<String, Map<String, Map<String, Double>>> intermediacteCnt = new HashMap<>();
        for(PredicateInterm interm : intermediates){
            Map<String, Map<String, Double>> temp = new HashMap<>();
            temp.put(interm.getTargetNodeName(), interm.predicateCountMap);
            intermediacteCnt.put(interm.getIntermNodeName(), temp);
        }
        return intermediacteCnt;
//        Utils.writeObject(intermediacteCnt, "dat_file/intermediateMap/" + pivotName + "_" + targetName + ".dat");
    }


    public static Map<String, Map<String, Integer>> findPredicateNeighborsCount(String subject){
        Map<String, Map<String, Integer>> predicateMap = new HashMap<>();
        LiteratureNode drugNode = new LiteratureNode(subject, 1809, 2004);
//
        // predicateNeighborPost return subject node's all neighbors nodes and the all predicate type between them
        Map<String, Multiset<String>> predicateNeighborPost = drugNode.getPredicateNeighbors();

        Map<String, Map<String, Integer>> returnObject = new HashMap<>();
        for(Map.Entry<String, Multiset<String>> drugB : predicateNeighborPost.entrySet()){
            Map<String, Integer> predicateCount = new HashMap<>();
            for (String unqPredicate : drugB.getValue().elementSet()){
                predicateCount.put(unqPredicate, drugB.getValue().count(unqPredicate));
            }
            returnObject.put(drugB.getKey(), predicateCount);
        }
        return returnObject;
    }

    /*
        find union neighbor between drug and list of disease (find B)
        return : union neighbor
        data structure: List<String>
     */
    public static  List<String> findUnionNeighbors (Set<String> drugNeighbors, List<String> diseaseNeighbors) {
        HashSet<String> unionNeighbors = new HashSet<String>(drugNeighbors);
        unionNeighbors.addAll(diseaseNeighbors);
        return unionNeighbors.stream().collect(Collectors.toList());
    }
    // TODO: // find the union neighbors between subject and targetC

}
