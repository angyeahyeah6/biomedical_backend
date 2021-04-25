package ken.labelSys;

import ken.network.PredicateNetwork;
import ken.node.LiteratureNode;
import ken.prepare.MeshConceptObject;
import ken.util.DbConnector;
import ken.util.JDBCHelper;
import ken.util.Utils;
import weka.core.Instances;

import java.util.*;

/**
 * Created by lbj23k on 2017/8/13.
 */
public class DiseaseRanking {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        DiseaseRanking diseaseRanking = new DiseaseRanking();
        diseaseRanking.createGoldenFile("1-Butanol", 2002);
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInSecond = totalTime / (1000);
        System.out.println("Time consumed (in seconds): " + totalTimeInSecond);
    }

    public void createGoldenFile(String drug, int endYear) {
        Set<String> diseases = new LinkedHashSet<>(MeshConceptObject.getDiseaseSeeds());
        LiteratureNode drugPre = new LiteratureNode(drug, 1809, endYear);
        HashMap<String, Integer> meshNameIdMap = MeshConceptObject.getMeshNameIdMap();
        Set<String> cooccurNeighborsPre = drugPre.getCooccurNeighbors().keySet();
        List<String> evalDiseaseLs = new ArrayList<>();
        Map<String, Instances> diseaseMap = new HashMap<>();

        int drugId = meshNameIdMap.get(drug);
        for (String disease : diseases) {
            System.out.println(disease);
            int diseaseId = meshNameIdMap.get(disease);
            if (cooccurNeighborsPre.contains(disease) || intermSize(drugId, diseaseId, endYear) < 5) {
                continue;
            }
            evalDiseaseLs.add(disease);
        }
        for (String evalDisease : evalDiseaseLs) {
            PredicateNetwork predicateNet = new PredicateNetwork(drug, evalDisease, 1809, endYear);
            Utils.writeAttr("dat_file/test/123.arff", predicateNet.getInst());
            diseaseMap.put(evalDisease, predicateNet.getInst());
        }
        Utils.writeObject(diseaseMap, "dat_file/test/" + drug + ".dat");
    }

    private int intermSize(int drugId, int diseaseId, int endYear) {
        String sql = "SELECT 1 FROM mesh_predication_aggregate " +
                "WHERE s_mesh_id=? and o_mesh_id in " +
                "(SELECT s_mesh_id FROM mesh_predication_aggregate WHERE o_mesh_id=? and year between 1809 and " +
                endYear + ") " + "and year between 1809 and " + endYear;
//        System.out.println(sql);
        Object[] params = {drugId, diseaseId};
        List<Map<String, Object>> result = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql, params);
        return result.size();
    }
}
