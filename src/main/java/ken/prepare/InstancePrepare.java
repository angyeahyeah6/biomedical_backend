package ken.prepare;

import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import ken.evaluation.IndexScore;
import ken.network.PredicateInterm;
import ken.network.PredicateNetwork;
import ken.node.LiteratureNode;
import ken.util.DbConnector;
import ken.util.JDBCHelper;
import ken.util.Utils;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import weka.core.Instances;

import java.util.*;

/**
 * Created by lbj23k on 2017/6/22.
 */
public class InstancePrepare {
    public static void main(String[] args) {
//        createAllEvalrank();
//        createGoldenFile();
        createInstFile();
    }

    /*  create attribute file from 500 focal drug
        output: dat_file/instMap/xxx(drug).dat
        data structure: Map<String (disease), Instances (weka data type, storing features)>
     */
    public static void createInstFile() {
        List<String> drugs500 = Utils.readLineFile("vocabulary/allDrugs_seed.txt");
        int count = 0;
        String fileptah = "dat_file/pre/goldenRank.dat";
        Map<String, Map<String, IndexScore>> perfectRank =
                (Map<String, Map<String, IndexScore>>) Utils.readObjectFile(fileptah);
        List<String> drugs = new ArrayList<>(perfectRank.keySet());
        //this run very slow, subList focal drug and run by multiple process is a good idea
//        for(String drug:drugs){
        for (String drug : drugs.subList(10, 11)) {
            System.out.println(++count + ":" + drug);
            Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);
            Map<String, Instances> diseaseMap = new HashMap<>();
            for (String disease : perfectDrugRank.keySet()) {
                PredicateNetwork predicateNet = new PredicateNetwork(drug, disease, 1809, 2015);
                diseaseMap.put(disease, predicateNet.getInst());
            }
            Utils.writeObject(diseaseMap, "dat_file/instMap/" + drug + ".dat");
            diseaseMap = new HashMap<>();
        }

    }
    /*
        create golden set for "all" drug (the drug that has potential for drug repurposing)
        output: dat_file/eval/golden_pairRank.dat
        data structure: LinkedHashMap<String (drug), Map<String (disease), Integer (score)>>
     */
    public static void createGoldenFile() {
        Set<String> diseases = new LinkedHashSet<>(MeshConceptObject.getDiseaseSeeds());
        HashMap<String, LiteratureNode> drugsPost = new HashMap<>();
        HashMap<String, LiteratureNode> allNodePre = LiteratureNode.getAllNodes(1809, 2004);
        LinkedHashMap<String, Map<String, Integer>> goldenMap = new LinkedHashMap<>();
        for (String drug : MeshConceptObject.getDrugSeeds()) {
            drugsPost.put(drug, new LiteratureNode(drug, 2005, 2020));
        }
        int count = 0;
        HashMap<String, Integer> meshNameIdMap = MeshConceptObject.getMeshNameIdMap();
        for (String drug : drugsPost.keySet()) {
            int drugId = meshNameIdMap.get(drug);
            System.out.format("%d:%s\n", count++, drug);
            // 兩個node代表的是同一個drug 但不同時間段的關係
            LiteratureNode nodePre = allNodePre.get(drug);
            LiteratureNode nodePost = drugsPost.get(drug);

            Map<String, Multiset<String>> predicateNeighborPost = nodePost.getPredicateNeighbors();
            Set<String> cooccurNeighborsPre = nodePre.getCooccurNeighbors().keySet();
            Set<String> cooccurNeighborsPost = nodePost.getCooccurNeighbors().keySet();

            // 找到所有與drug有共同出現過的node, 並且只 contained by set1 and not contained by set2.
            // 只在以後出現過，但沒有在以前出現過
            Set<String> evalSet = Sets.difference(cooccurNeighborsPost, cooccurNeighborsPre);

            // 只看disease的部分，evalSet裡面disease的部分
            Set<String> evalDiseaseSet = Sets.intersection(evalSet, diseases);

            Set<String> uselessDiseaseSet = Sets.difference(diseases, evalDiseaseSet);

            for (String disease : evalDiseaseSet) {
                int diseaseId = meshNameIdMap.get(disease);
                // 以前有沒有有關連過
                if (!hasIntermediates(drug, disease, allNodePre))
                    continue;
                //看post去做的條件判斷
                if (predicateNeighborPost.containsKey(disease)) {
                    Multiset<String> diseasePredicate = predicateNeighborPost.get(disease);
                    int diseaseCount = diseasePredicate.size();
                    if (diseasePredicate.count("TREATS") / (double) diseaseCount > 0.5) {
                        Map<String, Integer> rankMap = goldenMap.getOrDefault(drug, new LinkedHashMap<>());
                        rankMap.put(disease, 3);
                        goldenMap.put(drug, rankMap);
                    } else {
                        Map<String, Integer> rankMap = goldenMap.getOrDefault(drug, new LinkedHashMap<>());
                        rankMap.put(disease, 2);
                        goldenMap.put(drug, rankMap);
                    }
                } else {
                    if (cooccurNeighborsPost.contains(disease)) {
                        Map<String, Integer> rankMap = goldenMap.getOrDefault(drug, new LinkedHashMap<>());
                        rankMap.put(disease, 1);
                        goldenMap.put(drug, rankMap);
                    }
                }
            }
            if (goldenMap.containsKey(drug) && goldenMap.get(drug).values().contains(3)) {
                for (String uselessDiease : uselessDiseaseSet) {
                    int diseaseId = meshNameIdMap.get(uselessDiease);
                    if (cooccurNeighborsPre.contains(uselessDiease)) {
                        continue;
                    }
                    if (!hasIntermediates(drug, uselessDiease, allNodePre))
                        continue;
                    Map<String, Integer> rankMap = goldenMap.getOrDefault(drug, new LinkedHashMap<>());
                    rankMap.put(uselessDiease, 0);
                    goldenMap.put(drug, rankMap);
                }
            }
        }
        Utils.writeObject(goldenMap, "dat_file/golden_pairRank.dat");
    }


    /*
        store drug with at least one disease which score = 3
        output: dat_file/eval/golden_treatRank.dat
        data structure: LinkedHashMap<String, Map<String, Integer>>
        (same as golden_pairRank.dat)
     */
    public static void createTreatRankSet() {
        String filepath = "dat_file/eval/golden_pairRank.dat";
        Map<String, Map<String, Integer>> goldenMap =
                (Map<String, Map<String, Integer>>) Utils.readObjectFile(filepath);
        Map<String, Map<String, Integer>> treatMap = new LinkedHashMap<>();
        for (String drug : goldenMap.keySet()) {
            Map<String, Integer> diseaseMap = goldenMap.get(drug);
            //has TREATS predicate => keep it
            if (diseaseMap.values().contains(3)) {
                int zero = Collections.frequency(diseaseMap.values(), 0);
                int valid = diseaseMap.size() - zero;
                // #score > 0 / #total >=0.01 => keep it
                if ((double) valid / diseaseMap.size() >= 0.01) {
                    treatMap.put(drug, diseaseMap);
                }
            }
        }
        System.out.println(treatMap.size());
        Utils.writeObject(treatMap, "dat_file/eval/golden_treatRank.dat");
    }
    /*
        random select 500 drug as drug seeds
        output: dat_file/eval/goldenRank.dat & vocabulary/500drugs_seed.txt
        data structure: LinkedHashMap<String, HashMap<String, IndexScore>>
     */
    public static void createAllEvalrank() {
        String fileptah = "dat_file/pre/golden_treatRank.dat";
//        String fileptah = "dat_file/eval/golden_pairRank2.dat";
        LinkedHashMap<String, HashMap<String, IndexScore>> map = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> goldenMap =
                (Map<String, Map<String, Integer>>) Utils.readObjectFile(fileptah);
        //Water??
        goldenMap.remove("Water");
        List<String> drugs = new ArrayList<>(goldenMap.keySet());
        List<String> selectDrug = new ArrayList<>();
        // shuffle it!!
        Collections.shuffle(drugs, new Random(50));
        // focal drug size
        int seedC = goldenMap.size();
        // disease size
        int diseaseC = 1000;
          for (String drug : drugs) {
            HashMap<String, IndexScore> rankMap = new HashMap<>();
            List<IndexScore> perfectLs = new ArrayList<>();
            Map<String, Integer> diseaseMap = goldenMap.get(drug);
            if (diseaseMap.size() < diseaseC) continue;
            Set<String> hasRankDisease = new HashSet<>();
            for (String disease : diseaseMap.keySet()) {
                int score = diseaseMap.get(disease);
                if (score > 0) hasRankDisease.add(disease);
                IndexScore item = new IndexScore(score, disease);
                perfectLs.add(item);
                if (perfectLs.size() == diseaseC) break;
            }
            if (perfectLs.size() < diseaseC) {
                Set<String> uselessDiseaseSet = Sets.difference(diseaseMap.keySet(), hasRankDisease);
                for (String uselessDisease : uselessDiseaseSet) {
                    int score = diseaseMap.get(uselessDisease);
                    IndexScore item = new IndexScore(score, uselessDisease);
                    perfectLs.add(item);
                    if (perfectLs.size() == diseaseC) break;
                }
            }
            Collections.sort(perfectLs, Collections.reverseOrder(new IndexScore.ScoreComparator()));
            for (IndexScore indexItem : perfectLs) {
                rankMap.put(indexItem.getName(), indexItem);
            }
            map.put(drug, rankMap);
            selectDrug.add(drug);
            if (map.size() == seedC) break;
        }
        System.out.println(map.size());
        Utils.writeObject(map, "dat_file/goldenRank.dat");
        Utils.writeLineFile(selectDrug, "vocabulary/allDrugs_seed.txt");
    }

    /*
        use for cooccurFile
     */
    private void createCooccurFile() {
        String rankPath = "dat_file/eval/goldenRank.dat";
        Map<String, Map<String, IndexScore>> perfectRank =
                (Map<String, Map<String, IndexScore>>) Utils.readObjectFile(rankPath);
        String sql_ab = "SELECT sum(freq) as sf FROM neighbor_cooccur where mesh_id=? and neighbor in" +
                " (select mesh_id from neighbor_cooccur where neighbor=? and year between 1809 and 2004)" +
                " and year between 1809 and 2004 group by neighbor";
        String sql_bc = "SELECT sum(freq) as sf FROM neighbor_cooccur where neighbor=? and mesh_id in" +
                " (select neighbor from neighbor_cooccur where mesh_id=? and year between 1809 and 2004)" +
                " and year between 1809 and 2004 group by mesh_id";
        int count = 0;
        HashMap<String, Integer> meshNameIdMap = MeshConceptObject.getMeshNameIdMap();
        List<String> drugs = new ArrayList<>(perfectRank.keySet());
        for (String drug : drugs) {
            Map<String, List<String>> ltcMap = new HashMap<>();
            System.out.println(++count + ":" + drug);
            int drugId = meshNameIdMap.get(drug);
            Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);
            for (String disease : perfectDrugRank.keySet()) {
                int diseaseId = meshNameIdMap.get(disease);
                Object[] param_ab = {drugId, diseaseId};
                Object[] param_bc = {diseaseId, drugId};
                List<Map<String, Object>> result_ab = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql_ab, param_ab);
                List<Map<String, Object>> result_bc = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql_bc, param_bc);
                List<String> insts = new ArrayList<>();
                for (int i = 0; i < result_ab.size(); i++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(result_ab.get(i).get("sf")).append(",");
                    sb.append(result_bc.get(i).get("sf"));
                    insts.add(sb.toString());
                }
                ltcMap.put(disease, insts);
            }
            Utils.writeObject(ltcMap, "dat_file/cooccur/" + drug + ".dat");
        }
    }

    //not sure which one is fast, you can try on your own
    private static boolean hasIntermediates(String drug, String disease, HashMap<String, LiteratureNode> allNodePre) {
        boolean hasInterm = false;
        for (String interm : allNodePre.get(drug).getPredicateNeighbors().keySet()) {
            for (String intermNeighbor : allNodePre.get(interm).getPredicateNeighbors().keySet()) {
                if (intermNeighbor.equals(disease)) {
                    hasInterm = true;
                    break;
                }
            }
            if (hasInterm) break;
        }
        return hasInterm;
    }

    private static boolean hasIntermediates(int drugId, int diseaseId) {
        String sql = "SELECT 1 FROM mesh_predication_aggregate " +
                "WHERE s_mesh_id=? and o_mesh_id in " +
                "(SELECT s_mesh_id FROM mesh_predication_aggregate WHERE o_mesh_id=? and year between 1809 and 2004) " +
                "and year between 1809 and 2004 limit 1";
        Object[] params = {drugId, diseaseId};
        List<Map<String, Object>> result = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql, params);
        int j = 0;
        return result.size() > 0;
    }
}
