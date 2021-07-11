package weng.main;

import com.mongodb.DBCollection;
import weng.prepare.Instance;
import weng.prepare.UmlsConcept;
import weng.util.DbConnector;
import weng.util.MongoDbConnector;
import weng.util.dbAttributes.MongoDb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by lbj23k on 2017/6/22.
 */
public class InstancePrepare {
    public static void main(String[] args) throws IOException, SQLException {
        Logger logger = Logger.getAnonymousLogger();
        long startTime = System.currentTimeMillis();
        logger.info("[InstancePrepare] Start");

        DbConnector dbConnector = new DbConnector();
        Connection connSem = dbConnector.getSemMedConnection();
        Connection connLit = dbConnector.getLiteratureNodeConnection();
        List<HashMap<String, String>> umlsMaps = UmlsConcept.getUmlsNameCuiAndCuiNameMap(connSem);
        HashMap<String, String> umlsNameCuiMap = umlsMaps.get(0);
        HashMap<String, String> umlsCuiNameMap = umlsMaps.get(1);

        MongoDbConnector mongoDbConnector = new MongoDbConnector(MongoDb.DB, MongoDb.LOCALHOST, MongoDb.PORT);
        DBCollection collNeighborCoOccurPre = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COOCCUR_PRE);
        DBCollection collNeighborCoOccurPre_ref = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COOCCUR_PRE_REF);
        DBCollection collNeighborCoOccurPost = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COOCCUR_POST);
        DBCollection collNeighborCoOccurPost_ref = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COOCCUR_POST_REF);
        DBCollection collPredicationPre_s_ref = mongoDbConnector.getDb().getCollection(MongoDb.PREDICATION_PRE_S_REF);
        DBCollection collPredicationPost_s = mongoDbConnector.getDb().getCollection(MongoDb.PREDICATION_POST_S);
        DBCollection collPredicationPost_s_ref = mongoDbConnector.getDb().getCollection(MongoDb.PREDICATION_POST_S_REF);
        DBCollection collNeighborCountPre_s = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COUNT_PRE_S);
        DBCollection collPredicationCountTemp = mongoDbConnector.getDb().getCollection(MongoDb.PREDICATION_COUNT_TEMP);

        Instance instance = new Instance();
//        instance.createGoldenFile(connSem, connLit, umlsNameCuiMap, umlsCuiNameMap,
//                collNeighborCoOccurPre, collNeighborCoOccurPre_ref,
//                collNeighborCoOccurPost, collNeighborCoOccurPost_ref,
//                collNeighborCountPre_s, collPredicationPre_s_ref,
//                collPredicationPost_s, collPredicationPost_s_ref);

//        instance.createTreatRankSet();

//        int drugLimit = 500; // focal drug size
//        int diseaseLimit = 1000;
//        instance.create500EvalRank(drugLimit, diseaseLimit);

        boolean runOldModel = false;
        instance.createInstFile(runOldModel, connSem, umlsCuiNameMap, umlsNameCuiMap, connLit, collNeighborCountPre_s, collPredicationPre_s_ref);

        // DB for calculation, only use dat_file/eval/goldenRank_noSql_1.dat to dat_file/eval/goldenRank_noSql_100.dat
//        instance.predicationCount(umlsCuiNameMap, umlsNameCuiMap, connLit, collPredicationCountTemp);
//        mongoDbConnector.truncate(collPredicationCountTemp);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInSecond = totalTime / (1000);
        logger.info("[InstancePrepare] Total Time consumed (in seconds): " + totalTimeInSecond);
        logger.info("[InstancePrepare] Finish");

        connSem.close();
        connLit.close();
    }
}
