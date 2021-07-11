package weng.main;

import com.mongodb.DBCollection;
import weng.labelSys.Predict;
import weng.prepare.UmlsConcept;
import weng.util.DbConnector;
import weng.util.MongoDbConnector;
import weng.util.dbAttributes.MongoDb;
import weng.util.predict.Disease;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by lbj23k on 2017/8/13.
 */
public class DiseaseRanking {
    public static void main(String[] args) {
        Logger logger = Logger.getAnonymousLogger();
        logger.info("[DiseaseRanking] Start");
        long startTime = System.currentTimeMillis();

        // 威而鋼
//        String drug = "sildenafil";
//        int endYear = 1999;

        // 柔沛
//        String drug = "Finasteride";
//        int endYear = 1993;

        // 百憂解
//        String drug = "Fluoxetine";
//        int endYear = 1996;

//        String drug = "Thalidomide";
//        int endYear = 1998;

        String drug = "Metformin";
        int endYear = 2007;

        logger.info("[DiseaseRanking] drug: " + drug);
        logger.info("[DiseaseRanking] endYear: " + endYear);

        int classifierType = 0; // lr
//        int classifierType = 1; // nb
//        int classifierType = 2; // rf
//        int classifierType = 3; // svm
        int topK = 30;
        Predict predict = new Predict(drug, endYear, topK, classifierType);
        predict.run();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInSecond = totalTime / (1000);
        logger.info("Total Time consumed (in seconds): " + totalTimeInSecond);
        logger.info("[DiseaseRanking] Finish");
    }
}
