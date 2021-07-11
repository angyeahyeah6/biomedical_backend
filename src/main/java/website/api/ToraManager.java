package website.api;

import weng.labelSys.Predict;

import java.sql.SQLException;
import java.util.*;

public class ToraManager {
//    Map<String, Map<String, IndexScore>> perfectRank;
//    List<String> allDrugs;
//    private static Logger logger = Logger.getLogger("LearningModel");
    public static void main(String[] args) throws SQLException {
//        ProgressBar pb = new ProgressBar(100);
//        for (int i = 0; i <= 100; i++) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            pb.update();
//        }
        String drug = "Finasteride";
        int endYear = 1993;
        int classifierType = 0;
        String disease = "Neoplasm Metastasis";
        int topK = 30;
        Predict predict = new Predict(
                drug,
                endYear,
                topK,
                classifierType);
        List<List<Object>> a = predict.getDetailPath(drug, endYear, disease);
    }
}