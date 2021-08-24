package weng;

import weng.labelSys.Predict;

import java.sql.SQLException;

public class createPredictCase {
    public static void main(String[] args) throws SQLException {
        String drug = "Amphotericin B";
        int endYear = 1990;
        int classifierType = 0;
//        String disease = "Neoplasm Metastasis";
        int topK = 30;
        Predict predict = new Predict(
                drug,
                endYear,
                topK,
                classifierType);
        predict.run();
    }
}
