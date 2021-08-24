package website.api;

import weng.labelSys.Predict;

import java.sql.SQLException;

public class ToraManager {
    public static void main(String[] args) throws SQLException {
//        String drug = "Metformin";
//        int endYear = 2007;
//        int classifierType = 0;
//        String disease = "Malignant neoplasm of breast";
//        int topK = 10;
//        Predict predict = new Predict(
//                drug,
//                endYear,
//                topK,
//                classifierType);
//        predict.getDetailPath(drug, endYear, disease);
        String drug = "sildenafil";
        int endYear = 1999;
        int classifierType = 0;
//        String disease = "Neoplasm Metastasis";
        int topK = 30;
        Predict predict = new Predict(
                drug,
                endYear,
                topK,
                classifierType);
//        predict.run();
        predict.getCompletePath(drug, endYear);

    }
}