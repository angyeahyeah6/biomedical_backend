package weng.node;

import weng.util.DbConnector;
import weng.util.JDBCHelper;

import java.sql.Connection;
import java.util.*;

public class DrugbankNode {
    private boolean isExist = false;
    private String umlsName;
    private LinkedHashSet<String> targetUniprotIDs;

    public DrugbankNode(String drugUmls, Connection connBioRel) {
        umlsName = drugUmls;
        fetchProperties(connBioRel);
    }

    private void fetchProperties(Connection connBioRel) {
        String sql = "SELECT `TargetUniprotID` FROM `drugbank_umls_target` WHERE `umls`=?";
        targetUniprotIDs = new LinkedHashSet<>();
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result = jdbcHelper.query(connBioRel, sql, getUmlsName());
        for (Map row : result) {
            isExist = true;
            String rawTargetUPIDs = (String) row.get("TargetUniprotID");
            if (rawTargetUPIDs.length() > 0) {
                targetUniprotIDs.addAll(Arrays.asList(rawTargetUPIDs.split("\n")));
            }
        }
    }

    public boolean isExist() {
        return isExist;
    }

    public String getUmlsName() {
        return umlsName;
    }

    /**
     * Retrieve neighbor in terms of UMLS
     */

    public Set<String> getUmlsNeighbors(Connection connBioRel) {
        if (targetUniprotIDs == null) fetchProperties(connBioRel);
        String sql = "SELECT `umls` FROM `uniprot_umls` WHERE `UniProt` = ?";
        LinkedHashSet<String> targetUmlsNames = new LinkedHashSet<>();
        for (String targetUpid : targetUniprotIDs) {
            JDBCHelper jdbcHelper = new JDBCHelper();
            List<Map<String, Object>> result = jdbcHelper.query(connBioRel, sql, targetUpid);
            for (Map row : result) {
                String targetUmls = (String) row.get("umls");
                targetUmlsNames.add(targetUmls);
            }
        }
        return targetUmlsNames;
    }

    public Set<String> getUniprotNeighbors() {
        return targetUniprotIDs;
    }
}