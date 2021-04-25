package ken.node;

import ken.util.DbConnector;
import ken.util.JDBCHelper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DrugbankNode {
    private boolean isExist = false;
    private String meshName;
    private LinkedHashSet<String> targetUniprotIDs;
    private LinkedHashSet<String> targetMeshNames;

    public DrugbankNode(String drugMesh) {
        meshName = drugMesh;
        fetchProperties();
    }

    private void fetchProperties() {
        String sql = "SELECT TargetUniprotID FROM `drugbank_mesh_target` WHERE `Mesh` = ?";
        targetUniprotIDs = new LinkedHashSet<>();
        List<Map<String, Object>> result = JDBCHelper.query(DbConnector.BIORELATION, sql, meshName);
        for (Map row : result) {
            isExist = true;
            String rawTargetUPIDs = (String) row.get("TargetUniprotID");
            if (rawTargetUPIDs.length() > 0) {
                for (String pid : rawTargetUPIDs.split("\n"))
                    targetUniprotIDs.add(pid);
            }
        }


    }

    public boolean isExist() {
        return isExist;
    }

    public String getMeshName() {
        return meshName;
    }

    /**
     * Retrieve neighbor in terms of MESH
     */
    public Set<String> getNeighbors() {
        return getMeshNeighbors();
    }

    public Set<String> getMeshNeighbors() {
        if (targetUniprotIDs == null) fetchProperties();
        String sql = "SELECT `Mesh` FROM `uniprot_mesh` WHERE `UniProt` = ?";
        targetMeshNames = new LinkedHashSet<>();
        for (String targetUpid : targetUniprotIDs) {
            List<Map<String, Object>> result = JDBCHelper.query(DbConnector.BIORELATION, sql, targetUpid);
            for (Map row : result) {
                String targetMesh = (String) row.get("Mesh");
                targetMeshNames.add(targetMesh);
            }
        }
        return targetMeshNames;
    }

    public Set<String> getUniprotNeighbors() {
        return targetUniprotIDs;
    }
}
