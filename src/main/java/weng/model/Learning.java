package weng.model;

import weka.core.Instances;
import weng.evaluation.IndexScore;
import weng.network.PredictNetwork;
import weng.util.Utils;
import weng.util.file.RepurposingEval;
import weng.util.noSqlData.PredicationCountInfo;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by lbj23k on 2017/6/1.
 */
public class Learning implements Serializable {
    private static final long serialVersionUID = 689123320491080199L;
    private Map<String, Map<String, IndexScore>> perfectRank;
    private List<String> drugs500;
    private int diseaseCount;

    public Learning(int startFile, int endFile, int diseaseCount) {
        this.perfectRank = perfectRank(startFile, endFile);
        this.drugs500 = new Utils().readLineFile(RepurposingEval.focal500Drugs);
        this.diseaseCount = diseaseCount;
    }

    public void setDiseaseCount(int diseaseCount) {
        this.diseaseCount = diseaseCount;
    }

    public void setPerfectRank(Map<String, Map<String, IndexScore>> perfectRank) {
        this.perfectRank = perfectRank;
    }

    public void setDrugs500(List<String> drugs500) {
        this.drugs500 = drugs500;
    }

    public int getDiseaseCount() {
        return diseaseCount;
    }

    public Map<String, Map<String, IndexScore>> getPerfectRank() {
        return perfectRank;
    }

    public List<String> getDrugs500() {
        return drugs500;
    }

    public Map<String, Map<String, IndexScore>> perfectRank(int startFile, int endFile) {
        Map<String, Map<String, IndexScore>> perfectRank = new HashMap<>();
        for (int i = startFile; i <= endFile; i++) {
            int fileNum = 500 * i;
            if (new File(RepurposingEval.goldenRankSplit_noSql + fileNum + ".dat").exists()) {
                perfectRank.putAll((Map<String, Map<String, IndexScore>>) new Utils().readObjectFile(RepurposingEval.goldenRankSplit_noSql + fileNum + ".dat"));
            }
        }
        return perfectRank;
    }

    public void runModel(boolean runOldModel, int classifierType,
                         Connection connSem, Connection connLit,
                         int diseaseCount) throws SQLException {
        Map<String, List<String>> ndcgMap = new HashMap<>();
        int count = 0;
        List<String> drugs = new ArrayList<>(perfectRank.keySet());
        Logger logger = Logger.getLogger("Learning");
        logger.info("[runModel] Start");
        System.out.println(drugs.size());
        String rankingMethod = "";
        String features = "";
        for (String drug : drugs) {
            logger.info(++count + ":" + drug);
            Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);
            List<IndexScore> perfectScoreLs = new ArrayList<>();
            List<IndexScore> evalScoreLs = new ArrayList<>();
            if (new File(RepurposingEval.instance_newFeatureTo100 + drug + ".dat").exists()) {
                Map<String, Instances> instMap = (Map<String, Instances>) new Utils().readObjectFile(RepurposingEval.instance_newFeatureTo100 + drug + ".dat");
                for (String disease : perfectDrugRank.keySet()) {
                    Instances inst = instMap.get(disease);
                    if (inst != null) {
                        PredictNetwork predicateNet = new PredictNetwork(runOldModel, classifierType, drug, disease, inst, connSem, connLit);
                        Instances data = predicateNet.getDataUnlabeled();
                        predicateNet.setDataUnlabeled(predicateNet.takeOffSemanticGroupInsts(data));
                        features = "onlyStat";

                        predicateNet.predictDistribution();
                        IndexScore item = new IndexScore(disease);
                        IndexScore perfectItem = perfectDrugRank.get(disease);

                        item.setFeature(predicateNet.getExpectedProb());
                        rankingMethod = "ExpectedProb";

                        item.setRealScore(perfectItem.getRealScore());
                        evalScoreLs.add(item);
                        perfectScoreLs.add(perfectItem);
                    }
                }
                evalScoreLs.sort(Collections.reverseOrder(new IndexScore.FeatureComparator()));
                perfectScoreLs.sort(Collections.reverseOrder(new IndexScore.ScoreComparator()));
                List<String> ndcg = getNdcg(perfectScoreLs, evalScoreLs);
                ndcgMap.put(drug, ndcg);
            } else {
                logger.info(drug + "'s instMap file is not exist!");
            }
        }

