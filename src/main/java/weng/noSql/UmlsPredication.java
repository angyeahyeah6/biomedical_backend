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
import weng.util.noSqlColumn.Predication;
import weng.util.file.sql.Csv;
import weng.util.file.sql.Json;
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

public class UmlsPredication {
    public UmlsPredication() {
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
            references.addAll(predicationInsertPost(id, collPredication, jsonPath, splitYear));
            id = references.get(references.size() - 1).getId() + 1;
        }

        Logger logger = Logger.getAnonymousLogger();
        logger.info("[UmlsPredication reference insertPost] Start");
        List<DBObject> documents = referenceDocuments(references);
        collPredication_ref.insert(documents, WriteConcern.UNACKNOWLEDGED);
        logger.info("[UmlsPredication reference insertPost] Finish");

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInSecond = totalTime / (1000);
        System.out.println("[UmlsPredication insertPost] Time consumed (in seconds): " + totalTimeInSecond);
    }

    public void insertPre(int startFile, int endFile, int splitYear,
                          DBCollection collPredicationPre,
                          DBCollection collPredicationPre_ref) {
        long startTime = System.currentTimeMillis();

        int id = 1;
        List<Reference> references = new ArrayList<>();
        for (int fileNum = startFile; fileNum <= endFile; fileNum++) {
            System.out.println("fileNum " + fileNum);
            String jsonPath = Json.directory + Csv.fileName + fileNum + ".json";
            references.addAll(predicationInsertPre(id, collPredicationPre, jsonPath, splitYear));
            id = references.get(references.size() - 1).getId() + 1;
        }

        Logger logger = Logger.getAnonymousLogger();
        logger.info("[UmlsPredication reference insertPre] Start");
        List<DBObject> documents = referenceDocuments(references);
        collPredicationPre_ref.insert(documents, WriteConcern.UNACKNOWLEDGED);
        logger.info("[UmlsPredication reference insertPre] Finish");

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInSecond = totalTime / (1000);
        System.out.println("[UmlsPredication insertPre] Time consumed (in seconds): " + totalTimeInSecond);
    }

    public List<Reference> predicationInsertPost(int id,
                                                 DBCollection infoCollection,
                                                 String jsonPath,
                                                 int splitYear) {
        Logger logger = Logger.getAnonymousLogger();
        logger.info("[predicationInsertPost] Start");
        List<PredicationInfo> infos = predicationInfo(jsonPath);
        Map<String, List<PredicationInfo>> predicationsMap = infos.stream().collect(Collectors.groupingBy(PredicationInfo::getsCui));
        infoCollection.insert(predicationDocumentsPost(id, predicationsMap, splitYear), WriteConcern.UNACKNOWLEDGED);
        logger.info("[predicationInsertPost] Finish");
        return predicationReferences(id, predicationsMap);
    }

    public List<Reference> predicationInsertPre(int id,
                                                DBCollection infoCollection,
                                                String jsonPath,
                                                int splitYear) {
        Logger logger = Logger.getAnonymousLogger();
        logger.info("[predicationInsertPre] Start");
        List<PredicationInfo> infos = predicationInfo(jsonPath);
        Map<String, List<PredicationInfo>> predicationsMap = infos.stream().collect(Collectors.groupingBy(PredicationInfo::getsCui));
        infoCollection.insert(predicationDocumentsPre(id, predicationsMap, splitYear), WriteConcern.UNACKNOWLEDGED);
        logger.info("[predicationInsertPre] Finish");
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

    public List<DBObject> predicationDocumentsPost(int id,
                                                   Map<String, List<PredicationInfo>> predicationsMap,
                                                   int splitYear) {
        List<DBObject> documents = new ArrayList<>();
        for (String cui : predicationsMap.keySet()) {
            DBObject document = new BasicDBObject();
            document.put(NeighborCooccur.documentId, id);
            List<DBObject> item = new ArrayList<>();
            for (PredicationInfo info : predicationsMap.get(cui)) {
                if (info.getYear() >= splitYear) {
                    DBObject infos = new BasicDBObject();
                    infos.put(Predication.sCui, info.getsCui());
                    infos.put(Predication.sNovel, info.getsNovel());
                    infos.put(Predication.predicate, info.getPredicate());
                    infos.put(Predication.oCui, info.getoCui());
                    infos.put(Predication.oNovel, info.getoNovel());
                    infos.put(Predication.pmid, info.getPmid());
                    infos.put(Predication.year, info.getYear());
                    infos.put(Predication.pid, info.getPid());
                    infos.put(Predication.isExist, info.getIsExist());
                    item.add(infos);
                }
            }
            if (!item.isEmpty()) {
                document.put(Predication.predicationInfo, item);
                documents.add(document);
            }
            id++;
        }
        return documents;
    }

    public List<DBObject> predicationDocumentsPre(int id,
                                                  Map<String, List<PredicationInfo>> predicationsMap,
                                                  int splitYear) {
        List<DBObject> documents = new ArrayList<>();
        for (String cui : predicationsMap.keySet()) {
            DBObject document = new BasicDBObject();
            document.put(NeighborCooccur.documentId, id);
            List<DBObject> item = new ArrayList<>();
            for (PredicationInfo info : predicationsMap.get(cui)) {
                if (info.getYear() <= splitYear) {
                    DBObject infos = new BasicDBObject();
                    infos.put(Predication.sCui, info.getsCui());
                    infos.put(Predication.sNovel, info.getsNovel());
                    infos.put(Predication.predicate, info.getPredicate());
                    infos.put(Predication.oCui, info.getoCui());
                    infos.put(Predication.oNovel, info.getoNovel());
                    infos.put(Predication.pmid, info.getPmid());
                    infos.put(Predication.year, info.getYear());
                    infos.put(Predication.pid, info.getPid());
                    infos.put(Predication.isExist, info.getIsExist());
                    item.add(infos);
                }
            }
            if (!item.isEmpty()) {
                document.put(Predication.predicationInfo, item);
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

    public List<DBObject> referenceDocuments(List<Reference> references) {
        Map<String, List<Reference>> referencesMap = references.stream().collect(Collectors.groupingBy(Reference::getCui));
        List<DBObject> documents = new ArrayList<>();
        for (String cui : referencesMap.keySet()) {
            DBObject document = new BasicDBObject();
            document.put(NeighborCooccur.cui, cui);
            List<Integer> ids = new ArrayList<>();
            referencesMap.get(cui).forEach(e -> ids.add(e.getId()));
            document.put(NeighborCooccur.ids, ids);
            documents.add(document);
        }
        return documents;
    }
}
