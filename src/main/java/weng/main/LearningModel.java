package weng.main;

import com.mongodb.DBCollection;
import weng.model.Learning;
import weng.model.LearningPreparation;
import weng.util.DbConnector;
import weng.util.MongoDbConnector;
import weng.util.dbAttributes.MongoDb;
import weng.util.noSqlData.PredicationCountInfo;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by lbj23k on 2017/6/1.
 */
public class LearningModel implements Serializable {
    private static final long serialVersionUID = 689123320491080199L;

    public static void main(String[] args) throws SQLException {
        DbConnector dbConnector = new DbConnector();
        Connection connSem = dbConnector.getSemMedConnection();
        Connection connLit = dbConnector.getLiteratureNodeConnection();
        Connection connLSUMLS = dbConnector.getLabelSystemUmlsConnection();

        Logger logger = Logger.getLogger("LearningModel");
        logger.info("START....");

        LearningPreparation learningPreparation = new LearningPreparation();

        MongoDbConnector mongoDbConnector = new MongoDbConnector(MongoDb.DB, MongoDb.LOCALHOST, MongoDb.PORT);
        DBCollection collPredicationCount = mongoDbConnector.getDb().getCollection(MongoDb.PREDICATION_COUNT_TEMP);
        List<PredicationCountInfo> predicationCounts = learningPreparation.predicationCounts(collPredicationCount);

        int diseaseCount = 1000;
        logger.info("diseaseCount: " + diseaseCount);
        Learning learningByDifferentDiseaseCount = new Learning(1, 100, diseaseCount);
        learningByDifferentDiseaseCount.baseLine(true, connSem, connLit, predicationCounts, learningByDifferentDiseaseCount.getDiseaseCount()); // LTC_AMW

        int classifierType = 0; // lr
//        int classifierType = 1; // nb
//        int classifierType = 2; // rf
//        int classifierType = 3; // svm
        learningByDifferentDiseaseCount.runOldFeatureModel(true, classifierType, connSem, connLit, learningByDifferentDiseaseCount.getDiseaseCount()); // Lee's model
        learningByDifferentDiseaseCount.runModel(false, classifierType, connSem, connLit, learningByDifferentDiseaseCount.getDiseaseCount()); // My model

        logger.info("END....");

        connSem.close();
        connLit.close();
        connLSUMLS.close();
    }
}
