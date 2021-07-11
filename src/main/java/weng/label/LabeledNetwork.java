package weng.label;

import weng.node.LiteratureNode;
import weng.util.DbConnector;
import weng.util.JDBCHelper;
import weng.util.file.Stopwords;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by lbj23k on 2017/4/12.
 */
public class LabeledNetwork {
    private ArrayList<LabeledInterm> intermediates;
    private LiteratureNode pivotNode;
    private LiteratureNode targetNode;
    private final int link_id;
    private final static int startYear = 1809;
    private int endYear;
    private final double MIN_CONF = 0.0001;
    private int totalFreq;


    public LiteratureNode getPivotNode() {
        return pivotNode;
    }

    public LiteratureNode getTargetNode() {
        return targetNode;
    }

    public LabeledNetwork(String pivotName, String targetName, int endYear, int link_id,
                          Connection connSem, Connection connLit, Connection connLSUMLS, Connection connBioRel,
                          HashMap<String, HashMap<String, Integer>> semTypeMatrixByIntermediate) throws SQLException, IOException {
        this.endYear = endYear;
        this.link_id = link_id;
        this.pivotNode = new LiteratureNode(pivotName, startYear, endYear, connSem, connLit);
        this.targetNode = new LiteratureNode(targetName, startYear, endYear, connSem, connLit);
        this.totalFreq = setTotalFreq(connLit);
        this.intermediates = setIntermediates(connSem, connLSUMLS, connLit, connBioRel, semTypeMatrixByIntermediate);
    }

    private ArrayList<LabeledInterm> setIntermediates(Connection connSem, Connection connLSUMLS, Connection connLit, Connection connBioRel,
                                                      HashMap<String, HashMap<String, Integer>> semTypeMatrixByIntermediate) throws SQLException, IOException {
        ArrayList<LabeledInterm> intermediates = new ArrayList<>();
        String sql = "SELECT id,b_name, importance from intermediate where link_id=? order by importance desc";
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> ls = jdbcHelper.query(connLSUMLS, sql, link_id);
        for (Map<String, Object> row : ls) {
            String intermName = (String) row.get("b_name");
            int importance = (int) row.get("importance");
            int intermId = (int) row.get("id");
            LiteratureNode intermNode = new LiteratureNode(intermName, startYear, endYear, connSem, connLit);
            LabeledInterm labeledInterm = new LabeledInterm(
                    pivotNode, intermNode, targetNode, intermId, importance, totalFreq,
                    connLSUMLS, connSem, connLit,
                    semTypeMatrixByIntermediate);
            boolean ifABCRelationFiltered = labeledInterm.getIfABCRelationFiltered();
            if (!ifABCRelationFiltered) {
                intermediates.add(labeledInterm);
            }
        }
        return intermediates;
    }

    public int getFirstYear() {
        return this.endYear + 1;
    }

    public int getEndYear() {
        return endYear;
    }

    private int setTotalFreq(Connection connLit) throws SQLException {
        String sql = "SELECT COUNT(*) FROM `umls_predication_aggregate_filtered` where `is_exist`=1 and `year` between ? and ?;"; // Total predications' frequency
//        String sql = "SELECT sum(freq) as sf FROM umls_concept_by_year where year between ? and ?"; // Total concepts' frequency
        PreparedStatement ps = connLit.prepareStatement(sql);
        ps.clearParameters();
        ps.setInt(1, startYear);
        ps.setInt(2, getEndYear());
        ResultSet rs = ps.executeQuery();
        int totalFreq = 0;
        if (rs.next()) {
            totalFreq = rs.getInt("sf");
        }
        return totalFreq;
    }

    public ArrayList<LabeledInterm> getIntermediate() {
        return intermediates;
    }

}
