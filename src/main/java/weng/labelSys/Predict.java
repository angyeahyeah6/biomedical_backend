package weng.labelSys;

import com.mongodb.DBCollection;
import weka.core.pmml.jaxbbindings.True;
import weng.evaluation.IndexScore;
import weng.feature.SemanticType;
import weng.network.PredictNetwork;
import weng.node.LiteratureNode;
import weng.prepare.UmlsConcept;
import weng.util.DbConnector;
import weng.util.JDBCHelper;
import weng.util.MongoDbConnector;
import weng.util.ProgressBar;
import weng.util.dbAttributes.MongoDb;
import weng.util.dbAttributes.MySqlDb;
import weng.util.file.RepurposingEval;
import weng.util.predict.Disease;
import weng.util.predict.DiseaseScore;
import weng.util.predict.Intermediate;
import weng.util.predict.Path;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by weng on 2017/10/28.
 */

public class Predict {
    private Logger logger = Logger.getAnonymousLogger();
    private static final int startYear = 1809;
    private String drug;
    private int endYear;
    private int topK;
    private int classifierType;

    private List<Disease> topKDiseases;

    public Predict(String drug, int endYear, int topK,
                   int classifierType) {
        this.drug = drug;
        this.endYear = endYear;
        this.topK = topK;
        this.classifierType = classifierType;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setDrug(String drug) {
        this.drug = drug;
    }

    public void setEndYear(int endYear) {
        this.endYear = endYear;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public void setClassifierType(int classifierType) {
        this.classifierType = classifierType;
    }

    public String getDrug() {
        return drug;
    }

    public int getEndYear() {
        return endYear;
    }

    public int getTopK() {
        return topK;
    }

    public int getClassifierType() {
        return classifierType;
    }

    public void run() {
        DbConnector dbConnector = new DbConnector();
        Connection connLS = dbConnector.getLabelSystemUmlsConnection();
        Connection connSem = dbConnector.getSemMedConnection();
        Connection connLit = dbConnector.getLiteratureNodeConnection();

        MongoDbConnector mongoDbConnector = new MongoDbConnector(MongoDb.DB, MongoDb.LOCALHOST, MongoDb.PORT);
        DBCollection collNeighborCoOccurPre = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COOCCUR_PRE);
        DBCollection collNeighborCoOccurPre_ref = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COOCCUR_PRE_REF);
        DBCollection collPredicationPre_s_ref = mongoDbConnector.getDb().getCollection(MongoDb.PREDICATION_PRE_S_REF);
        DBCollection collNeighborCountPre_s = mongoDbConnector.getDb().getCollection(MongoDb.NEIGHBOR_COUNT_PRE_S);

        try {
            // get umls and cui pair
            List<HashMap<String, String>> umlsMaps = UmlsConcept.getUmlsNameCuiAndCuiNameMap(connSem);
            HashMap<String, String> umlsNameCuiMap = umlsMaps.get(0);
            HashMap<String, String> umlsCuiNameMap = umlsMaps.get(1);

            // check if the drug (include classifier type and time interval ) is train already
            boolean isPredictDrugDuplicate = examinePredictDrugDuplicate(connLS);
            // insert for "predict_drug"
            insertPredictCase(isPredictDrugDuplicate, connLS);

            boolean runOldModel = false;
            // find id of the predict drug
            int predictDrugId = predictDrugId(connLS);

            // insert for "predict_score" and insert "predict_important_b"
            constructNetwork(runOldModel, predictDrugId, isPredictDrugDuplicate, connLit, connLS, connSem, umlsCuiNameMap, umlsNameCuiMap,
                    collNeighborCountPre_s, collPredicationPre_s_ref, collNeighborCoOccurPre, collNeighborCoOccurPre_ref);


            // get top k disease (C)
            List<Disease> topKDiseases = topKDisease(isPredictDrugDuplicate, predictDrugId, connLS);
            this.topKDiseases = topKDiseases;
            // insert for all the other
            insertResults(topKDiseases, runOldModel, predictDrugId, umlsCuiNameMap, connLS, connSem, connLit);

            connLS.close();
            connSem.close();
            connLit.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public Map<String, Map<String, Object>> getOrderTarget(String drug, int endYear) throws SQLException {
        int startYear = 1809;

        Map<String, Map<String, Object>> result = new HashMap<>();
        // get drug id
        DbConnector dbConnector = new DbConnector();
        Connection connLS = dbConnector.getLabelSystemUmlsConnection();
        int predictDrugId =  predictDrugId(drug, startYear, endYear, connLS);

        List<DiseaseScore> DiseaseScores = getDiseaseScore(predictDrugId, connLS);
        Collections.sort(DiseaseScores);

        int cnt = 0;
        for(DiseaseScore d : DiseaseScores){
            Map<String, Object> eval = new HashMap<>();
            eval.put("id",cnt);
            eval.put("eval1", d.getScore());
            result.put(d.getName(), eval);
            cnt += 1;
        }
        return result;

    }
    // get top k disease and the important intermediate (b)
    public Map<String, Map<String, Map<String, Integer>>> getCompletePath(String drug, int endYear) throws SQLException {
        int startYear = 1809;
        // init result
        Map<String, Map<String, Map<String, Integer>>> result = new HashMap<>();
        // get drug id
        DbConnector dbConnector = new DbConnector();
        Connection connLS = dbConnector.getLabelSystemUmlsConnection();
        int predictDrugId =  predictDrugId(drug, startYear, endYear, connLS);

        // run old model
        boolean runOldModel = false;
        // get data from predict_diseases
        List<Disease> Diseases = getDisease(predictDrugId, connLS);

        // get intermediate from predict_intermediate
        for(Disease d : Diseases){
            List<Intermediate> Intermediates = getIntermediateByDisease(d.getId(), connLS);

            // get predicate from predict_relation
            for (Intermediate i : Intermediates){
                Path path = new Path(i.getName(), d.getName());

                // get bc relation (relation type set to 2)
                path.setPredicates(getRelationByIntermediateId(i.getId(), connLS, 2));
                if(!result.keySet().contains(path.getIntermediateName())) {
                    Map<String, Map<String, Integer>> predicate = new HashMap<>();
                    predicate.put(path.getTargetName(), path.getPredicates());
                    result.put(path.getIntermediateName(), predicate);
                }
                else{
                    result.compute(path.getIntermediateName(),
                            (key, value) -> addIntermediateToMap(value, path));
                }
            }
        }
        return result;
    }

    public List<List<Object>> getDetailPath(String drug, int endYear, String disease) throws SQLException {
        int startYear = 1809;

        List<List<Object>> result = new ArrayList<>();
        // get drug id
        DbConnector dbConnector = new DbConnector();
        Connection connLS = dbConnector.getLabelSystemUmlsConnection();
        int predictDrugId =  predictDrugId(drug, startYear, endYear, connLS);
        int predictDiseaseId = predictDiseaseId(predictDrugId, disease, connLS);

        List<Intermediate> Intermediates = getIntermediateByDisease(predictDiseaseId, connLS);

        for (Intermediate i : Intermediates){
            Path path = new Path(i.getName(), disease);

            // get bc relation (relation type set to 2)
            path.setPredicates(getRelationByIntermediateId(i.getId(), connLS, 2));
            for (Map.Entry<String, Integer> m : path.getPredicates().entrySet()){
                List<Object> onePath = new ArrayList<>();
                onePath.add(i.getName());
                onePath.add(m.getKey());
                onePath.add(i.getImportant());
                result.add(onePath);
            }
        }
        return result;
    }

    public Map<String, Map<String, Integer>> addIntermediateToMap(Map<String, Map<String, Integer>> value, Path path){
        value.put(path.getTargetName(), path.getPredicates());
        return value;
    }
    public Map<String, Integer> neighborCount() throws IOException {
        Map<String, Integer> neighborCount = new HashMap<>();
        FileReader fileReader = new FileReader(new File(RepurposingEval.neighborCount));
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] info = line.split("\t");
            neighborCount.put(info[0], Integer.parseInt(info[1]));
        }
        bufferedReader.close();
        fileReader.close();
        return neighborCount;
    }

