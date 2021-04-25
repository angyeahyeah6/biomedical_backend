package ken.prepare;

import ken.util.DbConnector;
import ken.util.JDBCHelper;
import ken.util.Utils;

import java.util.*;


public class MeshConceptObject {
    private static HashMap<String, Integer> meshNameIdMap = null;
    private static HashMap<Integer, String> meshIdNameMap = null;
    private static final String DRUG_VOCABULARY = "vocabulary/drug.txt";
    private static final String DISEASE_VOCABULARY = "vocabulary/disease.txt";
    private static final String GENE_VOCABULARY = "vocabulary/genes_proteins_enzyme.txt";
    private static final String PATHOLOGY_VOCABULARY = "vocabulary/pathology.txt";
    private static final String ANATOMY_VOCABULARY = "vocabulary/anatomy.txt";

    public static ArrayList<String> getIntermSeeds() {
        HashSet<String> intermsMesh = new HashSet<>();
        intermsMesh.addAll(getDiseaseSeeds());
        intermsMesh.addAll(getGeneProteinSeeds());
        intermsMesh.addAll(getPathologySeeds());
        intermsMesh.addAll(getAnatomySeeds());
        return (new ArrayList<>(intermsMesh));
    }

    private static ArrayList<String> getSeed(String filepath) {
        return Utils.readLineFile(filepath);
    }

    public static ArrayList<String> getDrugSeeds() {
        return getSeed(DRUG_VOCABULARY);
    }

    public static ArrayList<String> getDiseaseSeeds() {
        return getSeed(DISEASE_VOCABULARY);
    }

    public static ArrayList<String> getGeneProteinSeeds() {
        return getSeed(GENE_VOCABULARY);
    }

    public static ArrayList<String> getPathologySeeds() {
        return getSeed(PATHOLOGY_VOCABULARY);
    }

    public static ArrayList<String> getAnatomySeeds() {
        return getSeed(ANATOMY_VOCABULARY);
    }

    public static ArrayList<String> getSeedsNonRepeated() {
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
    public static HashMap<String, Integer> getMeshNameIdMap() {
        if (meshNameIdMap == null) {
            setMeshMap();
        }
        return meshNameIdMap;
    }

    public static HashMap<Integer, String> getMeshIdNameMap() {
        if (meshIdNameMap == null) {
            setMeshMap();
        }
        return meshIdNameMap;
    }

    private static void setMeshMap() {
        meshNameIdMap = new HashMap<>();
        meshIdNameMap = new HashMap<>();
        String sql = "SELECT `ID`, `Name` FROM `mesh_seeds`";
        Object[] params = {};
        List<Map<String, Object>> result = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql, params);
        for (Map row : result) {
            meshNameIdMap.put((String) row.get("Name"), (int) row.get("ID"));
            meshIdNameMap.put((int) row.get("ID"), (String) row.get("Name"));
        }
    }
}
