package weng.noSql;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import weng.util.noSqlColumn.NeighborCooccur;
import weng.util.file.sql.Csv;
import weng.util.file.sql.Json;
import weng.util.noSqlData.NeighborInfo;
import weng.util.noSqlData.Reference;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UmlsNeighborCooccur {
    public UmlsNeighborCooccur() {
    }

    public void insertPre(int startFile, int endFile, int splitYear,
                          DBCollection collPredication,
                          DBCollection collPredication_ref) {
        long startTime = System.currentTimeMillis();

        int id = 1;
        List<Reference> references = new ArrayList<>();
        for (int fileNum = startFile; fileNum <= endFile; fileNum++) {
            System.out.println("fileNum " + fileNum);
            String jsonPath = Json.directory + Csv.fileName + fileNum + ".json";
            references.addAll(neighborCooccurInsertPre(id, collPredication, jsonPath, splitYear));
            id = references.get(references.size() - 1).getId() + 1;
        }

        Logger logger = Logger.getAnonymousLogger();
        logger.info("[reference neighborCooccurInsert] Start");
        referenceDocuments(collPredication_ref, references);
        logger.info("[reference neighborCooccurInsert] Finish");

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInSecond = totalTime / (1000);
        System.out.println("[UmlsNeighborCooccur insertPre] Time consumed (in seconds): " + totalTimeInSecond);
    }

    public void insertPost(int startFile, int endFile, int splitYear,
                           DBCollection collPredication,
                           DBCollection collPredication_ref) {
        long startTime = System.currentTimeMillis();

        int id = 1;
        List<Reference> references = new ArrayList<>();
        for (int fileNum = startFile; fileNum <= endFile; fileNum++) {
            System.out.println("fileNum " + fileNum);
            String jsonPath = Json.directory + Csv.fileName + fileNum + ".json";
            references.addAll(neighborCooccurInsertPost(id, collPredication, jsonPath, splitYear));
            id = references.get(references.size() - 1).getId() + 1;
        }

        Logger logger = Logger.getAnonymousLogger();
        logger.info("[reference neighborCooccurInsert] Start");
        referenceDocuments(collPredication_ref, references);
        logger.info("[reference neighborCooccurInsert] Finish");

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInSecond = totalTime / (1000);
        System.out.println("[UmlsNeighborCooccur insertPost] Time consumed (in seconds): " + totalTimeInSecond);
    }

    public void referenceDocuments(DBCollection collNeighborCoOccur_ref, List<Reference> references) {
        Map<String, List<Reference>> referencesMap = references.stream().collect(Collectors.groupingBy(Reference::getCui));
        List<DBObject> documents = new ArrayList<>();
        int divide = 1;
        for (String cui : referencesMap.keySet()) {
            DBObject document = new BasicDBObject();
            document.put(NeighborCooccur.cui, cui);
            List<Integer> ids = new ArrayList<>();
            referencesMap.get(cui).forEach(e -> ids.add(e.getId()));
            document.put(NeighborCooccur.ids, ids);
            documents.add(document);
            if (divide % 20000 == 0) {
                collNeighborCoOccur_ref.insert(documents, WriteConcern.UNACKNOWLEDGED);
                documents = new ArrayList<>();
            }
            divide++;
        }
        collNeighborCoOccur_ref.insert(documents, WriteConcern.UNACKNOWLEDGED);
    }

    public List<Reference> neighborCooccurInsertPre(int id,
                                                    DBCollection infoCollection,
                                                    String jsonPath,
                                                    int splitYear) {
        Logger logger = Logger.getAnonymousLogger();
        logger.info("[MongoDB neighborCooccurInsert] Start");
        List<NeighborInfo> infos = neighborInfo(jsonPath);
        Map<String, List<NeighborInfo>> neighborsMap = infos.stream().collect(Collectors.groupingBy(NeighborInfo::getCui));
        infoCollection.insert(neighborDocumentsPre(id, neighborsMap, splitYear), WriteConcern.UNACKNOWLEDGED);
        logger.info("[MongoDB neighborCooccurInsert] Finish");
        return neighborReferences(id, neighborsMap);
    }

    public List<Reference> neighborCooccurInsertPost(int id,
                                                     DBCollection infoCollection,
                                                     String jsonPath,
                                                     int splitYear) {
        Logger logger = Logger.getAnonymousLogger();
        logger.info("[MongoDB neighborCooccurInsert] Start");
        List<NeighborInfo> infos = neighborInfo(jsonPath);
        Map<String, List<NeighborInfo>> neighborsMap = infos.stream().collect(Collectors.groupingBy(NeighborInfo::getCui));
        infoCollection.insert(neighborDocumentsPost(id, neighborsMap, splitYear), WriteConcern.UNACKNOWLEDGED);
        logger.info("[MongoDB neighborCooccurInsert] Finish");
        return neighborReferences(id, neighborsMap);
    }

    public List<NeighborInfo> neighborInfo(String jsonPath) {
        List<NeighborInfo> infos = new ArrayList<>();
        JSONParser parser = new JSONParser();
        try {
            JSONArray a = (JSONArray) parser.parse(new FileReader(new File(jsonPath)));
            for (Object o : a) {
                JSONObject neighbors = (JSONObject) o;
                infos.add(new NeighborInfo(
                        neighbors.get(NeighborCooccur.cui).toString(),
                        Integer.parseInt(neighbors.get(NeighborCooccur.year).toString()),
                        neighbors.get(NeighborCooccur.neighbor).toString(),
                        Integer.parseInt(neighbors.get(NeighborCooccur.freq).toString())));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return infos;
    }

    public List<Reference> neighborReferences(int id,
                                              Map<String, List<NeighborInfo>> neighborsMap) {
        List<Reference> references = new ArrayList<>();
        for (String cui : neighborsMap.keySet()) {
            references.add(new Reference(id, cui));
            id++;
        }
        return references;
    }

    public List<DBObject> neighborDocumentsPre(int id,
                                               Map<String, List<NeighborInfo>> neighborsMap,
                                               int splitYear) {
        List<DBObject> documents = new ArrayList<>();
        for (String cui : neighborsMap.keySet()) {
            DBObject document = new BasicDBObject();
            document.put(NeighborCooccur.documentId, id);
            List<DBObject> item = new ArrayList<>();
            for (NeighborInfo info : neighborsMap.get(cui)) {
                if (info.getYear() <= splitYear) {
                    DBObject infos = new BasicDBObject();
                    infos.put(NeighborCooccur.cui, info.getCui());
                    infos.put(NeighborCooccur.year, info.getYear());
                    infos.put(NeighborCooccur.neighbor, info.getNeighbor());
                    infos.put(NeighborCooccur.freq, info.getFreq());
                    item.add(infos);
                }
            }
            if (!item.isEmpty()) {
                document.put(NeighborCooccur.neighborInfo, item);
                documents.add(document);
            }
            id++;
        }
        return documents;
    }

    public List<DBObject> neighborDocumentsPost(int id,
                                                Map<String, List<NeighborInfo>> neighborsMap,
                                                int splitYear) {
        List<DBObject> documents = new ArrayList<>();
        for (String cui : neighborsMap.keySet()) {
            DBObject document = new BasicDBObject();
            document.put(NeighborCooccur.documentId, id);
            List<DBObject> item = new ArrayList<>();
            for (NeighborInfo info : neighborsMap.get(cui)) {
                if (info.getYear() >= splitYear) {
                    DBObject infos = new BasicDBObject();
                    infos.put(NeighborCooccur.cui, info.getCui());
                    infos.put(NeighborCooccur.year, info.getYear());
                    infos.put(NeighborCooccur.neighbor, info.getNeighbor());
                    infos.put(NeighborCooccur.freq, info.getFreq());
                    item.add(infos);
                }
            }
            if (!item.isEmpty()) {
                document.put(NeighborCooccur.neighborInfo, item);
                documents.add(document);
            }
            id++;
        }
        return documents;
    }
}