    public void insertPredictScore(Connection connLS, int predictDrugId,
                                   String disease, double score) {
        String insertSql = "INSERT INTO `" + MySqlDb.predict_score + "` (`predict_drug_id`, `disease`, `score`) VALUES (?, ?, ?);";
        try {
            PreparedStatement psInsert = connLS.prepareStatement(insertSql);
            psInsert.setInt(1, predictDrugId);
            psInsert.setString(2, disease);
            psInsert.setDouble(3, score);
            psInsert.addBatch();
            psInsert.executeBatch();
            psInsert.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertPredictImportantBs(Connection connLS, int predictScoreId, List<String> importantBs) {
        String insertSql = "INSERT INTO `" + MySqlDb.predict_important_bs + "` (`pre_s_id`, `important_b`) VALUES (?, ?);";
        try {
            PreparedStatement psInsert = connLS.prepareStatement(insertSql);
            for (String importantB : importantBs) {
                psInsert.clearParameters();
                psInsert.setInt(1, predictScoreId);
                psInsert.setString(2, importantB);
                psInsert.addBatch();
                psInsert.executeBatch();
            }
            psInsert.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void constructNetwork(boolean runOldModel, int predictDrugId, boolean isDuplicate,
                                 Connection connLit, Connection connLS, Connection connSem,
                                 HashMap<String, String> umlsCuiNameMap,
                                 HashMap<String, String> umlsNameCuiMap,
                                 DBCollection collInfo, DBCollection collRef,
                                 DBCollection collNeighborCoOccurPre, DBCollection collNeighborCoOccurPre_ref) throws SQLException, IOException {
        if (!isDuplicate) {
            logger.info("[Predict] Construct Predict Network");
            Set<String> diseases = new LinkedHashSet<>(UmlsConcept.disorderSeeds);
            LiteratureNode drugPre = new LiteratureNode(getDrug(), startYear, getEndYear(), connSem, connLit);
            HashMap<String, Integer> cooccurNeighbors = drugPre.getCooccurNeighbors(umlsCuiNameMap, collNeighborCoOccurPre, collNeighborCoOccurPre_ref);
            int count = 0;
            ProgressBar pb = new ProgressBar(diseases.size());
            for (String disease : diseases) {
                pb.update();
                if (!cooccurNeighbors.keySet().contains(disease)) {
                    System.out.print("\r" + count++);
                    HashMap<String, HashSet<String>> semGroupsByIntermediate = getSemGroupsByIntermediate(getDrug(), disease, connSem);
                    HashMap<String, HashMap<String, Integer>> semGroupMatrixByIntermediate = getSemGroupMatrixByIntermediate(semGroupsByIntermediate);
                    PredictNetwork predicateNet = new PredictNetwork(runOldModel, getClassifierType(), neighborCount(), getDrug(), disease, startYear, getEndYear(), umlsCuiNameMap, umlsNameCuiMap, connLit, semGroupMatrixByIntermediate, collInfo, collRef);
                    if (predicateNet.isValid()) {
                        predicateNet.setAutoAttrs();
                        predicateNet.predictInterm();
                        IndexScore item = new IndexScore(disease);
                        item.setFeature(predicateNet.getExpectedProb());
                        double score = item.getFeature();
                        // insert disease score into "predict score" table
                        insertPredictScore(connLS, predictDrugId, disease, score);
                        // get predict score id
                        int predictScoreId = predictScoreId(connLS, disease, score);
                        insertPredictImportantBs(connLS, predictScoreId, predicateNet.getImportInterm());
                    }
                }
            }
        }
    }

    public List<DiseaseScore> getDiseaseScore(int predictDrugId, Connection connLS) throws SQLException {
        logger.info("[Score ] Get");

        List<DiseaseScore> DiseaseScores = new ArrayList<>();
        String insertSql = "SELECT `disease`, `score` FROM `" + MySqlDb.predict_score + "` WHERE predict_drug_id =?";
        try {
            PreparedStatement ps = connLS.prepareStatement(insertSql);
            ps.setInt(1, predictDrugId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DiseaseScores.add(new DiseaseScore(
                        rs.getString("disease"),
                        rs.getDouble("score")
                ));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return DiseaseScores;
    }

    public  List<Disease> getDisease(int predictDrugId, Connection connLS) throws SQLException {
        logger.info("[Diseases] Get");

        List<Disease> Diseases = new ArrayList<>();
        String insertSql = "SELECT `id`, `name` FROM `" + MySqlDb.predict_diseases + "` WHERE pre_d_id =?";
        try {
            PreparedStatement ps = connLS.prepareStatement(insertSql);
            ps.setInt(1, predictDrugId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Diseases.add(new Disease(
                    rs.getInt("id"),
                    rs.getString("name")
                ));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Diseases;
    }

    public  List<Intermediate> getIntermediateByDisease(int predictDiseaseId, Connection connLS) throws SQLException {
        logger.info("[Get Intermediate by Disease Id] ");

        List<Intermediate> Intermediates = new ArrayList<>();
        String insertSql = "SELECT `id`, `b_name`, `importance`, `b_id` FROM `" + MySqlDb.predict_intermediate + "` WHERE pre_disease_id =?";
        try {
            PreparedStatement ps = connLS.prepareStatement(insertSql);
            ps.setInt(1, predictDiseaseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Intermediates.add(
                    new Intermediate(
                        rs.getInt("id"),
                        rs.getBoolean("importance"),
                        rs.getString("b_id"),
                        rs.getString("b_name")
                    )
                );
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Intermediates;
    }

    public Map<String, Integer> getRelationByIntermediateId(int predictIntermediateId, Connection connLS, int relationType) throws SQLException{
        logger.info("[ Get Relation by Intermediate Id]");
        Map<String, Integer> predicates = new HashMap<>();

        String insertSql = "SELECT `interaction` FROM `" + MySqlDb.predict_relation + "` WHERE intermediate_id =? and type=?";
        try {
            PreparedStatement ps = connLS.prepareStatement(insertSql);
            ps.setInt(1, predictIntermediateId);
            ps.setInt(2, relationType);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if(!predicates.keySet().contains(rs.getString("interaction"))){
                    predicates.put(rs.getString("interaction"), 1);
                }
                else{
                    predicates.compute(rs.getString("interaction"), (key, value) -> value += 1);
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return predicates;
    }
    public List<Disease> topKDisease(boolean isDuplicate, int predictDrugId,
                                     Connection connLS) {
        List<Disease> topKDiseases = new ArrayList<>();
        if (!isDuplicate) {
            String selSql = "SELECT `id`, `disease` FROM `" + MySqlDb.predict_score + "` WHERE `predict_drug_id`=? ORDER BY `score` DESC LIMIT ?;";
            try {
                PreparedStatement psSel = connLS.prepareStatement(selSql);
                psSel.setInt(1, predictDrugId);
                psSel.setInt(2, getTopK());
                ResultSet rsSel = psSel.executeQuery();
                while (rsSel.next()) {
                    topKDiseases.add(new Disease(
                            rsSel.getInt("id"),
                            rsSel.getString("disease")
                    ));
                }
                rsSel.close();
                psSel.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            logger.info("[Predict] The Predict Case Is Duplicate!");
        }
        return topKDiseases;
    }

    public void insertResults(List<Disease> topKDiseases, boolean runOldModel,
                              int predictDrugId,
                              HashMap<String, String> umlsCuiNameMap,
                              Connection connLS, Connection connSem, Connection connLit) {
        try {
            for (Disease disease : topKDiseases) {
                logger.info("[Predict] Insert Data: Predicted Disease: " + disease.getName());
                String selImportantBs = "SELECT `important_b` FROM `" + MySqlDb.predict_important_bs + "` WHERE `pre_s_id`=?;";
                PreparedStatement psSelImportantBs = connLS.prepareStatement(selImportantBs);
                psSelImportantBs.setInt(1, disease.getId());
                ResultSet rsSelImportantBs = psSelImportantBs.executeQuery();
                List<String> importInterm = new ArrayList<>();
                while (rsSelImportantBs.next()) {
                    importInterm.add(rsSelImportantBs.getString("important_b"));
                }
                rsSelImportantBs.close();
                psSelImportantBs.close();
                SemPathFinder finder = new SemPathFinder(runOldModel, getDrug(), disease.getName(), getEndYear(), -1, importInterm, connSem, connLit, umlsCuiNameMap);
                finder.insertToLabelSystem(connLS, connSem, predictDrugId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, HashMap<String, Integer>> getSemGroupMatrixByIntermediate(HashMap<String, HashSet<String>> semGroupsByIntermediate) {
        HashMap<String, HashMap<String, Integer>> semGroupMatrixByIntermediate = new HashMap<>();
        for (String interm : semGroupsByIntermediate.keySet()) {
            HashSet<String> semGroups = semGroupsByIntermediate.get(interm);
            SemanticType semanticType = new SemanticType();
            HashMap<String, Integer> semGroupAppearedList = semanticType.initSemGroupAppearedList();
            for (String semType : semGroups) {
                semGroupAppearedList.put(semType, 1);
            }
            semGroupMatrixByIntermediate.put(interm, semGroupAppearedList);
        }
        return semGroupMatrixByIntermediate;
    }

    public HashMap<String, HashSet<String>> getSemGroupsByIntermediate(String a_name, String c_name, Connection connSem) {
        HashMap<String, HashSet<String>> semGroupsByIntermediate = new HashMap<>();
        String sqlSelSemType = "select `b_name`, `sg`.`SEMGROUP` from `" + DbConnector.LABELSYS_UMLS + "`.`" + MySqlDb.prediction_view + "` AS `pv` " +
                "inner join `" + DbConnector.LABELSYS_UMLS + "`.`intermediate` as `in` on `pv`.`id`=`in`.`link_id` " +
                "inner join `" + DbConnector.SEMMED + "`.`" + MySqlDb.concept + "` AS `con` on `con`.`PREFERRED_NAME`=`in`.`b_name` " +
                "inner join `" + DbConnector.SEMMED + "`.`" + MySqlDb.concept_semtype + "` AS `cs` on `cs`.`CONCEPT_ID`=`con`.`CONCEPT_ID` " +
                "inner join `" + DbConnector.SEMMED + "`.`" + MySqlDb.semanticType + "` AS `st` on `cs`.`SEMTYPE`=`st`.`SEMTYPE` " +
                "inner join `" + DbConnector.SEMMED + "`.`" + MySqlDb.semanticGroup + "` AS `sg` on `st`.`SEMTYPE_ID`=`sg`.`SEMTYPE_ID` " +
                "where `pv`.`count_b`=`pv`.`label_count` AND `flag`=0 AND `con`.`TYPE`=\"META\" AND `pv`.`a_name`=? AND `pv`.`c_name`=?;";
        Object[] params = {a_name, c_name};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> ls = jdbcHelper.query(connSem, sqlSelSemType, params);
        for (Map<String, Object> row : ls) {
            String intermName = (String) row.get("b_name");
            String semGroup = (String) row.get("SEMGROUP");
            HashSet<String> semGroups = new HashSet<>();
            if (semGroupsByIntermediate.keySet().contains(intermName)) {
                semGroups = semGroupsByIntermediate.get(intermName);
            }
            semGroups.add(semGroup);
            semGroupsByIntermediate.put(intermName, semGroups);
        }
        return semGroupsByIntermediate;
    }

    public boolean examinePredictDrugDuplicate(Connection connLS) {
        logger.info("[Predict] Examine Duplicate Predict Drug");
        String selectSql = "SELECT name, startYear, endYear, classifierType FROM `" + MySqlDb.predict_drug + "`";
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result = jdbcHelper.query(connLS, selectSql);
        boolean isDuplicate = false;
        for (Map row : result) {
            String exitedDrug = (String) row.get("name");
            int exitedStartY = Integer.parseInt(row.get("startYear").toString());
            int exitedEndY = Integer.parseInt(row.get("endYear").toString());
            int classifierType = Integer.parseInt(row.get("classifierType").toString());
            if (getDrug().equals(exitedDrug)
                    && getEndYear() == exitedEndY
                    && startYear == exitedStartY
                    && getClassifierType() == classifierType) {
                isDuplicate = true;
            }
        }
        return isDuplicate;
    }

    public boolean examinePredictCaseDuplicate(Connection connLS, String drug, int endYear) {
        logger.info("[Predict] Examine Duplicate Predict Case");
        String selectSql = "SELECT * FROM `link` WHERE `a_name`=? AND `year`=? AND `flag`=1;";
        boolean isPredictCaseDuplicate = false;
        try {
            PreparedStatement psSel = connLS.prepareStatement(selectSql);
            psSel.setString(1, drug);
            psSel.setDouble(2, endYear);
            ResultSet rsSel = psSel.executeQuery();
            if (rsSel.next()) {
                isPredictCaseDuplicate = true;
            }
            rsSel.close();
            psSel.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isPredictCaseDuplicate;
    }

    public void insertPredictCase(boolean isDuplicate, Connection connLS) {
        if (!isDuplicate) {
            logger.info("[Predict] INSERT INTO predict_drug");
            String insertSql = "INSERT INTO `" + MySqlDb.predict_drug + "` (name, startYear, endYear, classifierType) VALUES(?, ?, ?, ?)";
            Object[] param = {getDrug(), startYear, getEndYear(), getClassifierType()};
            JDBCHelper jdbcHelper = new JDBCHelper();
            jdbcHelper.insert(connLS, insertSql, param);
        } else {
            logger.info("[Predict] The Predict Drug Is Duplicate!");
        }
    }

    public int predictDrugId(Connection connLS) {
        String selSql = "SELECT `id` FROM `" + MySqlDb.predict_drug + "` WHERE `name` LIKE ? AND `startYear`=? AND `endYear`=?;";
        int predictDrugId = 0;
        try {
            PreparedStatement psSel = connLS.prepareStatement(selSql);
            psSel.setString(1, getDrug());
            psSel.setInt(2, startYear);
            psSel.setInt(3, getEndYear());
            ResultSet rsSel = psSel.executeQuery();
            if (rsSel.next()) {
                predictDrugId = rsSel.getInt("id");
            }
            rsSel.close();
            psSel.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return predictDrugId;
    }
    public int predictDiseaseId(int predictDrugId, String disease, Connection connLS){
        String selSql = "SELECT `id` FROM `" + MySqlDb.predict_diseases + "` WHERE `pre_d_id`=? AND `name`=?;";
        int predictDiseaseId = 0;
        try {
            PreparedStatement psSel = connLS.prepareStatement(selSql);
            psSel.setInt(1, predictDrugId);
            psSel.setString(2, disease);
            ResultSet rsSel = psSel.executeQuery();
            if (rsSel.next()) {
                predictDiseaseId = rsSel.getInt("id");
            }
            rsSel.close();
            psSel.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return predictDiseaseId;
    }
    public int predictDrugId(String drug, int startYear, int endYear, Connection connLS) {
        String selSql = "SELECT `id` FROM `" + MySqlDb.predict_drug + "` WHERE `name` LIKE ? AND `startYear`=? AND `endYear`=?;";
        int predictDrugId = 0;
        try {
            PreparedStatement psSel = connLS.prepareStatement(selSql);
            psSel.setString(1, drug);
            psSel.setInt(2, startYear);
            psSel.setInt(3, endYear);
            ResultSet rsSel = psSel.executeQuery();
            if (rsSel.next()) {
                predictDrugId = rsSel.getInt("id");
            }
            rsSel.close();
            psSel.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return predictDrugId;
    }

    public int predictScoreId(Connection connLS, String disease, double score) {
        String selSql = "SELECT `id` FROM `" + MySqlDb.predict_score + "` WHERE `disease` LIKE ? AND `score`=?;";
        int predictScoreId = 0;
        try {
            PreparedStatement psSel = connLS.prepareStatement(selSql);
            psSel.setString(1, disease);
            psSel.setDouble(2, score);
            ResultSet rsSel = psSel.executeQuery();
            if (rsSel.next()) {
                predictScoreId = rsSel.getInt("id");
            }
            rsSel.close();
            psSel.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return predictScoreId;
    }
    //    public List<Intermediate> getIntermediateByScoreId(int predictScoreId, HashMap<String, String> umlsCuiNameMap, Connection connLS){
//        List<Intermediate> Intermediates = new ArrayList<>();
//        String selSql = "SELECT `important_b` FROM `" + MySqlDb.predict_important_bs + "` WHERE `pre_s_id`=?;";
//        try {
//            PreparedStatement psSel = connLS.prepareStatement(selSql);
//            psSel.setInt(1, predictScoreId);
//            ResultSet rsSel = psSel.executeQuery();
//            while (rsSel.next()) {
//                Intermediates.add(new Intermediate(
//                        true,
//                        predictScoreId,
//                        rsSel.getString("important_b"),
//                        umlsCuiNameMap.get(rsSel.getString("important_b"))
//                ));
//            }
//            rsSel.close();
//            psSel.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return Intermediates;
//    }
}
