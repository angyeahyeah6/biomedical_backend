package weng.prepare;

import weng.util.DbConnector;
import weng.util.JDBCHelper;
import weng.util.Utils;

import java.util.*;


public class MeshConcept {
    private HashMap<String, Integer> meshNameIdMap = null;
    private HashMap<Integer, String> meshIdNameMap = null;
    private String DRUG_VOCABULARY = "vocabulary/mesh_controlledVoc/drug.txt";
    private String DISEASE_VOCABULARY = "vocabulary/mesh_controlledVoc/disease.txt";
    private String GENE_VOCABULARY = "vocabulary/mesh_controlledVoc/genes_proteins_enzyme.txt";
    private String PATHOLOGY_VOCABULARY = "vocabulary/mesh_controlledVoc/pathology.txt";
    private String ANATOMY_VOCABULARY = "vocabulary/mesh_controlledVoc/anatomy.txt";

    public ArrayList<String> getIntermSeeds() {
        HashSet<String> intermsMesh = new HashSet<>();
        intermsMesh.addAll(getDiseaseSeeds());
        intermsMesh.addAll(getGeneProteinSeeds());
        intermsMesh.addAll(getPathologySeeds());
        intermsMesh.addAll(getAnatomySeeds());
        return (new ArrayList<>(intermsMesh));
    }

    public ArrayList<String> getSeed(String filepath) {
        return new Utils().readLineFile(filepath);
    }

    public ArrayList<String> getDrugSeeds() {
        return getSeed(DRUG_VOCABULARY);
    }

    public ArrayList<String> getDiseaseSeeds() {
        return getSeed(DISEASE_VOCABULARY);
    }

    public ArrayList<String> getGeneProteinSeeds() {
        return getSeed(GENE_VOCABULARY);
    }

    public ArrayList<String> getPathologySeeds() {
        return getSeed(PATHOLOGY_VOCABULARY);
    }

    public ArrayList<String> getAnatomySeeds() {
        return getSeed(ANATOMY_VOCABULARY);
    }

    public ArrayList<String> getSeedsNonRepeated() {
        ArrayList<String> allSeeds = new ArrayList<>();
        allSeeds.addAll(getDrugSeeds());
        allSeeds.addAll(getDiseaseSeeds());
        allSeeds.addAll(getGeneProteinSeeds());
        allSeeds.addAll(getAnatomySeeds());
        allSeeds.addAll(getPathologySeeds());
        return allSeeds;
    }

    /*
    * data like this:
    * Name|ID:
    * 1,2-Dihydroxybenzene-3,5-Disulfonic Acid Disodium Salt|2
    * 3,3'-Dichlorobenzidine|50
    * */
    public HashMap<String, Integer> getMeshNameIdMap() {
        if (meshNameIdMap == null) {
            setMeshMap();
        }
        return meshNameIdMap;
    }

    public HashMap<Integer, String> getMeshIdNameMap() {
        if (meshIdNameMap == null) {
            setMeshMap();
        }
        return meshIdNameMap;
    }

    public void setMeshMap() {
        meshNameIdMap = new HashMap<>();
        meshIdNameMap = new HashMap<>();
        String sql = "SELECT `ID`, `Name` FROM `mesh_seeds`";
        Object[] params = {};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result = jdbcHelper.query(DbConnector.LITERATURE_YEAR, sql, params);
        for (Map row : result) {
            meshNameIdMap.put((String) row.get("Name"), (int) row.get("ID"));
            meshIdNameMap.put((int) row.get("ID"), (String) row.get("Name"));
        }
    }
}
