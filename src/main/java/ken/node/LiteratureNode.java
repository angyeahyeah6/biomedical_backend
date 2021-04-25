package ken.node;


import com.google.common.base.Objects;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import ken.util.DbConnector;
import ken.util.JDBCHelper;
import ken.prepare.MeshConceptObject;
import ken.util.Utils;

import java.math.BigDecimal;
import java.util.*;

public class LiteratureNode {
    private final String meshName;
    private final int scopeStartYear;
    private final int scopeEndYear;
    private final int meshId;
    private HashMap<String, Integer> cooccurNeighbors = null;
    private HashMap<String, Multiset<String>> predicateNeighbors = null;
    private HashMap<String, Integer> neighbors = null;
    private HashSet<String> documents = null;
    private int df = (-1);
    private int freq = (-1);
    private static HashMap<String, LiteratureNode> allNodes;
    private int predicateNeighborCount = (-1);
    private int predicateCooccurCount = (-1);
    private static Set<String> predicateMap;

    static {
        predicateMap = new HashSet<>(Utils.readLineFile("vocabulary/contentWord.txt"));
    }

    public LiteratureNode(String name, int startYear, int endYear) {
        meshName = name;
        scopeStartYear = startYear;
        scopeEndYear = endYear;
        meshId = MeshConceptObject.getMeshNameIdMap().get(meshName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        LiteratureNode node0 = (LiteratureNode) obj;
        return ((this.meshName.equals(node0.meshName)) && (this.scopeStartYear == node0.scopeStartYear)
                && (this.scopeEndYear == node0.scopeEndYear));
    }

    @Override
    public int hashCode() {
        return (Objects.hashCode(meshName, scopeStartYear, scopeEndYear));
    }

    public String getName() {
        return meshName;
    }

    public int df() {
        if (df == -1) {
            String sql = "SELECT df FROM `mesh_concept_by_year` WHERE mesh_id = ? AND (`year` BETWEEN ? AND ?)";
            Integer[] params = {meshId, scopeStartYear, scopeEndYear};
            List<Map<String, Object>> result = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql, params);
            int freq = 0;
            for (Map row : result) {
                int df = (int) row.get("df");
                freq += df;
            }
            df = freq;
        }
        return df;
    }

    public HashSet<String> getDocuments() {
        if (documents == null) setDocuments();
        return documents;
    }

    /**
     * Get node's neighbors
     *
     * @return (neighborName, frequency)
     * MeshID|Year|Neighbor|Frequency
     * 5247|1809|15830|5
     */
    public HashMap<String, Multiset<String>> getPredicateNeighbors() {
        if (predicateNeighbors == null) {
            predicateNeighbors = new LinkedHashMap<>();
            String sql = "SELECT s_mesh_id, o_mesh_id,predicate FROM mesh_predication_aggregate WHERE s_mesh_id = ? AND (year BETWEEN ? AND ?)";

            Integer[] params = {meshId, scopeStartYear, scopeEndYear};

            List<Map<String, Object>> result = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql, params);
            for (Map row : result) {
                int neighborId = (int) row.get("o_mesh_id");
                String neighborName = MeshConceptObject.getMeshIdNameMap().get(neighborId);
                String predicate = (String) row.get("predicate");
//                if (!predicateMap.contains(predicate)) continue;
                Multiset<String> neighborInfo = predicateNeighbors.getOrDefault(neighborName, HashMultiset.create());
                neighborInfo.add(predicate);
                predicateNeighbors.put(neighborName, neighborInfo);
            }
        }
        return predicateNeighbors;

    }

    public HashMap<String, Integer> getNeighbors() {
        if (neighbors == null) {
            neighbors = new HashMap<>();
            String sql = "SELECT neighbor,freq FROM neighbor_by_predication WHERE mesh_id = ? AND (year BETWEEN ? AND ?)";
            Integer[] params = {meshId, scopeStartYear, scopeEndYear};
            List<Map<String, Object>> result = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql, params);
            for (Map row : result) {
                int neighborId = (int) row.get("neighbor");
                String neighborName = MeshConceptObject.getMeshIdNameMap().get(neighborId);
                int frequency = neighbors.getOrDefault(neighborName, 0);
                neighbors.put(neighborName, frequency + (int) row.get("freq"));
//                neighbors.put(neighborName, 1);
            }
        }
        return neighbors;
    }

    public HashMap<String, Integer> getCooccurNeighbors() {
        if (cooccurNeighbors == null) {
            cooccurNeighbors = new LinkedHashMap<>();
            String sql = "SELECT neighbor,freq FROM neighbor_cooccur WHERE mesh_id = ? AND (year BETWEEN ? AND ?)";
            Integer[] params = {meshId, scopeStartYear, scopeEndYear};
            List<Map<String, Object>> result = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql, params);
            for (Map row : result) {
                int neighborId = (int) row.get("neighbor");
                String neighborName = MeshConceptObject.getMeshIdNameMap().get(neighborId);
                int frequency = cooccurNeighbors.getOrDefault(neighborName, 0);
                cooccurNeighbors.put(neighborName, frequency + (int) row.get("freq"));
            }
        }
        return cooccurNeighbors;
    }

    @Override
    public String toString() {
        return "Node: " + this.meshName;
    }

    /*
    * NAME|YEAR|PMID|DF
    * 1,2-Dimethylhydrazine|1976|1016720,136114|2
    * */
    private void setDocuments() {
        documents = new HashSet<>();
        String sql = "SELECT pmid,freq FROM `mesh_concept_by_year` WHERE mesh_id = ? AND (year BETWEEN ? AND ?)";
        Integer[] params = {meshId, scopeStartYear, scopeEndYear};
        List<Map<String, Object>> result = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql, params);
        df = 0;
        freq = 0;
        for (Map row : result) {
            String[] pmids = ((String) row.get("pmid")).split(",");
            df += pmids.length;
            documents.addAll(Arrays.asList(pmids));
            freq += (int) row.get("freq");
        }
    }

    public int getFrequency() {
        if (freq == -1) {
            String sql = "SELECT sum(freq) as sf FROM `mesh_concept_by_year` WHERE mesh_id = ? AND (year BETWEEN ? AND ?)";
            Integer[] params = {meshId, scopeStartYear, scopeEndYear};
            List<Map<String, Object>> result = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql, params);
            freq = ((BigDecimal) result.get(0).get("sf")).intValue();
        }
        return freq;
    }

    public int getMeshId() {
        return this.meshId;
    }

    public int getEndYear() {
        return this.scopeEndYear;
    }

    public static HashMap<String, LiteratureNode> getAllNodes(int startYear, int endYear) {
        if (allNodes == null) {
            allNodes = new HashMap<>();
            for (String seed : MeshConceptObject.getSeedsNonRepeated()) {
                allNodes.put(seed, new LiteratureNode(seed, startYear, endYear));
            }
        }
        return allNodes;
    }

    public int getNeighborPredicateCount() {
        if (predicateNeighborCount == (-1)) {
            predicateNeighborCount = 0;
            for (Multiset<String> item : predicateNeighbors.values()) {
                predicateNeighborCount += item.size();
            }
        }
        return predicateNeighborCount;
    }

    public int getNeighborCooccurCount() {
        if (predicateCooccurCount == (-1)) {
            predicateCooccurCount = 0;
            for (Integer count : cooccurNeighbors.values()) {
                predicateCooccurCount += count;
            }
        }
        return predicateCooccurCount;
    }
}
