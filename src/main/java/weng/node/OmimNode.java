package weng.node;

import weng.util.DbConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

public class OmimNode {
    private boolean isExist = false;
    private String umlsName;
    private LinkedHashSet<String> relatedMims;
    private LinkedHashSet<String> relatedUmls;

    public OmimNode(String diseaseUmls, Connection connBioRel) {
        umlsName = diseaseUmls;
        fetchProperties(connBioRel);
    }

    public String getUmlsName() {
        return umlsName;
    }

    private void fetchProperties(Connection connBioRel) {
        String queryMimUmlsRelation = "SELECT * FROM `omim_umls_relation` WHERE `umls` = ?";
        try (PreparedStatement psQuery = connBioRel.prepareStatement(queryMimUmlsRelation)) {
            psQuery.setString(1, getUmlsName());
            ResultSet rsQuery = psQuery.executeQuery();
            relatedMims = new LinkedHashSet<>();
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

    public Set<String> getUmlsNeighbors() {
        if (relatedUmls == null) {
            relatedUmls = new LinkedHashSet<>();
            String queryRelMimUmlsSql = "SELECT `umls` FROM `omim_umls` WHERE `mim`=?;";
            DbConnector dbConnector = new DbConnector();
            try (Connection connBioConcept = dbConnector.getBioConceptConnection();
                 PreparedStatement psQuery = connBioConcept.prepareStatement(queryRelMimUmlsSql)) {
                for (String relateMim : relatedMims) {
                    psQuery.clearParameters();
                    psQuery.setString(1, relateMim);
                    ResultSet queryUmlsRS = psQuery.executeQuery();
                    while (queryUmlsRS.next()) {
                        String relateUmls = queryUmlsRS.getString("umls");
                        relatedUmls.add(relateUmls);
                    }
                    queryUmlsRS.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return relatedUmls;
    }

    public Set<String> getMimNeighbors() {
        return relatedMims;
    }

    public boolean isExist() {
        return isExist;
    }
}