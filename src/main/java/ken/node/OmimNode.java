package ken.node;

import ken.util.DbConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

public class OmimNode {
    private boolean isExist = false;
    private String meshName;
    private LinkedHashSet<String> relatedMims;
    private LinkedHashSet<String> relatedMesh;

    public OmimNode(String diseaseMesh) {
        meshName = diseaseMesh;
        fetchProperties();
    }

    private void fetchProperties() {
        String queryMimMeshRelation = "SELECT * FROM `omim_mesh_relation` WHERE `Mesh` = ?";
        try (Connection connBioRelation = DbConnector.getBioRelationConnection();
             PreparedStatement psQuery = connBioRelation.prepareStatement(queryMimMeshRelation)) {
            psQuery.setString(1, meshName);
            ResultSet rsQuery = psQuery.executeQuery();
            relatedMims = new LinkedHashSet<String>();
            while (rsQuery.next()) {
                isExist = true;
                String rawMims = rsQuery.getString("Related_MIM");
                for (String relateMim : rawMims.split("\n")) {
                    relateMim = relateMim.trim();
                    relatedMims.add(relateMim);
                }
            }
            rsQuery.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Set<String> getNeighbors() {
        return getMeshNeighbors();
    }

    public Set<String> getMeshNeighbors() {
        if (relatedMesh == null) {
            relatedMesh = new LinkedHashSet<>();
            String queryRelMimMeshSql = "SELECT `Mesh` FROM `omim_mesh` WHERE `MIM` = ?";
            try (Connection connBioConcept = DbConnector.getBioConceptConnection();
                 PreparedStatement psQuery = connBioConcept.prepareStatement(queryRelMimMeshSql)) {
                for (String relateMim : relatedMims) {
                    psQuery.clearParameters();
                    psQuery.setString(1, relateMim);
                    ResultSet queryMeshRS = psQuery.executeQuery();
                    while (queryMeshRS.next()) {
                        String relateMesh = queryMeshRS.getString("Mesh");
                        relatedMesh.add(relateMesh);
                    }
                    queryMeshRS.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return relatedMesh;
    }

    public Set<String> getMimNeighbors() {
        return relatedMims;
    }

    public boolean isExist() {
        return isExist;
    }
}
