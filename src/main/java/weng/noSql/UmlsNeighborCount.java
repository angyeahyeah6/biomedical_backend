package weng.noSql;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import weng.util.file.sql.Csv;
import weng.util.file.sql.Json;
import weng.util.noSqlColumn.NeighborCooccur;
import weng.util.noSqlColumn.Predication;
import weng.util.noSqlData.PredicationInfo;
import weng.util.noSqlData.Reference;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UmlsNeighborCount {
    public UmlsNeighborCount() {
    }

    public void insertPre(int startFile, int endFile, int splitYear,
                          DBCollection collNeighborCoOccur_info) {
        long startTime = System.currentTimeMillis();

        int id = 1;
        List<Reference> references = new ArrayList<>();
        for (int fileNum = startFile; fileNum <= endFile; fileNum++) {
            System.out.println("fileNum " + fileNum);
            String jsonPath = Json.directory + Csv.fileName + fileNum + ".json";
            references.addAll(predicationInsertPre(id, collNeighborCoOccur_info, jsonPath, splitYear));
            id = references.get(references.size() - 1).getId() + 1;
        }
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInSecond = totalTime / (1000);
        System.out.println("[UmlsNeighborCount insertPre] Time consumed (in seconds): " + totalTimeInSecond);
    }

    public void insertPost(int startFile, int endFile, int splitYear,
                           DBCollection collNeighborCoOccur_info) {
        long startTime = System.currentTimeMillis();

        int id = 1;
        List<Reference> references = new ArrayList<>();
        for (int fileNum = startFile; fileNum <= endFile; fileNum++) {
            System.out.println("fileNum " + fileNum);
            String jsonPath = Json.directory + Csv.fileName + fileNum + ".json";
            references.addAll(predicationInsertPost(id, collNeighborCoOccur_info, jsonPath, splitYear));
            id = references.get(references.size() - 1).getId() + 1;
        }
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInSecond = totalTime / (1000);
        System.out.println("[UmlsNeighborCount insertPost] Time consumed (in seconds): " + totalTimeInSecond);
    }

    public List<Reference> predicationInsertPre(int id, DBCollection infoCollection, String jsonPath, int splitYear) {
        Logger logger = Logger.getAnonymousLogger();
        logger.info("[predicationInsertPre] Start");
        List<PredicationInfo> infos = predicationInfo(jsonPath);
        Map<String, List<PredicationInfo>> predicationsMap = infos.stream().collect(Collectors.groupingBy(PredicationInfo::getsCui));
        infoCollection.insert(predicationDocumentsPre(id, predicationsMap, splitYear), WriteConcern.UNACKNOWLEDGED);
        logger.info("[predicationInsertPre] Finish");
        return predicationReferences(id, predicationsMap);
    }

    public List<Reference> predicationInsertPost(int id, DBCollection infoCollection, String jsonPath, int splitYear) {
        Logger logger = Logger.getAnonymousLogger();
        logger.info("[predicationInsertPost] Start");
        List<PredicationInfo> infos = predicationInfo(jsonPath);
        Map<String, List<PredicationInfo>> predicationsMap = infos.stream().collect(Collectors.groupingBy(PredicationInfo::getsCui));
        infoCollection.insert(predicationDocumentsPost(id, predicationsMap, splitYear), WriteConcern.UNACKNOWLEDGED);
        logger.info("[predicationInsertPost] Finish");
        return predicationReferences(id, predicationsMap);
    }

    public List<Reference> predicationReferences(int id,
                                                 Map<String, List<PredicationInfo>> predicationsMap) {
        List<Reference> references = new ArrayList<>();
        for (String cui : predicationsMap.keySet()) {
            references.add(new Reference(id, cui));
            id++;
        }
        return references;
    }

    public List<DBObject> predicationDocumentsPre(int id,
                                                  Map<String, List<PredicationInfo>> predicationsMap,
                                                  int splitYear) {
        List<DBObject> documents = new ArrayList<>();
        for (String cui : predicationsMap.keySet()) {
            DBObject document = new BasicDBObject();
            document.put(NeighborCooccur.documentId, id);
            List<String> neighbors = new ArrayList<>();
            List<String> predicates = new ArrayList<>();
            for (PredicationInfo info : predicationsMap.get(cui)) {
                if (info.getYear() <= splitYear) {
                    neighbors.add(info.getoCui());
                    predicates.add(info.getPredicate());
                }
            }
            if (!neighbors.isEmpty() || !predicates.isEmpty()) {
                document.put(Predication.oCuis, neighbors);
                document.put(Predication.predicates, predicates);
                documents.add(document);
            }
            id++;
        }
        return documents;
    }

    public List<DBObject> predicationDocumentsPost(int id,
                                                   Map<String, List<PredicationInfo>> predicationsMap,
                                                   int splitYear) {
        List<DBObject> documents = new ArrayList<>();
        for (String cui : predicationsMap.keySet()) {
            DBObject document = new BasicDBObject();
            document.put(NeighborCooccur.documentId, id);
            List<String> neighbors = new ArrayList<>();
            List<String> predicates = new ArrayList<>();
            for (PredicationInfo info : predicationsMap.get(cui)) {
                if (info.getYear() >= splitYear) {
                    neighbors.add(info.getoCui());
                    predicates.add(info.getPredicate());
                }
            }
            if (!neighbors.isEmpty() || !predicates.isEmpty()) {
                document.put(Predication.oCuis, neighbors);
                document.put(Predication.predicates, predicates);
                documents.add(document);
            }
            id++;
        }
        return documents;
    }

    public List<PredicationInfo> predicationInfo(String jsonPath) {
        List<PredicationInfo> infos = new ArrayList<>();
        JSONParser parser = new JSONParser();
        try {
            JSONArray a = (JSONArray) parser.parse(new FileReader(new File(jsonPath)));
            for (Object o : a) {
                JSONObject predication = (JSONObject) o;
                infos.add(new PredicationInfo(
                        predication.get(Predication.sCui).toString(),
                        Integer.parseInt(predication.get(Predication.sNovel).toString()),
                        predication.get(Predication.predicate).toString(),
                        predication.get(Predication.oCui).toString(),
                        Integer.parseInt(predication.get(Predication.oNovel).toString()),
                        predication.get(Predication.pmid).toString(),
                        Integer.parseInt(predication.get(Predication.year).toString()),
                        Integer.parseInt(predication.get(Predication.pid).toString()),
                        Integer.parseInt(predication.get(Predication.isExist).toString())));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return infos;
    }
}