        switch (classifierType) {
            case 0:
                new Utils().writeObject(ndcgMap, RepurposingEval.datFileNdcgDirectory + diseaseCount + RepurposingEval.ndcgLROriginalFeature_diseaseCount + rankingMethod + "_" + features + "_24.dat");
                break;
            case 1:
                new Utils().writeObject(ndcgMap, RepurposingEval.datFileNdcgDirectory + diseaseCount + RepurposingEval.ndcgNBOriginalFeature_diseaseCount + rankingMethod + "_" + features + "_24.dat");
                break;
            case 2:
                new Utils().writeObject(ndcgMap, RepurposingEval.datFileNdcgDirectory + diseaseCount + RepurposingEval.ndcgRFOriginalFeature_diseaseCount + rankingMethod + "_" + features + "_24.dat");
                break;
            case 3:
                new Utils().writeObject(ndcgMap, RepurposingEval.datFileNdcgDirectory + diseaseCount + RepurposingEval.ndcgSVMOriginalFeature_diseaseCount + rankingMethod + "_" + features + "_24.dat");
                break;
        }
        logger.info("[runModel] Finish");
    }

    public void runOldFeatureModel(boolean runOldModel, int classifierType,
                                   Connection connSem, Connection connLit,
                                   int diseaseCount) throws SQLException {
        Map<String, List<String>> ndcgMap = new HashMap<>();
        int count = 0;
        List<String> drugs = new ArrayList<>(perfectRank.keySet());
        Logger logger = Logger.getLogger("Learning");
        logger.info("[runOldFeatureModel] Start");
        String rankingMethod = "";
        String features = "";
        System.out.println("total drugs: " + drugs.size());
        for (String drug : drugs) {
            logger.info(++count + ":" + drug);
            Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);
            List<IndexScore> perfectScoreLs = new ArrayList<>();
            List<IndexScore> evalScoreLs = new ArrayList<>();
            if (new File(RepurposingEval.instance_oldFeatureTo100 + drug + ".dat").exists()) {
                Map<String, Instances> instMap = (Map<String, Instances>) new Utils().readObjectFile(RepurposingEval.instance_oldFeatureTo100 + drug + ".dat");
                for (String disease : perfectDrugRank.keySet()) {
                    Instances inst = instMap.get(disease);
                    if (inst != null) {
                        PredictNetwork predicateNet = new PredictNetwork(runOldModel, classifierType, drug, disease, inst, connSem, connLit);
                        features = "onlyStat";

                        predicateNet.predictDistribution();
                        IndexScore item = new IndexScore(disease);
                        IndexScore perfectItem = perfectDrugRank.get(disease);

                        item.setFeature(predicateNet.getExpectedProb());
                        rankingMethod = "ExpectedProb";

                        item.setRealScore(perfectItem.getRealScore());
                        evalScoreLs.add(item);
                        perfectScoreLs.add(perfectItem);
                    }
                }
                evalScoreLs.sort(Collections.reverseOrder(new IndexScore.FeatureComparator()));
                perfectScoreLs.sort(Collections.reverseOrder(new IndexScore.ScoreComparator()));
                List<String> ndcg = getNdcg(perfectScoreLs, evalScoreLs);
                ndcgMap.put(drug, ndcg);
            }
        }

        switch (classifierType) {
            case 0:
                new Utils().writeObject(ndcgMap, RepurposingEval.datFileNdcgDirectory + diseaseCount + RepurposingEval.ndcgLROldFeature_diseaseCount + rankingMethod + "_" + features + "_24.dat");
                break;
            case 1:
                new Utils().writeObject(ndcgMap, RepurposingEval.datFileNdcgDirectory + diseaseCount + RepurposingEval.ndcgNBOldFeature_diseaseCount + rankingMethod + "_" + features + "_24.dat");
                break;
            case 2:
                new Utils().writeObject(ndcgMap, RepurposingEval.datFileNdcgDirectory + diseaseCount + RepurposingEval.ndcgRFOldFeature_diseaseCount + rankingMethod + "_" + features + "_24.dat");
                break;
            case 3:
                new Utils().writeObject(ndcgMap, RepurposingEval.datFileNdcgDirectory + diseaseCount + RepurposingEval.ndcgSVMOldFeature_diseaseCount + rankingMethod + "_" + features + "_24.dat");
                break;
        }
        logger.info("[runOldFeatureModel] Finish");
    }

    public void baseLine(boolean runOldModel, Connection connSem, Connection connLit,
                         List<PredicationCountInfo> predicationCounts,
                         int diseaseCount) throws SQLException {
        Map<String, List<String>> ndcgMap = new HashMap<>();
        int count = 0;
        List<String> drugs = new ArrayList<>(perfectRank.keySet());
        Logger logger = Logger.getLogger("Learning");
        logger.info("[baseLine] Start");
        System.out.println("total drugs: " + drugs.size());
        for (String drug : drugs) {
            logger.info(String.valueOf(++count) + ":" + drug);
            Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);
            List<IndexScore> perfectScoreLs = new ArrayList<>();
            List<IndexScore> evalScoreLs = new ArrayList<>();
            if (new File(RepurposingEval.instance_oldFeatureTo100 + drug + ".dat").exists()) {
                Map<String, Instances> instMap = (Map<String, Instances>) new Utils().readObjectFile(RepurposingEval.instance_oldFeatureTo100 + drug + ".dat");
                for (String disease : perfectDrugRank.keySet()) {
                    Instances inst = instMap.get(disease);
                    if (inst != null) {
                        PredictNetwork predicateNet = new PredictNetwork(runOldModel, drug, disease, inst, connSem, connLit);
                        IndexScore item = new IndexScore(disease);
                        IndexScore perfectItem = perfectDrugRank.get(disease);
                        item.setFeature(predicateNet.getLTC(), predicateNet.getAMW(predicationCounts));
                        item.setRealScore(perfectItem.getRealScore());
                        evalScoreLs.add(item);
                        perfectScoreLs.add(perfectItem);
                    }
                }
                evalScoreLs.sort(Collections.reverseOrder(new IndexScore.FeatureComparator()));
                perfectScoreLs.sort(Collections.reverseOrder(new IndexScore.ScoreComparator()));
                List<String> ndcg = getNdcg(perfectScoreLs, evalScoreLs);
                ndcgMap.put(drug, ndcg);
            }
        }
        new Utils().writeObject(ndcgMap, RepurposingEval.datFileNdcgDirectory + diseaseCount + RepurposingEval.ndcgLtcAmw);
        logger.info("[baseLine] Finish");
    }

    public static List<String> getNdcg(List<IndexScore> perfectLs, List<IndexScore> evalLs) {
        List<String> ndcg = new ArrayList<>();
        double idcg = 0, dcg = 0;
        for (int i = 0; i < perfectLs.size(); i++) {
            int rank = i + 1;
            IndexScore perfectItem = perfectLs.get(i);
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
