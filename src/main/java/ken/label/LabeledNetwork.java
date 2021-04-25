package ken.label;

import ken.node.LiteratureNode;
import ken.util.DbConnector;
import ken.util.JDBCHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by lbj23k on 2017/4/12.
 */
public class LabeledNetwork {
    private ArrayList<LabeledInterm> intermediates;
    private LiteratureNode pivotNode;
    private LiteratureNode targetNode;
    private final int link_id;
    private int endYear;
    private final double MIN_CONF = 0.0001;
    private int totalFreq = (-1);


    public LiteratureNode getPivotNode() {
        return pivotNode;
    }

    public LiteratureNode getTargetNode() {
        return targetNode;
    }

    public LabeledNetwork(String pivotName, String targetName, int endYear, int link_id,
                          boolean important) {
        int startYear = 1809;
        this.endYear = endYear;
        this.link_id = link_id;
        this.pivotNode = new LiteratureNode(pivotName, startYear, endYear);
        this.targetNode = new LiteratureNode(targetName, startYear, endYear);
        setTotalFreq();
        setIntermediates();
    }

    private void setIntermediates() {
        intermediates = new ArrayList<>();
        String sql = "SELECT id,b_name, importance from intermediate where link_id=? order by importance desc";
        List<Map<String, Object>> ls = JDBCHelper.query(DbConnector.LABELSYS, sql, link_id);
        for (Map<String, Object> row : ls) {
            String intermName = (String) row.get("b_name");
            int importance = (int) row.get("importance");
            int intermId = (int) row.get("id");
            LiteratureNode intermNode = new LiteratureNode(intermName, 1809, endYear);
            intermediates.add(new LabeledInterm(pivotNode, intermNode, targetNode, intermId, importance, totalFreq));
        }
    }

    public HashSet<String> getNonNovelName() {
        String sql = "SELECT Mesh FROM biomedical_concept_mapping.umls_mesh where CUI in " +
                "(SELECT c.cui FROM semmed_ver26.concept_semtype cs " +
                "join semmed_ver26.concept c on c.concept_id=cs.concept_id where cs.novel='N')";
        List<Map<String, Object>> ls = JDBCHelper.query(DbConnector.BIOCONCEPT, sql);
        HashSet<String> hs = new HashSet<>();
        for (Map<String, Object> row : ls) {
            hs.add((String) row.get("mesh"));
        }
        return hs;
    }

    public int getFirstYear() {
        return this.endYear + 1;
    }

    private void setTotalFreq() {
        if (totalFreq == (-1)) {
            String sql = "SELECT sum(freq) as sf FROM mesh_concept_by_year where year between ? and ?";
            Object[] param = {1809, endYear};
            List<Map<String, Object>> ls = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql, param);
            totalFreq = ((BigDecimal) ls.get(0).get("sf")).intValue();
        }
    }

    public ArrayList<LabeledInterm> getIntermediate() {
        return intermediates;
    }

}
