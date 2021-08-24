package weng.util;

import com.mongodb.*;

import java.util.Arrays;

public class MongoDbConnector {
    private DB db;
    char[] password = new char[] { '6', '1', '1', '8', '1' };
    public MongoDbConnector(String dbName, String localhost, int port) {
        MongoCredential credential = MongoCredential.createScramSha1Credential("james", "biomedical", password);
        MongoClient mongoClient = new MongoClient(new ServerAddress("140.112.106.212", port),
                Arrays.asList(credential));

//        MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://" + localhost + ":" + port));
//        MongoClient mongoClient = new MongoClient(localhost, port);
        this.db = mongoClient.getDB(dbName); // DB
    }

    public void truncate(DBCollection collection) {
        BasicDBObject document = new BasicDBObject();
        collection.remove(document);
    }

    public void setDb(DB db) {
        this.db = db;
    }

    public DB getDb() {
        return db;
    }
}
