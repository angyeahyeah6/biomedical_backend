package weng.util.calculator;

import weng.util.JDBCHelper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

public class WeightedPredicationCalc {
    public static double count(Connection connLit,
                               List<String> pmids) {
        HashMap<String, Integer> relationFreq = relationFreq(pmids);
        HashMap<String, Integer> totalFreq = totalFreq(pmids, connLit);
        return freq(relationFreq) / totalPredicationFreq(relationFreq, totalFreq);
    }

    public static int totalPredicationFreq(HashMap<String, Integer> relationFreqByPmid,
                                           HashMap<String, Integer> totalFreqByPmid) {
        int totalPredicationFreq = 0;
        for (String pmid : relationFreqByPmid.keySet()) {
            int relationFreq = relationFreqByPmid.get(pmid);
            int totalFreq = totalFreqByPmid.get(pmid);
            totalPredicationFreq += totalFreq * relationFreq;
        }
        return totalPredicationFreq;
    }

    public static int freq(HashMap<String, Integer> relationFreqByPmid) {
        int predicationFreq = 0;
        for (String pmid : relationFreqByPmid.keySet()) {
            int relationFreq = relationFreqByPmid.get(pmid);
            predicationFreq += relationFreq;
        }
        return predicationFreq;
    }

    public static HashMap<String, Integer> totalFreq(List<String> pmids,
                                                     Connection connLit) {
        HashMap<String, Integer> totalFreqByPmid = new HashMap<>();
        for (String pmid : pmids) {
            Object[] params = {pmid};
            String sql = "SELECT sum(count) as totalC FROM pmid_count_umls WHERE pmid=?";
            JDBCHelper jdbcHelper = new JDBCHelper();
            BigDecimal count = (BigDecimal) jdbcHelper.query(connLit, sql, params).get(0).get("totalC");
            totalFreqByPmid.put(pmid, count.intValue());
        }
        return totalFreqByPmid;
    }

    public static HashMap<String, Integer> relationFreq(List<String> pmids) {
        HashMap<String, Integer> relationFreqByPmid = new HashMap<>();
        for (String pmid : pmids) {
            int relationFreq = 0;
            if (relationFreqByPmid.containsKey(pmid)) {
                relationFreq = relationFreqByPmid.get(pmid);
            }
            relationFreq += 1;
            relationFreqByPmid.put(pmid, relationFreq);
        }
        return relationFreqByPmid;
    }
}
