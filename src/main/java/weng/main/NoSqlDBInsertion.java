package weng.main;

import com.mongodb.*;
import weng.noSql.UmlsNeighborCooccur;
import weng.noSql.UmlsNeighborCount;
import weng.noSql.UmlsPredication;
import weng.util.MongoDbConnector;
import weng.util.dbAttributes.MongoDb;

public class NoSqlDBInsertion {
    public static void main(String[] args) {
        MongoDbConnector mongoDbConnector = new MongoDbConnector(MongoDb.DB, MongoDb.LOCALHOST, MongoDb.PORT);
//        System.out.println(mongoDbConnector.getDb().getCollectionNames());

        UmlsPredication umlsPredication = new UmlsPredication();
        DBCollection collPredicationPre_s = mongoDbConnector.getDb().getCollection(MongoDb.PREDICATION_PRE_S);
        DBCollection collPredicationPre_s_ref = mongoDbConnector.getDb().getCollection(MongoDb.PREDICATION_PRE_S_REF);
        umlsPredication.insertPre(1, 79, 2004, collPredicationPre_s, collPredicationPre_s_ref);
        DBCollection collPredicationPost_s = mongoDbConnector.getDb().getCollection(MongoDb.PREDICATION_POST_S);
        DBCollection collPredicationPost_s_ref = mongoDbConnector.getDb().getCollection(MongoDb.PREDICATION_POST_S_REF);
        umlsPredication.insertPost(79, 147, 2005, collPredicationPost_s, collPredicationPost_s_ref);

        UmlsNeighborCount umlsNeighborCount = new UmlsNeighborCount();
        DBCollection collNeighborCountPre = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COUNT_PRE_S);
        umlsNeighborCount.insertPre(1, 79, 2004, collNeighborCountPre);
        DBCollection collNeighborCountPost = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COUNT_POST_S);
        umlsNeighborCount.insertPost(79, 147, 2005, collNeighborCountPost);

        UmlsNeighborCooccur umlsNeighborCooccur = new UmlsNeighborCooccur();
        DBCollection collNeighborCooccurPre = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COOCCUR_PRE);
        DBCollection collNeighborCooccurPre_ref = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COOCCUR_PRE_REF);
        umlsNeighborCooccur.insertPre(1, 79, 2004, collNeighborCooccurPre, collNeighborCooccurPre_ref);
        DBCollection collNeighborCooccurPost = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COOCCUR_POST);
        DBCollection collNeighborCooccurPost_ref = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COOCCUR_POST_REF);
        umlsNeighborCooccur.insertPost(79, 147, 2005, collNeighborCooccurPost, collNeighborCooccurPost_ref);

//        mongoDbConnector.truncate(collection);
//        collection.drop();

        // TODO: Create index
//        long startTime = System.currentTimeMillis();
//        collPredicationPost_s.createIndex(BasicDBObjectBuilder
//                .start(Predication.predicationInfo + "." + Predication.documentId, 1)
//                .add(Predication.predicationInfo + "." + Predication.oCui, 1)
//                .add(Predication.predicationInfo + "." + Predication.predicate, 1)
//                .get(), "id_oCui_predicate");
//        long endTime = System.currentTimeMillis();
//        long totalTime = endTime - startTime;
//        double totalTimeInSecond = totalTime / (1000);
//        System.out.println("[Index Creation] Time consumed (in seconds): " + totalTimeInSecond);

        // TODO: Try Query
//        long startTime = System.currentTimeMillis();
//        BasicDBObject searchQuery = new BasicDBObject();
//        searchQuery.put(NeighborCooccur.cui, "C0017337");
//        DBCursor cursor = collPredicationPre_s_ref.find(searchQuery);
//        while (cursor.hasNext()) {
//            DBCursor cursorInfo = collPredicationPre_s.find(
//                    new BasicDBObject(
//                            NeighborCooccur.documentId,
//                            new BasicDBObject("$in", cursor.next().get(NeighborCooccur.ids))));
//            while (cursorInfo.hasNext()) {
//                BasicDBList list = (BasicDBList) cursorInfo.next().get(NeighborCooccur.neighborInfo);
//                for (Object l : list) {
//                    JSONObject json = new JSONObject((Map) l);
//                    System.out.println(json.get(NeighborCooccur.neighbor));
//                }
//            }
//        }
//        long endTime = System.currentTimeMillis();
//        long totalTime = endTime - startTime;
//        double totalTimeInSecond = totalTime / (1000);
//        System.out.println("[NoSqlData Query] Time consumed (in seconds): " + totalTimeInSecond);
    }
}
