package ken.model;

import ken.evaluation.IndexScore;
import ken.network.PredicateNetwork;
import ken.prepare.MeshConceptObject;
import ken.util.DbConnector;
import ken.util.JDBCHelper;
import ken.util.Utils;
import weka.core.Instances;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by lbj23k on 2017/6/1.
 */
public class LearningModel {
    Map<String, Map<String, IndexScore>> perfectRank;
    List<String> drugs500;
    private static Logger logger = Logger.getLogger("LearningModel");

    public static void main(String[] args) {


        logger.info("START....");
        LearningModel model = new LearningModel();
//        model.baseLine();
        model.runModel();
        logger.info("END....");
    }

    public LearningModel() {
        String rankPath = "dat_file/pre/goldenRank.dat";
        perfectRank = (Map<String, Map<String, IndexScore>>) Utils.readObjectFile(rankPath);
        drugs500 = Utils.readLineFile("vocabulary/allDrugs_seed.txt");
    }

    /*
        run model
        output: dat_file/ndcg/svm_feature_selection.dat (e.g.)
        data structure: Map<String (drug), List<String (ndcg score for diseases)>
     */
    public void runModel() {
        Map<String, List<String>> ndcgMap = new HashMap<>();
        int count = 0;
        List<String> drugs = new ArrayList<>(perfectRank.keySet());
        for (String drug : drugs) {
            logger.info(String.valueOf(++count) + ":" + drug);
            Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);
            List<IndexScore> perfectScoreLs = new ArrayList<>();
            List<IndexScore> evalScoreLs = new ArrayList<>();
            int diseaseC = 0;
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
                item.setFeature(predicateNet.getImportCount(),predicateNet.getExpdectProb());
                item.setRealScore(perfectItem.getRealScore());
                evalScoreLs.add(item);
                perfectScoreLs.add(perfectItem);
            }
            Collections.sort(evalScoreLs, Collections.reverseOrder(new IndexScore.FeatureComparator()));
//            System.out.println("stop...");
            Utils.writeObject(ndcgMap, "dat_file/tmp/tmp_test.dat");
//            Collections.sort(perfectScoreLs, Collections.reverseOrder(new IndexScore.ScoreComparator()));
//            List<String> ndcg = getNdcg(perfectScoreLs, evalScoreLs);
//            ndcgMap.put(drug, ndcg);
        }

//        Utils.writeObject(ndcgMap, "dat_file/ndcg/svm_feature_selection.dat");
    }

    /*
        run model for benchmark method
        output: dat_file/ndcg/ltc_amw.dat
        data structure: Map<String (drug), List<String (ndcg score for diseases)>
     */
    private void baseLine() {
        Map<String, List<String>> ndcgMap = new HashMap<>();
        int count = 0;
        List<String> drugs = new ArrayList<>(perfectRank.keySet());
        for (String drug : drugs) {
            logger.info(String.valueOf(++count) + ":" + drug);
            Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);
            List<IndexScore> perfectScoreLs = new ArrayList<>();
            List<IndexScore> evalScoreLs = new ArrayList<>();
            Map<String, Instances> instMap = (Map<String, Instances>)
                    Utils.readObjectFile("dat_file/instMap/" + drug + ".dat");
            for (String disease : perfectDrugRank.keySet()) {
                PredicateNetwork predicateNet = new PredicateNetwork(drug, disease, instMap.get(disease));
                IndexScore item = new IndexScore(disease);
                IndexScore perfectItem = perfectDrugRank.get(disease);
                item.setFeature(predicateNet.getLTC(), predicateNet.getAMW());
                item.setRealScore(perfectItem.getRealScore());
                evalScoreLs.add(item);
                perfectScoreLs.add(perfectItem);
            }
            Collections.sort(evalScoreLs, Collections.reverseOrder(new IndexScore.FeatureComparator()));
            Collections.sort(perfectScoreLs, Collections.reverseOrder(new IndexScore.ScoreComparator()));
            List<String> ndcg = getNdcg(perfectScoreLs, evalScoreLs);
            ndcgMap.put(drug, ndcg);
        }
        Utils.writeObject(ndcgMap, "dat_file/ndcg/ltc_amw.dat");
    }

    /*
        run model for benchmark2 method (cooccurrence)
        output: dat_file/ndcg/ltc_amw_cooccur.dat
        data structure: Map<String (drug), List<String (ndcg score for diseases)>
     */
    private void baseLine2() {
        Map<String, List<String>> ndcgMap = new HashMap<>();
        int count = 0;
        List<String> drugs = new ArrayList<>(perfectRank.keySet());
        for (String drug : drugs) {
            logger.info(String.valueOf(++count) + ":" + drug);
            Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);
            List<IndexScore> perfectScoreLs = new ArrayList<>();
            List<IndexScore> evalScoreLs = new ArrayList<>();
            Map<String, List<String>> ltcMap =
                    (Map<String, List<String>>) Utils.readObjectFile("dat_file/cooccur/" + drug + ".dat");
            for (String disease : perfectDrugRank.keySet()) {
                int ltc = ltcMap.get(disease).size();
                double amw = 0.0;
                for (String inst : ltcMap.get(disease)) {
                    String[] item = inst.split(",");
                    amw += Math.min(Integer.valueOf(item[0]), Integer.valueOf(item[1]));
                }
                amw /= ltc;
                IndexScore item = new IndexScore(disease);
                IndexScore perfectItem = perfectDrugRank.get(disease);
                item.setFeature(ltc, amw);
                item.setRealScore(perfectItem.getRealScore());
                evalScoreLs.add(item);
                perfectScoreLs.add(perfectItem);
            }
            Collections.sort(evalScoreLs, Collections.reverseOrder(new IndexScore.FeatureComparator()));
            Collections.sort(perfectScoreLs, Collections.reverseOrder(new IndexScore.ScoreComparator()));
            List<String> ndcg = getNdcg(perfectScoreLs, evalScoreLs);
            ndcgMap.put(drug, ndcg);
        }
        Utils.writeObject(ndcgMap, "dat_file/ndcg/ltc_amw_cooccur.dat");
    }

    public static List<String> getNdcg(List<IndexScore> perfeckLs, List<IndexScore> evalLs) {
        List<String> ndcg = new ArrayList<>();
        double idcg = 0, dcg = 0;
        for (int i = 0; i < perfeckLs.size(); i++) {
            int rank = i + 1;
            IndexScore perfectItem = perfeckLs.get(i);
            IndexScore evalItem = evalLs.get(i);
            int evalScore = evalItem.getRealScore();
            int perfectScore = perfectItem.getRealScore();
            dcg += (Math.pow(2, evalScore) - 1.0) * (Math.log(2) / Math.log(rank + 1));
            idcg += (Math.pow(2, perfectScore) - 1.0) * (Math.log(2) / Math.log(rank + 1));
            if (rank % 5 == 0) {
                ndcg.add(String.valueOf(dcg / idcg));
            }
        }
        return ndcg;
    }
}
