package weng.parser;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
// import com.sun.deploy.util.StringUtils;
import weng.util.DbConnector;
import weng.util.JDBCHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class SemMedPreparser {

    public SemMedPreparser() {
    }

    public void createUMLSPmidCount(Connection connLit) throws SQLException {
        Logger logger = Logger.getLogger("createUMLSPmidCount");
        logger.info("[createUMLSPmidCount] Start");

        logger.info("[createUMLSPmidCount] Select");
        String selSql = "SELECT `pmid`, COUNT(*) AS `count` FROM `umls_predication_aggregate_filtered` WHERE `is_exist`=1 GROUP BY `pmid`;";
        PreparedStatement psSel = connLit.prepareStatement(selSql);
        ResultSet rsSel = psSel.executeQuery();
        HashMap<Integer, Integer> pmidCount = new HashMap<>();
        while (rsSel.next()) {
            int pmid = rsSel.getInt("pmid");
            int count = rsSel.getInt("count");
            pmidCount.put(pmid, count);
        }
        rsSel.close();
        psSel.close();

        logger.info("[createUMLSPmidCount] Insert");
        String insertSql = "INSERT IGNORE INTO `pmid_count_umls` (`pmid`, `count`) VALUES (?, ?)";
        PreparedStatement psInsert = connLit.prepareStatement(insertSql);
        int line_count = 1;
        for (Integer pmid : pmidCount.keySet()) {
            int count = pmidCount.get(pmid);
            psInsert.clearParameters();
            psInsert.setInt(1, pmid);
            psInsert.setInt(2, count);
            psInsert.addBatch();

            int remainder = line_count % 20000;
            if (remainder == 0) {
                System.out.println("----------line_count: " + line_count + " execute batch...-----------");
                psInsert.executeBatch();
            }
            line_count++;
        }
        psInsert.executeBatch();
        psInsert.close();
        logger.info("[createUMLSPmidCount] Finish");
    }

    public void createUMLSPredicationAggregate(Connection connSem, Connection connLit) throws SQLException {
        Logger logger = Logger.getLogger("createUMLSPredicationAggregate");
        logger.info("[createUMLSPredicationAggregate] Start");
        System.out.println("[createUMLSPredicationAggregate] Start");
        final int processStartYear = 1809; // Citations were published from 1809 to 2016
        final int processEndYear = 2016;

        String querySql = "SELECT `s_cui`, `o_cui`, `predicate`, `pre`.`PMID` AS `pmid`,`PID`, " +
                "`s_novel`, `o_novel` FROM `predication_aggregate` as `pre` inner join `citations` " +
                "as `cit` on `pre`.`PMID`=`cit`.`PMID` WHERE `cit`.`PYEAR`=?";
        PreparedStatement psSelUMLSPrediAggre = connSem.prepareStatement(querySql);
        String insertSql =
                "INSERT INTO `umls_predication_aggregate` (`s_cui`, `s_novel`,`predicate`,`o_cui`," +
                        "`o_novel`,`pmid`, `year`,`pid`,`is_exist`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement psUMLSPredicationAggregate = connLit.prepareStatement(insertSql);
        for (int pubYear = processStartYear; pubYear <= processEndYear; pubYear++) {
            int count = 0;
            logger.info("Start query publication year: " + pubYear);
            psSelUMLSPrediAggre.clearParameters();
            psSelUMLSPrediAggre.setInt(1, pubYear);
            ResultSet rsUMLSPrediAggre = psSelUMLSPrediAggre.executeQuery();
            while (rsUMLSPrediAggre.next()) {
                String s_cui = rsUMLSPrediAggre.getString("s_cui");
                boolean isS_novel = rsUMLSPrediAggre.getString("s_novel") != null;
                int s_novel = isS_novel ? 1 : 0;
                String predicate = rsUMLSPrediAggre.getString("predicate");
                String o_cui = rsUMLSPrediAggre.getString("o_cui");
                boolean isO_novel = rsUMLSPrediAggre.getString("o_novel") != null;
                int o_novel = isO_novel ? 1 : 0;
                String pmid = rsUMLSPrediAggre.getString("pmid");
                int pid = rsUMLSPrediAggre.getInt("PID");
                int isExist = 1;

                psUMLSPredicationAggregate.clearParameters();
                psUMLSPredicationAggregate.setString(1, s_cui);
                psUMLSPredicationAggregate.setInt(2, s_novel);
                psUMLSPredicationAggregate.setString(3, predicate);
                psUMLSPredicationAggregate.setString(4, o_cui);
                psUMLSPredicationAggregate.setInt(5, o_novel);
                psUMLSPredicationAggregate.setString(6, pmid);
                psUMLSPredicationAggregate.setInt(7, pubYear);
                psUMLSPredicationAggregate.setInt(8, pid);
                psUMLSPredicationAggregate.setInt(9, isExist);
                psUMLSPredicationAggregate.addBatch();

                isExist = 0;
                psUMLSPredicationAggregate.clearParameters();
                psUMLSPredicationAggregate.setString(1, o_cui);
                psUMLSPredicationAggregate.setInt(2, o_novel);
                psUMLSPredicationAggregate.setString(3, predicate);
                psUMLSPredicationAggregate.setString(4, s_cui);
                psUMLSPredicationAggregate.setInt(5, s_novel);
                psUMLSPredicationAggregate.setString(6, pmid);
                psUMLSPredicationAggregate.setInt(7, pubYear);
                psUMLSPredicationAggregate.setInt(8, pid);
                psUMLSPredicationAggregate.setInt(9, isExist);
                psUMLSPredicationAggregate.addBatch();
                if (++count % 5000 == 0) {
                    psUMLSPredicationAggregate.executeBatch();
                }
            }
            rsUMLSPrediAggre.close();
        }
        psUMLSPredicationAggregate.executeBatch();
        psUMLSPredicationAggregate.close();
        psSelUMLSPrediAggre.close();
        logger.info("[createUMLSPredicationAggregate] Finish");
        System.out.println("[createUMLSPredicationAggregate] Finish");
    }

    public void createUMLSPredicationAggregateFilteredBySemType(Connection connSem, Connection connLit) throws SQLException {
        Logger logger = Logger.getLogger("createUMLSPredicationAggregateFilteredBySemType");
        logger.info("[createUMLSPredicationAggregateFilteredBySemType] Start");
        System.out.println("[createUMLSPredicationAggregateFilteredBySemType] Start");
        final int processStartYear = 1809; // Citations were published from 1809 to 2016
        final int processEndYear = 2016;

        String querySql = "SELECT `s_cui`, `o_cui`, `predicate`, `pre`.`PMID` AS `pmid`,`PID`, " +
                "`s_novel`, `o_novel` FROM `predication_aggregate` as `pre` inner join `citations` as `cit` " +
                "on `pre`.`PMID`=`cit`.`PMID` WHERE (`s_type`=\"aapp\" or `s_type`=\"antb\" or `s_type`=\"bacs\" " +
                "or `s_type`=\"bodm\" or `s_type`=\"carb\" or `s_type`=\"chem\" or `s_type`=\"chvf\" or `s_type`=\"chvs\" " +
                "or `s_type`=\"clnd\" or `s_type`=\"eico\" or `s_type`=\"elii\" or `s_type`=\"enzy\" or `s_type`=\"hops\" " +
                "or `s_type`=\"horm\" or `s_type`=\"imft\" or `s_type`=\"irda\" or `s_type`=\"inch\" or `s_type`=\"lipd\" " +
                "or `s_type`=\"nsba\" or `s_type`=\"nnon\" or `s_type`=\"orch\" or `s_type`=\"opco\" or `s_type`=\"phsu\" " +
                "or `s_type`=\"rcpt\" or `s_type`=\"strd\" or `s_type`=\"vita\" or `s_type`=\"acab\" or `s_type`=\"anab\" " +
                "or `s_type`=\"comd\" or `s_type`=\"cgab\" or `s_type`=\"dsyn\" or `s_type`=\"emod\" or `s_type`=\"fndg\" " +
                "or `s_type`=\"inpo\" or `s_type`=\"mobd\" or `s_type`=\"neop\" or `s_type`=\"patf\" or `s_type`=\"sosy\" " +
                "or `s_type`=\"amas\" or `s_type`=\"crbs\" or `s_type`=\"gngm\" or `s_type`=\"mosq\" or `s_type`=\"nusq\" " +
                "or `s_type`=\"celf\" or `s_type`=\"clna\" or `s_type`=\"genf\" or `s_type`=\"menp\" or `s_type`=\"moft\" " +
                "or `s_type`=\"orga\" or `s_type`=\"orgf\" or `s_type`=\"ortf\" or `s_type`=\"phsf\" or `s_type`=\"anst\" " +
                "or `s_type`=\"blor\" or `s_type`=\"bpoc\" or `s_type`=\"bsoj\" or `s_type`=\"bdsu\" or `s_type`=\"bdsy\" " +
                "or `s_type`=\"cell\" or `s_type`=\"celc\" or `s_type`=\"emst\" or `s_type`=\"ffas\" or `s_type`=\"tisu\") " +
                "and (`o_type`=\"aapp\" or `o_type`=\"antb\" or `o_type`=\"bacs\" or `o_type`=\"bodm\" or `o_type`=\"carb\" " +
                "or `o_type`=\"chem\" or `o_type`=\"chvf\" or `o_type`=\"chvs\" or `o_type`=\"clnd\" or `o_type`=\"eico\" " +
                "or `o_type`=\"elii\" or `o_type`=\"enzy\" or `o_type`=\"hops\" or `o_type`=\"horm\" or `o_type`=\"imft\" " +
                "or `o_type`=\"irda\" or `o_type`=\"inch\" or `o_type`=\"lipd\" or `o_type`=\"nsba\" or `o_type`=\"nnon\" " +
                "or `o_type`=\"orch\" or `o_type`=\"opco\" or `o_type`=\"phsu\" or `o_type`=\"rcpt\" or `o_type`=\"strd\" " +
                "or `o_type`=\"vita\" or `o_type`=\"acab\" or `o_type`=\"anab\" or `o_type`=\"comd\" or `o_type`=\"cgab\" " +
                "or `o_type`=\"dsyn\" or `o_type`=\"emod\" or `o_type`=\"fndg\" or `o_type`=\"inpo\" or `o_type`=\"mobd\" " +
                "or `o_type`=\"neop\" or `o_type`=\"patf\" or `o_type`=\"sosy\" or `o_type`=\"amas\" or `o_type`=\"crbs\" " +
                "or `o_type`=\"gngm\" or `o_type`=\"mosq\" or `o_type`=\"nusq\" or `o_type`=\"celf\" or `o_type`=\"clna\" " +
                "or `o_type`=\"genf\" or `o_type`=\"menp\" or `o_type`=\"moft\" or `o_type`=\"orga\" or `o_type`=\"orgf\" " +
                "or `o_type`=\"ortf\" or `o_type`=\"phsf\" or `o_type`=\"anst\" or `o_type`=\"blor\" or `o_type`=\"bpoc\" " +
                "or `o_type`=\"bsoj\" or `o_type`=\"bdsu\" or `o_type`=\"bdsy\" or `o_type`=\"cell\" or `o_type`=\"celc\" " +
                "or `o_type`=\"emst\" or `o_type`=\"ffas\" or `o_type`=\"tisu\") and `cit`.`PYEAR`=?";
        PreparedStatement psSelUMLSPrediAggre = connSem.prepareStatement(querySql);
        String insertSql =
                "INSERT INTO `umls_predication_aggregate_filtered` (`s_cui`, `s_novel`,`predicate`,`o_cui`," +
                        "`o_novel`,`pmid`, `year`,`pid`,`is_exist`) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement psUMLSPredicationAggregate = connLit.prepareStatement(insertSql);
        for (int pubYear = processStartYear; pubYear <= processEndYear; pubYear++) {
            int count = 0;
            logger.info("Start query publication year: " + pubYear);
            psSelUMLSPrediAggre.clearParameters();
            psSelUMLSPrediAggre.setInt(1, pubYear);
            ResultSet rsUMLSPrediAggre = psSelUMLSPrediAggre.executeQuery();
            while (rsUMLSPrediAggre.next()) {
                String s_cui = rsUMLSPrediAggre.getString("s_cui");
                boolean isS_novel = rsUMLSPrediAggre.getString("s_novel") != null;
                int s_novel = isS_novel ? 1 : 0;
                String predicate = rsUMLSPrediAggre.getString("predicate");
                String o_cui = rsUMLSPrediAggre.getString("o_cui");
                boolean isO_novel = rsUMLSPrediAggre.getString("o_novel") != null;
                int o_novel = isO_novel ? 1 : 0;
                String pmid = rsUMLSPrediAggre.getString("pmid");
                int pid = rsUMLSPrediAggre.getInt("PID");
                int isExist = 1;

                psUMLSPredicationAggregate.clearParameters();
                psUMLSPredicationAggregate.setString(1, s_cui);
                psUMLSPredicationAggregate.setInt(2, s_novel);
                psUMLSPredicationAggregate.setString(3, predicate);
                psUMLSPredicationAggregate.setString(4, o_cui);
                psUMLSPredicationAggregate.setInt(5, o_novel);
                psUMLSPredicationAggregate.setString(6, pmid);
                psUMLSPredicationAggregate.setInt(7, pubYear);
                psUMLSPredicationAggregate.setInt(8, pid);
                psUMLSPredicationAggregate.setInt(9, isExist);
                psUMLSPredicationAggregate.addBatch();

                isExist = 0;
                psUMLSPredicationAggregate.clearParameters();
                psUMLSPredicationAggregate.setString(1, o_cui);
                psUMLSPredicationAggregate.setInt(2, o_novel);
                psUMLSPredicationAggregate.setString(3, predicate);
                psUMLSPredicationAggregate.setString(4, s_cui);
                psUMLSPredicationAggregate.setInt(5, s_novel);
                psUMLSPredicationAggregate.setString(6, pmid);
                psUMLSPredicationAggregate.setInt(7, pubYear);
                psUMLSPredicationAggregate.setInt(8, pid);
                psUMLSPredicationAggregate.setInt(9, isExist);
                psUMLSPredicationAggregate.addBatch();
                if (++count % 5000 == 0) {
                    psUMLSPredicationAggregate.executeBatch();
                }
            }
            rsUMLSPrediAggre.close();
        }
        psUMLSPredicationAggregate.executeBatch();
        psUMLSPredicationAggregate.close();
        psSelUMLSPrediAggre.close();
        logger.info("[createUMLSPredicationAggregateFilteredBySemType] Finish");
        System.out.println("[createUMLSPredicationAggregateFilteredBySemType] Finish");
    }

    public void processMedlineGroupByYear(Connection connSem, Connection connLit) throws SQLException {
        String pubYearQuerySql = "select `s_cui`, `predicate`, `o_cui`, `PYEAR`, `pre`.`PMID` " +
                "FROM `predication_aggregate` AS `pre` inner join `citations` AS `cit` " +
                "on `pre`.`PMID`=`cit`.`PMID` where `cit`.`PYEAR`=?;";
        PreparedStatement prepStatementSemMed = connSem.prepareStatement(pubYearQuerySql);
        String medlinePerYearInsertSql =
                "INSERT INTO `umls_concept_by_year` (`cui`, `year`, `pmid`, `df`, `freq`) VALUES (? , ?, ?, ?, ?)";
        PreparedStatement prepStatementUmls = connLit.prepareStatement(medlinePerYearInsertSql);

        // Citations were published from 1809 to 2016
        for (int pubYear = 1809; pubYear <= 2016; pubYear++) {
            HashMap<String, UmlsConceptYear> umlsMedlinesGroupByYear = new HashMap<>();

            System.out.println("===========================================");
            System.out.println("Start query publication year: " + pubYear);
            prepStatementSemMed.clearParameters();
            prepStatementSemMed.setInt(1, pubYear);
            ResultSet umlsResultSet = prepStatementSemMed.executeQuery();
            while (umlsResultSet.next()) {
                String pmid = umlsResultSet.getString("PMID");
                String sCui = umlsResultSet.getString("s_cui");
                String oCui = umlsResultSet.getString("o_cui");

                UmlsConceptYear item = new UmlsConceptYear();
                if (umlsMedlinesGroupByYear.containsKey(sCui)) {
                    item = umlsMedlinesGroupByYear.get(sCui);
                }
                int freq = item.getFreq();
                freq += 1;
                item.setFreq(freq);
                LinkedHashSet<String> pmidSet = item.getPmidSet();
                pmidSet.add(pmid);
                item.setPmidSet(pmidSet);
                umlsMedlinesGroupByYear.put(sCui, item);

                item = new UmlsConceptYear();
                if (umlsMedlinesGroupByYear.containsKey(oCui)) {
                    item = umlsMedlinesGroupByYear.get(oCui);
                }
                freq = item.getFreq();
                freq += 1;
                item.setFreq(freq);
                pmidSet = item.getPmidSet();
                pmidSet.add(pmid);
                item.setPmidSet(pmidSet);
                umlsMedlinesGroupByYear.put(oCui, item);
            }

            //Insert data entry into database
            int count = 0;
            for (Map.Entry<String, UmlsConceptYear> umlsEntry : umlsMedlinesGroupByYear.entrySet()) {
                String cui = umlsEntry.getKey();
                UmlsConceptYear umlsInfo = umlsEntry.getValue();
                int df = umlsInfo.getPmidSet().size();
                int freq = umlsInfo.getFreq();

                prepStatementUmls.clearParameters();
                prepStatementUmls.setString(1, cui);
                prepStatementUmls.setInt(2, pubYear);
                // prepStatementUmls.setString(3, StringUtils.join(umlsInfo.getPmidSet(), ","));
                prepStatementUmls.setInt(4, df);
                prepStatementUmls.setInt(5, freq);
                prepStatementUmls.addBatch();
                if (++count % 5000 == 0) {
                    prepStatementUmls.executeBatch();
                }
            }
        }
        prepStatementUmls.executeBatch();
        prepStatementUmls.close();
        prepStatementSemMed.close();
    }

    public void createUMLSNeighborByPredication(Connection connLit) throws SQLException {
        Logger logger = Logger.getLogger("processUMLSPredicateNeighborGroupByYear_Together");
        //Citations were published from 1809 to 2016
        final int processStartYear = 1809;
        final int processEndYear = 2016;
        String querySql = "SELECT `s_cui`, `predicate`, `o_cui`, `PYEAR`, `pre`.`PMID` FROM `predication_aggregate` " +
                "AS `pre` INNER JOIN `citations` AS `cit` ON `pre`.`PMID`=`cit`.`PMID` WHERE `PYEAR`=?";
        String insertSql = "INSERT INTO `umls_neighbor_by_predication` (`cui`, `year`, `neighbor`, `freq`) VALUES (?, ?, ?, ?)";
        PreparedStatement prepStatementUmls = connLit.prepareStatement(insertSql);
        for (int pubYear = processStartYear; pubYear <= processEndYear; pubYear++) {
            HashMap<String, Multiset<String>> neighborsMap = new HashMap<>();
            logger.info("Start query publication year: " + pubYear);
            int count = 0;
            String semDBName = DbConnector.SEMMED;
            JDBCHelper jdbcHelper = new JDBCHelper();
            List<Map<String, Object>> result = jdbcHelper.query(semDBName, querySql, pubYear);
            for (Map row : result) {
                String sCui = (String) row.get("s_cui");
                String oCui = (String) row.get("o_cui");
                Multiset<String> neighbor = neighborsMap.getOrDefault(sCui, HashMultiset.create());
                neighbor.add(oCui);
                neighborsMap.put(sCui, neighbor);
            }

            for (String cui : neighborsMap.keySet()) {
                for (Multiset.Entry<String> neighbor : neighborsMap.get(cui).entrySet()) {
                    prepStatementUmls.clearParameters();
                    prepStatementUmls.setString(1, cui);
                    prepStatementUmls.setInt(2, pubYear);
                    prepStatementUmls.setString(3, neighbor.getElement());
                    prepStatementUmls.setInt(4, neighbor.getCount());
                    prepStatementUmls.addBatch();
                    /*a is b's neighbor == b is a's neighbor
                    if a==b => only need inserting once,
                    else => insert a as b's neighbor
                     */
                    if (!cui.equals(neighbor.getElement())) {
                        prepStatementUmls.clearParameters();
                        prepStatementUmls.setString(1, neighbor.getElement());
                        prepStatementUmls.setInt(2, pubYear);
                        prepStatementUmls.setString(3, cui);
                        prepStatementUmls.setInt(4, neighbor.getCount());
                        prepStatementUmls.addBatch();
                    }
                }
                if (++count % 5000 == 0) {
                    prepStatementUmls.executeBatch();
                }
            }
        }
        prepStatementUmls.executeBatch();
        prepStatementUmls.close();
    }

    class UmlsConceptYear {
        private int freq;
        private LinkedHashSet<String> pmidSet;

        UmlsConceptYear() {
            freq = 0;
            pmidSet = new LinkedHashSet<>();
        }

        public void setFreq(int freq) {
            this.freq = freq;
        }

        public void setPmidSet(LinkedHashSet<String> pmidSet) {
            this.pmidSet = pmidSet;
        }

        public int getFreq() {
            return freq;
        }

        public LinkedHashSet<String> getPmidSet() {
            return pmidSet;
        }
    }

    class Tuple {
        private String cui1;
        private String cui2;

        Tuple(String cui1, String cui2) {
            this.cui1 = cui1;
            this.cui2 = cui2;
        }

        public void setCui1(String cui1) {
            this.cui1 = cui1;
        }

        public void setCui2(String cui2) {
            this.cui2 = cui2;
        }

        public String getCui1() {
            return cui1;
        }

        public String getCui2() {
            return cui2;
        }

        @Override
        public boolean equals(Object that) {
            if (that instanceof Tuple) {
                Tuple p = (Tuple) that;
                if ((this.cui1 == p.cui1) && (this.cui2 == p.cui2)) {
                    return true;
                } else if ((this.cui2 == p.cui1) && (this.cui1 == p.cui2)) {
                    return true;
                }
            }
            return false;
        }

//        @Override
//        public int hashCode() {
//            return this.cui1 + this.cui2;
//        }
    }
}
