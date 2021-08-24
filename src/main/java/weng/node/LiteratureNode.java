package weng.node;


import com.google.common.base.Objects;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.mongodb.*;
import org.json.simple.JSONObject;
import weng.prepare.UmlsConcept;
import weng.util.noSqlColumn.NeighborCooccur;
import weng.util.noSqlColumn.Predication;
import weng.util.file.Predicate;
import weng.util.DbConnector;
import weng.util.JDBCHelper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class LiteratureNode {
    private Logger logger = Logger.getAnonymousLogger();
    private String conceptName;
    private int startYear;
    private int endYear;
    private String cui;
    private HashMap<String, Multiset<String>> predicateNeighbors = null;
    private HashMap<String, Integer> neighbors = null;
    private HashSet<String> documents = null;
    private int df = (-1);
    private int freq;

    public LiteratureNode(String conceptName, int startYear, int endYear, Connection connSem, Connection connLit) throws SQLException {
        this.conceptName = conceptName;
        this.startYear = startYear;
        this.endYear = endYear;
        this.cui = setCui(connSem, conceptName);
        this.freq = setArticleFrequency(connLit);
    }

    public LiteratureNode(String conceptName, String cui, int startYear, int endYear, Connection connLit) throws SQLException {
        this.conceptName = conceptName;
        this.startYear = startYear;
        this.endYear = endYear;
        this.cui = cui;
        this.freq = setArticleFrequency(connLit);
    }

    public LiteratureNode(String conceptName, String cui, int startYear, int endYear) {
        this.conceptName = conceptName;
        this.startYear = startYear;
        this.endYear = endYear;
        this.cui = cui;
    }

    public int getFreq() {
        return freq;
    }

    public String setCui(Connection connSem, String conceptName) throws SQLException {
        String cui = "";
        String sqlSelConcept = "SELECT `CUI` FROM `concept` WHERE `PREFERRED_NAME`=? AND `TYPE`=\"META\"";
        PreparedStatement psSelConcept = connSem.prepareStatement(sqlSelConcept);
        psSelConcept.clearParameters();
        psSelConcept.setString(1, conceptName);
        ResultSet rsSelConcept = psSelConcept.executeQuery();
        if (rsSelConcept.next()) {
            cui = rsSelConcept.getString("CUI");
        }
        rsSelConcept.close();
        psSelConcept.close();
        return cui;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        LiteratureNode node0 = (LiteratureNode) obj;
        return ((this.conceptName.equals(node0.conceptName)) && (this.startYear == node0.startYear)
                && (this.endYear == node0.endYear));
    }

    @Override
    public int hashCode() {
        return (Objects.hashCode(conceptName, startYear, endYear));
    }

    public String getName() {
        return conceptName;
    }

    public int df(Connection connLit) { // Document's frequency
        if (df == -1) {
            String sql = "SELECT df FROM `umls_concept_by_year` WHERE cui = ? AND (`year` BETWEEN ? AND ?)";
            Object[] params = {cui, startYear, endYear};
            JDBCHelper jdbcHelper = new JDBCHelper();
            List<Map<String, Object>> result = jdbcHelper.query(connLit, sql, params);
            int freq = 0;
            for (Map row : result) {
                int df = (int) row.get("df");
                freq += df;
            }
            df = freq;
        }
        return df;
    }

    public HashSet<String> getDocuments(Connection connLit) {
        if (documents == null) setDocuments(connLit);
        return documents;
    }

    public int getNeighborCount(HashMap<String, String> umlsCuiNameMap, DBCollection collInfo, DBCollection collRef) {
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(Predication.cui, getCui());
        DBCursor cursor = collRef.find(searchQuery);
        Set<String> neighbors = new HashSet<>();
        while (cursor.hasNext()) {
            DBCursor cursorInfo = collInfo.find(
                    new BasicDBObject(
                            Predication.documentId,
                            new BasicDBObject("$in", cursor.next().get(Predication.ids))));
            while (cursorInfo.hasNext()) {
                DBObject next = cursorInfo.next();
                BasicDBList oCuis = (BasicDBList) next.get(Predication.oCuis);
                BasicDBList predicates = (BasicDBList) next.get(Predication.predicates);
                for (int index = 0; index < predicates.size(); index++) {
                    String neighborCui = String.valueOf(oCuis.get(index));
                    String neighborName = umlsCuiNameMap.get(neighborCui);
                    if (neighborName != null) {
                        String predicate = String.valueOf(predicates.get(index));
                        if (Predicate.set.contains(predicate)) {
                            neighbors.add(neighborName);
                        }
                    }
                }
            }
        }
        return neighbors.size();
    }

    public int getNeighborCount(HashMap<String, String> umlsCuiNameMap, Connection connLit) {
        String sql = "SELECT s_cui, o_cui, predicate FROM umls_predication_aggregate_filtered WHERE s_cui = ? AND (year BETWEEN ? AND ?)";
        Object[] params = {cui, startYear, endYear};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result = jdbcHelper.query(connLit, sql, params);
        Set<String> neighbors = new HashSet<>();
        for (Map row : result) {
            String neighborCui = (String) row.get("o_cui");
            String neighborName = umlsCuiNameMap.get(neighborCui);
            if (neighborName != null) {
                String predicate = (String) row.get("predicate");
                if (Predicate.set.contains(predicate)) {
                    neighbors.add(neighborName);
                }
            }
        }
        return neighbors.size();

    }

    public int getNeighborCount(Connection connSem, Connection connLit) throws SQLException {
        String sql = "SELECT s_cui, o_cui, predicate FROM umls_predication_aggregate_filtered WHERE s_cui = ? AND (year BETWEEN ? AND ?)";
        Object[] params = {cui, startYear, endYear};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result = jdbcHelper.query(connLit, sql, params);
        Set<String> neighbors = new HashSet<>();
        for (Map row : result) {
            String neighborCui = (String) row.get("o_cui");
            String neighborName = conceptName(connSem, neighborCui);
            if (neighborName != null) {
                String predicate = (String) row.get("predicate");
                if (Predicate.set.contains(predicate)) {
                    neighbors.add(neighborName);
                }
            }
        }
        return neighbors.size();

    }

    public String conceptName(Connection connSem, String cui) throws SQLException {
        String name = "";
        String sqlSelConcept = "SELECT `PREFERRED_NAME` FROM `concept` WHERE `CUI`=? AND `TYPE`=\"META\"";
        PreparedStatement psSelConcept = connSem.prepareStatement(sqlSelConcept);
        psSelConcept.clearParameters();
        psSelConcept.setString(1, cui);
        ResultSet rsSelConcept = psSelConcept.executeQuery();
        if (rsSelConcept.next()) {
            name = rsSelConcept.getString("PREFERRED_NAME");
        }
        rsSelConcept.close();
        psSelConcept.close();
        return name;
    }

    public HashMap<String, Multiset<String>> getPredicateNeighbors(HashMap<String, String> umlsCuiNameMap,
                                                                   DBCollection collPredicationPost, DBCollection collPredicationPost_ref) {
        // TODO: test
        long startTime = System.currentTimeMillis();

        if (predicateNeighbors == null) {
            predicateNeighbors = new LinkedHashMap<>();

            BasicDBObject searchQuery = new BasicDBObject();
            searchQuery.put(Predication.cui, getCui());
            DBCursor cursor = collPredicationPost_ref.find(searchQuery);
            while (cursor.hasNext()) {
                DBCursor cursorInfo = collPredicationPost.find(
                        new BasicDBObject(
                                Predication.documentId,
                                new BasicDBObject("$in", cursor.next().get(Predication.ids))));
                while (cursorInfo.hasNext()) {
                    BasicDBList list = (BasicDBList) cursorInfo.next().get(Predication.predicationInfo);
                    for (Object l : list) {
                        JSONObject json = new JSONObject((Map) l);
                        String neighborCui = String.valueOf(json.get(Predication.oCui));
                        String neighborName = umlsCuiNameMap.get(neighborCui);
                        if (neighborName != null) {
                            String predicate = String.valueOf(json.get(Predication.predicate));
                            if (Predicate.set.contains(predicate)) {
                                Multiset<String> neighborInfo = predicateNeighbors.getOrDefault(neighborName, HashMultiset.create());
                                neighborInfo.add(predicate);
                                predicateNeighbors.put(neighborName, neighborInfo);
                            }
                        }
                    }
                }
            }
        }

        // TODO: test
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        //        double totalTimeInSecond = totalTime / (1000);
        double totalTimeInSecond = totalTime;
        logger.info("[getPredicateNeighbors] Time consumed (ms): " + totalTimeInSecond);
        return predicateNeighbors;
    }

    public HashMap<String, Multiset<String>> getPredicateNeighbors(HashMap<String, String> umlsCuiNameMap,
                                                                   Connection connLit) {
        // TODO: test
        long startTime = System.currentTimeMillis();

        if (predicateNeighbors == null) {
            predicateNeighbors = new LinkedHashMap<>();
            String sql = "SELECT s_cui, o_cui, predicate FROM umls_predication_aggregate_filtered WHERE s_cui = ? AND (year BETWEEN ? AND ?)";
            Object[] params = {cui, startYear, endYear};
            JDBCHelper jdbcHelper = new JDBCHelper();
            List<Map<String, Object>> result = jdbcHelper.query(connLit, sql, params);
            for (Map row : result) {
                String neighborCui = (String) row.get("o_cui");
                String neighborName = umlsCuiNameMap.get(neighborCui);
                if (neighborName != null) {
                    String predicate = (String) row.get("predicate");
                    if (Predicate.set.contains(predicate)) {
                        Multiset<String> neighborInfo = predicateNeighbors.getOrDefault(neighborName, HashMultiset.create());
                        neighborInfo.add(predicate);
                        predicateNeighbors.put(neighborName, neighborInfo);
                    }
                }
            }
        }

        // TODO: test
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        //        double totalTimeInSecond = totalTime / (1000);
        double totalTimeInSecond = totalTime;
        System.out.println("[getPredicateNeighbors] Time consumed (in seconds): " + totalTimeInSecond);
        return predicateNeighbors;
    }

    /**
     * Get node's neighbors
     *
     * @return (neighborName, frequency)
     * cui|year|neighbor|frequency
     * C0007272|1809|C0002940|5
     */
    public HashMap<String, Integer> getNeighbors(Connection connSem) throws SQLException {
        if (neighbors == null) {
            neighbors = new HashMap<>();
            HashMap<String, String> umlsCuiNameMap = UmlsConcept.getUmlsCuiNameMap(connSem);
            String sql = "SELECT neighbor, freq FROM umls_neighbor_by_predication WHERE cui = ? AND (year BETWEEN ? AND ?)";
            Object[] params = {cui, startYear, endYear};
            String litDBName = DbConnector.LITERATURE_YEAR;
            JDBCHelper jdbcHelper = new JDBCHelper();
            List<Map<String, Object>> result = jdbcHelper.query(litDBName, sql, params);
            for (Map row : result) {
                String neighborCui = (String) row.get("neighbor");
                String neighborName = umlsCuiNameMap.get(neighborCui);
                if (neighborName != null) {
                    int frequency = neighbors.getOrDefault(neighborName, 0);
                    neighbors.put(neighborName, frequency + (int) row.get("freq"));
                }
            }
        }
        return neighbors;
    }

//    public HashMap<String, Integer> getCooccurNeighbors(HashMap<String, String> umlsCuiNameMap,
//                                                        Connection connLit) {
//        HashMap<String, Integer> cooccurNeighbors = new LinkedHashMap<>();
//        String sql = "SELECT neighbor, freq FROM umls_neighbor_cooccur WHERE cui = ? AND (year BETWEEN ? AND ?)";
//        Object[] params = {getCui(), getStartYear(), getEndYear()};
//        JDBCHelper jdbcHelper = new JDBCHelper();
//        List<Map<String, Object>> result = jdbcHelper.query(connLit, sql, params);
//        for (Map row : result) {
//            String neighborCui = (String) row.get("neighbor");
//            String neighborName = umlsCuiNameMap.get(neighborCui);
//            if (neighborName != null) {
//                int frequency = cooccurNeighbors.getOrDefault(neighborName, 0);
//                cooccurNeighbors.put(neighborName, frequency + (int) row.get("freq"));
//            }
//        }
//        return cooccurNeighbors;
//    }

    public HashMap<String, Integer> getCooccurNeighbors(HashMap<String, String> umlsCuiNameMap,
                                                        DBCollection collection_info,
                                                        DBCollection collection_ref) {
        HashMap<String, Integer> cooccurNeighbors = new LinkedHashMap<>();
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(NeighborCooccur.cui, getCui());
        DBCursor cursor = collection_ref.find(searchQuery);
        while (cursor.hasNext()) {
            DBCursor cursorInfo = collection_info.find(
                    new BasicDBObject(
                            NeighborCooccur.documentId,
                            new BasicDBObject("$in", cursor.next().get(NeighborCooccur.ids))));
            while (cursorInfo.hasNext()) {
                BasicDBList list = (BasicDBList) cursorInfo.next().get(NeighborCooccur.neighborInfo);
                for (Object l : list) {
                    JSONObject json = new JSONObject((Map) l);
                    if(Integer.valueOf(json.get("year").toString()) >= getEndYear()){
                        continue;
                    }
                    String neighborCui = String.valueOf(json.get(NeighborCooccur.neighbor));
                    String neighborName = umlsCuiNameMap.get(neighborCui);
                    if (neighborName != null) {
                        int frequency = cooccurNeighbors.getOrDefault(neighborName, 0);
                        cooccurNeighbors.put(neighborName, frequency + Integer.parseInt(String.valueOf(json.get(NeighborCooccur.freq))));
                    }
                }
            }
        }
        return cooccurNeighbors;
    }

    @Override
    public String toString() {
        return "Node: " + this.conceptName;
    }

    /*
     * NAME|YEAR|PMID|DF
     * 1,2-Dimethylhydrazine|1976|1016720,136114|2
     * */
    private void setDocuments(Connection connLit) {
        documents = new HashSet<>();
        String sql = "SELECT pmid, freq FROM `umls_concept_by_year` WHERE cui = ? AND (year BETWEEN ? AND ?)";
        Object[] params = {cui, startYear, endYear};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result = jdbcHelper.query(connLit, sql, params);
        df = 0;
        freq = 0;
        for (Map row : result) {
            String[] pmids = ((String) row.get("pmid")).split(",");
            df += pmids.length;
            documents.addAll(Arrays.asList(pmids));
            freq += (int) row.get("freq");
        }
    }

    public int setArticleFrequency(Connection connLit) {
        String sql = "SELECT sum(freq) as sf FROM `umls_concept_by_year` WHERE cui = ? AND (year BETWEEN ? AND ?)";
        String cui = getCui();
        int startYear = getStartYear();
        int endYear = getEndYear();
        Object[] params = {cui, startYear, endYear};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result = jdbcHelper.query(connLit, sql, params);
        int articleFreq = 0;
        if (result.get(0).get("sf") != null) {
            articleFreq = ((BigDecimal) result.get(0).get("sf")).intValue();
        }
        return articleFreq;
    }

    public String getCui() {
        return this.cui;
    }

    public int getStartYear() {
        return this.startYear;
    }

    public int getEndYear() {
        return this.endYear;
    }
}
