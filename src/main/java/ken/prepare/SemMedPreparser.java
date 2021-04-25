package ken.prepare;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
//import com.sun.deploy.util.StringUtils;
import org.apache.commons.lang3.StringUtils;
import ken.util.DbConnector;
import ken.util.JDBCHelper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class SemMedPreparser {

    private HashMap<String, Integer> meshNameIdMap = MeshConceptObject.getMeshNameIdMap();

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        SemMedPreparser semPreParser = new SemMedPreparser();
        try {
            semPreParser.create_neighbor_by_predication();
//            semPreParser.create_mesh_predication_aggregate();
            semPreParser.processCooccurNeighborGroupByYear();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Finished.");

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInSecond = totalTime / (1000);
        System.out.println("Time consumed (in seconds): " + totalTimeInSecond);
    }

    public void processMedlineGroupByYear() throws SQLException {

        Connection connectionSemMed = DbConnector.getSemMedConnection();
        //        String pubYearQuerySql = "SELECT pre.`PMID`, pre.`s_name`, pre.`o_name`, cit.`PYEAR` FROM `predication_aggregate` pre, `citations` cit WHERE cit.`PYEAR` = ? AND pre.`PMID` = cit.`PMID`";
        String pubYearQuerySql =
                "select umls.mesh as s_mesh_name, pre.`predicate`,umls2.mesh as o_mesh_name, cit.pyear, "
                        +
                        "pre.`PMID` FROM semmed_ver26.predication_aggregate pre "
                        +
                        "inner join biomedical_concept_mapping.umls_mesh umls on pre.s_cui = umls.cui "
                        +
                        "inner join biomedical_concept_mapping.umls_mesh umls2 on pre.o_cui = umls2.cui "
                        +
                        "inner join semmed_ver26.citations cit on pre.pmid=cit.pmid "
                        +
                        "where cit.pyear = ?";
        PreparedStatement prepStatementSemMed = connectionSemMed.prepareStatement(pubYearQuerySql);

        //        Connection connLit = DbConnector.getLiteratureNodeConnection();
        Connection connLit =
                DbConnector.getInstance().getConnectionByType(DbConnector.LITERATURE_YEAR);
        connLit.setAutoCommit(false);
        String medlinePerYearInsertSql =
                "INSERT INTO `mesh_concept_by_year` (`mesh_id`, `year`, `pmid`, `df`,`freq`) VALUES (? , ?, ?, ?, ?)";
        PreparedStatement prepStatementUmls = connLit.prepareStatement(medlinePerYearInsertSql);

        HashMap<String, Integer> meshNameIdMap = MeshConceptObject.getMeshNameIdMap();
        // Citations were published from 1809 to 2016
        for (int pubYear = 1809; pubYear <= 2016; pubYear++) {
            HashMap<String, MeshConceptYear> meshMedlinesGroupByYear = new HashMap<>();

            System.out.println("===========================================");
            System.out.println("Start query publication year: " + pubYear);
            prepStatementSemMed.clearParameters();
            prepStatementSemMed.setInt(1, pubYear);
            ResultSet umlsResultSet = prepStatementSemMed.executeQuery();
            while (umlsResultSet.next()) {
                String pmid = umlsResultSet.getString("PMID");
                String sName_mesh = umlsResultSet.getString("s_mesh_name");
                String oName_mesh = umlsResultSet.getString("o_mesh_name");

                if (meshMedlinesGroupByYear.containsKey(sName_mesh)) {
                    MeshConceptYear item = meshMedlinesGroupByYear.get(sName_mesh);
                    item.freq += 1;
                    item.pmidSet.add(pmid);
                    meshMedlinesGroupByYear.put(sName_mesh, item);
                } else {
                    MeshConceptYear item = new MeshConceptYear();
                    item.pmidSet.add(pmid);
                    item.freq += 1;
                    meshMedlinesGroupByYear.put(sName_mesh, item);
                }

                if (meshMedlinesGroupByYear.containsKey(oName_mesh)) {
                    MeshConceptYear item = meshMedlinesGroupByYear.get(oName_mesh);
                    item.freq += 1;
                    item.pmidSet.add(pmid);
                    meshMedlinesGroupByYear.put(oName_mesh, item);
                } else {
                    MeshConceptYear item = new MeshConceptYear();
                    item.pmidSet.add(pmid);
                    item.freq += 1;
                    meshMedlinesGroupByYear.put(oName_mesh, item);
                }
            }
            //Insert data entry into database
            int count = 0;
            for (Map.Entry<String, MeshConceptYear> meshEntry : meshMedlinesGroupByYear.entrySet()) {
                String meshName = meshEntry.getKey();
                MeshConceptYear meshInfo = meshEntry.getValue();
                int df = meshInfo.pmidSet.size();
                int freq = meshInfo.freq;
                int meshId = meshNameIdMap.get(meshName);
                //                System.out.println(meshName + " : " + (++count) + "/" + totalSeeds);

                prepStatementUmls.clearParameters();
                prepStatementUmls.setInt(1, meshId);
                prepStatementUmls.setInt(2, pubYear);
                prepStatementUmls.setString(3, StringUtils.join(meshInfo.pmidSet, ","));
                prepStatementUmls.setInt(4, df);
                prepStatementUmls.setInt(5, freq);
                prepStatementUmls.addBatch();
                if (++count % 2000 == 0) {
                    prepStatementUmls.executeBatch();
                    connLit.commit();
                }
            }
        }
        prepStatementUmls.executeBatch();
        connLit.commit();
        prepStatementUmls.close();
        connLit.close();
        prepStatementSemMed.close();
        connectionSemMed.close();
    }


    /**
     * Use on literature before 1970.
     *
     * @throws SQLException
     * @throws IOException
     */
    public void processCooccurNeighborGroupByYear() throws SQLException {
        Logger logger = Logger.getLogger("processNeighborGroupByYear");
        //Citations were published from 1809 to 2016
        final int processStartYear = 1809;
        final int processEndYear = 2016;

        Connection connSemMed = DbConnector.getSemMedConnection();
        String pubYearQuerySql =
                "select umls.mesh as s_mesh_name, pre.`predicate`,umls2.mesh as o_mesh_name, cit.pyear, "
                        +
                        "pre.`PMID` FROM semmed_ver26.predication_aggregate pre "
                        +
                        "inner join biomedical_concept_mapping.umls_mesh umls on pre.s_cui = umls.cui "
                        +
                        "inner join biomedical_concept_mapping.umls_mesh umls2 on pre.o_cui = umls2.cui "
                        +
                        "inner join semmed_ver26.citations cit on pre.pmid=cit.pmid "
                        +
                        "where cit.pyear = ?";

        PreparedStatement prepStatementSemMed = connSemMed.prepareStatement(pubYearQuerySql);

        Connection connLit = DbConnector.getLiteratureNodeConnection();
        connLit.setAutoCommit(false);

        String medlinePerYearInsertSql =
                "INSERT INTO `neighbor_cooccur` (`mesh_id`, `year`, `neighbor`, `freq`) VALUES ( ?, ?, ?, ?)";
        PreparedStatement prepStatementMesh = connLit.prepareStatement(medlinePerYearInsertSql);
        //Citations were published from 1809 to 2016
        for (int pubYear = processStartYear; pubYear <= processEndYear; pubYear++) {
            HashMap<String, LinkedHashSet<Integer>> pmidMeshByYearMap = new HashMap<>();

            System.out.println("===========================================");
            logger.info("Start query publication year: " + pubYear);
            prepStatementSemMed.clearParameters();
            prepStatementSemMed.setInt(1, pubYear);
            ResultSet umlsResultSet = prepStatementSemMed.executeQuery();
            while (umlsResultSet.next()) {
                String pmid = umlsResultSet.getString("PMID");
                int sMeshId = meshNameIdMap.get(umlsResultSet.getString("s_mesh_name"));
                int oMeshId = meshNameIdMap.get(umlsResultSet.getString("o_mesh_name"));
                LinkedHashSet<Integer> hs;
                if (pmidMeshByYearMap.containsKey(pmid)) {
                    hs = pmidMeshByYearMap.get(pmid);
                } else {
                    hs = new LinkedHashSet<>();
                }
                hs.add(sMeshId);
                hs.add(oMeshId);
                pmidMeshByYearMap.put(pmid, hs);
            }
            umlsResultSet.close();
            HashMap<Tuple, Integer> tupleMap = new HashMap<>();
            for (String pmid : pmidMeshByYearMap.keySet()) {
                LinkedHashSet<Integer> meshIdList = pmidMeshByYearMap.get(pmid);
                HashSet<Tuple> tupleSet = new HashSet<>();
                for (Integer meshId1 : meshIdList) {
                    for (Integer meshId2 : meshIdList) {
                        if (meshId1.equals(meshId2)) continue;
                        Tuple tuple = new Tuple(meshId1, meshId2);
                        tupleSet.add(tuple);
                    }
                }
                for (Tuple tuple : tupleSet) {
                    if (tupleMap.containsKey(tuple)) {
                        tupleMap.put(tuple, tupleMap.get(tuple) + 1);
                    } else {
                        tupleMap.put(tuple, 1);
                    }
                }
            }
            //Insert into database
            int count=0;
            for (Map.Entry<Tuple, Integer> tupleEntry : tupleMap.entrySet()) {
                Tuple tuple = tupleEntry.getKey();
                int freq = tupleEntry.getValue();
                prepStatementMesh.clearParameters();
                prepStatementMesh.setInt(1, tuple.meshId1);
                prepStatementMesh.setInt(2, pubYear);
                prepStatementMesh.setInt(3, tuple.meshId2);
                prepStatementMesh.setInt(4, freq);
                prepStatementMesh.addBatch();
                prepStatementMesh.clearParameters();
                prepStatementMesh.setInt(1, tuple.meshId2);
                prepStatementMesh.setInt(2, pubYear);
                prepStatementMesh.setInt(3, tuple.meshId1);
                prepStatementMesh.setInt(4, freq);
                prepStatementMesh.addBatch();
                if(++count%10000==0){
                    prepStatementMesh.executeBatch();
                    connLit.commit();
                }
            }
            prepStatementMesh.executeBatch();
            connLit.commit();
        }

        prepStatementSemMed.close();
        prepStatementMesh.close();
        connLit.close();
        logger.info("##Finish!");
    }

    public void create_neighbor_by_predication() throws SQLException {
        Logger logger = Logger.getLogger("processPredicateNeighborGroupByYear_Together");
        //Citations were published from 1809 to 2016
        final int processStartYear = 1809;
        final int processEndYear = 2016;
        String querySql = "SELECT umls.seed_id as s_mesh_id, pre.`predicate`,umls2.seed_id as o_mesh_id,cit.pyear, "
                + "pre.`PMID` FROM predication_aggregate pre "
                + "INNER JOIN biomedical_concept_mapping.umls_mesh umls ON pre.s_cui = umls.cui "
                + "INNER JOIN biomedical_concept_mapping.umls_mesh umls2 ON pre.o_cui = umls2.cui "
                + "INNER JOIN citations cit ON pre.pmid=cit.pmid "
                + "WHERE cit.pyear = ?";
        String insertSql =
                "INSERT INTO `neighbor_by_predication` (`mesh_id`, `year`, `neighbor`, `freq`) VALUES (?, ?, ?, ?)";
        Connection connLit = DbConnector.getLiteratureNodeConnection();
        connLit.setAutoCommit(false);
        PreparedStatement prepStatementMesh = connLit.prepareStatement(insertSql);
        for (int pubYear = processStartYear; pubYear <= processEndYear; pubYear++) {
            HashMap<Integer, Multiset<Integer>> neighborsMap = new HashMap<>();
            logger.info("Start query publication year: " + pubYear);
            int count = 0;
            List<Map<String, Object>> result =
                    JDBCHelper.query(DbConnector.SEMMED, querySql, pubYear);
            for (Map row : result) {
                int s_meshId = (int) row.get("s_mesh_id");
                int o_meshId = (int) row.get("o_mesh_id");
                Multiset<Integer> neighbor = neighborsMap.getOrDefault(s_meshId, HashMultiset.create());
                neighbor.add(o_meshId);
                neighborsMap.put(s_meshId, neighbor);
            }

            for (Integer meshId : neighborsMap.keySet()) {
                for (Multiset.Entry<Integer> neighbor : neighborsMap.get(meshId).entrySet()) {
                    prepStatementMesh.clearParameters();
                    prepStatementMesh.setInt(1, meshId);
                    prepStatementMesh.setInt(2, pubYear);
                    prepStatementMesh.setInt(3, neighbor.getElement());
                    prepStatementMesh.setInt(4, neighbor.getCount());
                    prepStatementMesh.addBatch();
                    /*a is b's neighbor == b is a's neighbor
                    if a==b => only need inserting once,
                    else => insert a as b's neighbor
                     */
                    if (meshId.intValue() != neighbor.getElement()) {
                        prepStatementMesh.clearParameters();
                        prepStatementMesh.setInt(1, neighbor.getElement());
                        prepStatementMesh.setInt(2, pubYear);
                        prepStatementMesh.setInt(3, meshId);
                        prepStatementMesh.setInt(4, neighbor.getCount());
                        prepStatementMesh.addBatch();
                    }
                }
                if (++count % 2000 == 0) {
                    prepStatementMesh.executeBatch();
                    connLit.commit();
                }
            }
        }
        prepStatementMesh.executeBatch();
        connLit.commit();
        prepStatementMesh.close();
        connLit.close();
    }

    public void create_mesh_predication_aggregate() throws SQLException {
        Logger logger = Logger.getLogger("processPredicationToMeshRelation");
        //Citations were published from 1809 to 2016
        final int processStartYear = 1809;
        //        final int processStartYear = 1993;
        final int processEndYear = 2016;

        Connection connLit = DbConnector.getLiteratureNodeConnection();
        connLit.setAutoCommit(false);
        String insertSql =
                "INSERT INTO `mesh_predication_aggregate` (`s_mesh_id`, `s_novel`,`predicate`,`o_mesh_id`," +
                        "`o_novel`,`pmid`, `year`,`pid`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement prepStatementMesh = connLit.prepareStatement(insertSql);
        String querySql = "SELECT umls.`seed_id` as s_mesh_id, " +
                "umls2.`seed_id` as o_mesh_id, " +
                "pre.`predicate`, pre.`PMID`,pre.`PID`,pre.s_novel, pre.o_novel " +
                "FROM `predication_aggregate` pre " +
                "inner join biomedical_concept_mapping.umls_mesh umls on pre.s_cui = umls.cui " +
                "inner join biomedical_concept_mapping.umls_mesh umls2 on pre.o_cui = umls2.cui " +
                "inner join citations cit on pre.pmid=cit.pmid " +
                "WHERE cit.pyear = ?";

        //Citations were published from 1809 to 2016
        for (int pubYear = processStartYear; pubYear <= processEndYear; pubYear++) {
            logger.info("Start query publication year: " + pubYear);
            int count = 0;
            List<Map<String, Object>> result =
                    JDBCHelper.query(DbConnector.SEMMED, querySql, pubYear);
            for (Map row : result) {
                int s_meshId = (int) row.get("s_mesh_id");
                int o_meshId = (int) row.get("o_mesh_id");
                prepStatementMesh.clearParameters();
                prepStatementMesh.setInt(1, s_meshId);
                boolean s_novel = row.get("s_novel") != null && (boolean) row.get("s_novel");
                boolean o_novel = row.get("o_novel") != null && (boolean) row.get("o_novel");
                prepStatementMesh.setInt(2, s_novel ? 1 : 0);
                prepStatementMesh.setString(3, (String) row.get("predicate"));
                prepStatementMesh.setInt(4, o_meshId);
                prepStatementMesh.setInt(5, o_novel ? 1 : 0);
                prepStatementMesh.setString(6, (String) row.get("PMID"));
                prepStatementMesh.setInt(7, pubYear);
                prepStatementMesh.setInt(8, ((Long) row.get("PID")).intValue());
                prepStatementMesh.addBatch();

                prepStatementMesh.clearParameters();
                prepStatementMesh.setInt(1, o_meshId);
                prepStatementMesh.setInt(2, o_novel ? 1 : 0);
                prepStatementMesh.setString(3, (String) row.get("predicate"));
                prepStatementMesh.setInt(4, s_meshId);
                prepStatementMesh.setInt(5, s_novel ? 1 : 0);
                prepStatementMesh.setString(6, (String) row.get("PMID"));
                prepStatementMesh.setInt(7, pubYear);
                prepStatementMesh.setInt(8, ((Long) row.get("PID")).intValue());
                prepStatementMesh.addBatch();
                if (++count % 2000 == 0) {
                    prepStatementMesh.executeBatch();
                    connLit.commit();
                }
            }
        }

        prepStatementMesh.executeBatch();
        connLit.commit();
        prepStatementMesh.close();
        connLit.close();
        logger.info("##Finish!");
    }

    public void tmp() throws SQLException {
        Logger logger = Logger.getLogger("processPredicationToMeshRelation");
        //Citations were published from 1809 to 2016
        final int processStartYear = 1809;
        //        final int processStartYear = 1993;
        final int processEndYear = 2016;

        Connection connLit = DbConnector.getLiteratureNodeConnection();
        connLit.setAutoCommit(false);
        String insertSql =
                "INSERT INTO `mesh_predication_aggregate` (`s_mesh_id`, `s_novel`,`predicate`,`o_mesh_id`," +
                        "`o_novel`,`pmid`, `year`,`pid`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement prepStatementMesh = connLit.prepareStatement(insertSql);
        String querySql = "SELECT * FROM mesh_predication_aggregate_once WHERE s_mesh_id=?";

        //Citations were published from 1809 to 2016
        for (int mesh_id = 1; mesh_id <= 17264; mesh_id++) {
            logger.info("Start query mesh_id: " + mesh_id);
            int count = 0;
            List<Map<String, Object>> result =
                    JDBCHelper.query(DbConnector.LITERATURE_YEAR, querySql, mesh_id);
            for (Map row : result) {
                int s_meshId = (int) row.get("s_mesh_id");
                int o_meshId = (int) row.get("o_mesh_id");
                if (s_meshId == o_meshId) continue;
                prepStatementMesh.clearParameters();
                prepStatementMesh.setInt(1, s_meshId);
                prepStatementMesh.setInt(2, (int) row.get("s_novel"));
                prepStatementMesh.setString(3, (String) row.get("predicate"));
                prepStatementMesh.setInt(4, o_meshId);
                prepStatementMesh.setInt(5, (int) row.get("o_novel"));
                prepStatementMesh.setString(6, (String) row.get("pmid"));
                prepStatementMesh.setInt(7, (int) row.get("year"));
                prepStatementMesh.setInt(8, (int) row.get("pid"));
                prepStatementMesh.addBatch();

                prepStatementMesh.clearParameters();
                prepStatementMesh.setInt(1, o_meshId);
                prepStatementMesh.setInt(2, (int) row.get("o_novel"));
                prepStatementMesh.setString(3, (String) row.get("predicate"));
                prepStatementMesh.setInt(4, s_meshId);
                prepStatementMesh.setInt(5, (int) row.get("s_novel"));
                prepStatementMesh.setString(6, (String) row.get("pmid"));
                prepStatementMesh.setInt(7, (int) row.get("year"));
                prepStatementMesh.setInt(8, (int) row.get("pid"));
                prepStatementMesh.addBatch();
                if (++count % 1000 == 0) {
                    prepStatementMesh.executeBatch();
                    connLit.commit();
                }
            }
        }

        prepStatementMesh.executeBatch();
        connLit.commit();
        prepStatementMesh.close();
        connLit.close();
        logger.info("##Finish!");
    }

    public void createUmlsMeshBasedonSeed() {
        String umls = "SELECT * FROM umls_mesh ORDER BY mesh";
        String seed = "SELECT * FROM mesh_seeds ORDER BY name";
        String insert =
                "INSERT INTO umls_mesh_seed (cui,preferred_name,mesh,seed_id) values(?,?,?,?)";
        ArrayList<Map<String, Object>> umlsLs = new ArrayList<>();
        HashSet<String> seedMap = new HashSet<>();
        HashMap<String, Integer> meshIdMap = new HashMap<>();
        List<Map<String, Object>> umlsRs = JDBCHelper.query(DbConnector.BIOCONCEPT, umls);
        List<Map<String, Object>> seedRs = JDBCHelper.query(DbConnector.LITERATURE_YEAR, seed);
        for (Map<String, Object> row : seedRs) {
            String mesh = (String) row.get("name");
            seedMap.add(mesh);
            meshIdMap.put(mesh, (int) row.get("id"));
        }
        for (Map<String, Object> row : umlsRs) {
            String mesh = (String) row.get("mesh");
            if (seedMap.contains(mesh)) {
                row.put("seed_id", meshIdMap.get(mesh));
                umlsLs.add(row);
            }
        }
        Object[][] params = new Object[umlsLs.size()][umlsLs.get(0).size()];
        int i = 0;
        for (Map<String, Object> row : umlsLs) {
            params[i][0] = (String) row.get("CUI");
            params[i][1] = (String) row.get("preferred_name");
            params[i][2] = (String) row.get("mesh");
            params[i][3] = (int) row.get("seed_id");
            i++;
        }
        JDBCHelper.updateBatch(DbConnector.BIOCONCEPT, insert, params);
    }

    class MeshConceptYear {
        int freq;
        LinkedHashSet<String> pmidSet;

        MeshConceptYear() {
            freq = 0;
            pmidSet = new LinkedHashSet<>();
        }
    }

    class Tuple {
        int meshId1;
        int meshId2;

        Tuple(int meshId1, int meshId2) {
            this.meshId1 = meshId1;
            this.meshId2 = meshId2;
        }

        @Override
        public boolean equals(Object that) {
            if (that instanceof Tuple) {
                Tuple p = (Tuple) that;
                if ((this.meshId1 == p.meshId1) && (this.meshId2 == p.meshId2)) {
                    return true;
                } else if ((this.meshId2 == p.meshId1) && (this.meshId1 == p.meshId2)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.meshId1 + this.meshId2;
        }
    }
}
