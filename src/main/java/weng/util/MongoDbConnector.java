package weng.util;

import com.mongodb.*;

public class MongoDbConnector {
    private DB db;

    public MongoDbConnector(String dbName, String localhost, int port) {
        MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://" + localhost + ":" + port));
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
