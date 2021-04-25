package ken.network;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import ken.node.LiteratureNode;
import ken.util.DbConnector;
import ken.util.JDBCHelper;
import ken.util.Utils;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.SparseInstance;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by lbj23k on 2017/6/2.
 */
public class PredicateInterm {
    private static ArrayList<Attribute> statAttributes;
    private static final int TOTALFREQ = 23532734;
    private static HashMap<String, Integer> contentWordMap;
    private static HashSet<String> nonNovelSet;

    private LiteratureNode pivotNode;
    private LiteratureNode intermNode;
    private LiteratureNode targetNode;
    private ArrayList<PredicateInfo> ABrelation;
    private ArrayList<PredicateInfo> BCrelation;
    private LinkedHashMultiset absmap;
    private LinkedHashMultiset bcsmap;
    private double entropyAB;
    private double entropyBC;
    private double pmiAB;
    private double pmiBC;
    private double pmidPredicateRatioAB;
    private double pmidPredicateRatioBC;
    private int yearAB;
    private int yearBC;
    private double jaccardSimilarityAB;
    private double jaccardSimilarityBC;
    private double NMDSimilarityAB = 0;
    private double NMDSimilarityBC = 0;
    private int ontolotyMentionAB;
    private int ontolotyMentionBC;
    private static List<String> predicateLs;
    public Map<String, Double> predicateCountMap;

    static {
        if (statAttributes == null) {
            statAttributes = createAttr();
        }
        if (contentWordMap == null) {
            setContentWordMap();
        }
        if (nonNovelSet == null) {
            setNonNovelSet();
        }
        predicateLs = Utils.readLineFile("vocabulary/trainingContentWord.txt");
    }

    public PredicateInterm(LiteratureNode pivotNode, LiteratureNode intermNode, LiteratureNode targetNode) {
        this.intermNode = intermNode;
        this.pivotNode = pivotNode;
        this.targetNode = targetNode;
        predicateCountMap = new HashMap<>();
        for (String predicate : predicateLs) predicateCountMap.put(predicate, 0.0);
    }

    public void setPredicateInfo(List<Map<String, Object>> abItem, List<Map<String, Object>> bcItem) {
        ABrelation = new ArrayList<>();
        BCrelation = new ArrayList<>();
        int large = (abItem.size() > bcItem.size()) ? abItem.size() : bcItem.size();
        for (int i = 0; i < large; i++) {
            if (i < abItem.size()) {
                int neighborId = (int) abItem.get(i).get("neighbor");
                if (neighborId == intermNode.getMeshId()) {
                    String predicate = (String) abItem.get(i).get("predicate");
                    String pmid_AB = (String) abItem.get(i).get("pmid");
                    int year_AB = (int) abItem.get(i).get("year");
                    if (predicateCountMap.containsKey(predicate)) {
                        predicateCountMap.put(predicate, predicateCountMap.get(predicate) + 1);
                    }
                    ABrelation.add(new PredicateInfo(predicate, pmid_AB, year_AB));
                }
            }
            if (i < bcItem.size()) {
                int neighborId = (int) bcItem.get(i).get("neighbor");
                if (neighborId == intermNode.getMeshId()) {
                    String predicate = (String) bcItem.get(i).get("predicate");
                    String pmid_BC = (String) bcItem.get(i).get("pmid");
                    int year_BC = (int) bcItem.get(i).get("year");
                    if (predicateCountMap.containsKey(predicate)) {
                        predicateCountMap.put(predicate, predicateCountMap.get(predicate) + 1);
                    }
                    BCrelation.add(new PredicateInfo(predicate, pmid_BC, year_BC));
                }
            }
        }
        int j = 0;
        absmap = LinkedHashMultiset.create(ABrelation);
        bcsmap = LinkedHashMultiset.create(BCrelation);
        initAttr();
    }

    private void initAttr() {
        setEntropy();
        setPMI();
        setPmidPredicateRatio();
        setYear();
        setJaccardSimilarity();
        setNMS();
        setOntology();
    }

    private void setEntropy() {
        Iterator<Multiset.Entry<PredicateInfo>> it1 = absmap.entrySet().iterator();
        Iterator<Multiset.Entry<PredicateInfo>> it2 = bcsmap.entrySet().iterator();
        entropyAB = 0.0;
        entropyBC = 0.0;
        while (it1.hasNext() || it2.hasNext()) {
            if (it1.hasNext()) {
                Multiset.Entry<PredicateInfo> itemAB = it1.next();
                double probAB = (double) itemAB.getCount() / absmap.size();
                entropyAB += Math.log(probAB) * probAB;
            }
            if (it2.hasNext()) {
                Multiset.Entry<PredicateInfo> itemBC = it2.next();
                double probBC = (double) itemBC.getCount() / bcsmap.size();
                entropyBC += Math.log(probBC) * probBC;
            }
        }
        //normalized
        entropyAB /= absmap.elementSet().size();
        entropyBC /= bcsmap.elementSet().size();
        entropyAB = Math.abs(entropyAB);
        entropyBC = Math.abs(entropyBC);
    }

    private void setPmidPredicateRatio() {
        Set<String> pmidABset = new HashSet<>();
        Set<String> pmidBCset = new HashSet<>();
        int large = (ABrelation.size() > BCrelation.size()) ? ABrelation.size() : BCrelation.size();
        for (int i = 0; i < large; i++) {
            if (i < ABrelation.size()) {
                pmidABset.add(ABrelation.get(i).getPmid());
            }
            if (i < BCrelation.size()) {
                pmidBCset.add(BCrelation.get(i).getPmid());
            }
        }
        /*
         * bad idea to construct such sql, but no idea how to fix...
         */
        StringBuilder sb_AB = new StringBuilder();
        StringBuilder sb_BC = new StringBuilder();

        Object[] paramsAB = pmidABset.toArray();
        Object[] paramsBC = pmidBCset.toArray();
        large = (paramsAB.length > paramsBC.length) ? paramsAB.length : paramsBC.length;
        for (int i = 0; i < large; i++) {
            if (i < paramsAB.length) {
                sb_AB.append("?,");
            }
            if (i < paramsBC.length) {
                sb_BC.append("?,");
            }
        }
        String sql_AB = "SELECT count(*) as count FROM mesh_predication_aggregate WHERE pmid in ("
                + sb_AB.deleteCharAt(sb_AB.length() - 1) + ")";
        String sql_BC = "SELECT count(*) as count FROM mesh_predication_aggregate WHERE pmid in ("
                + sb_BC.deleteCharAt(sb_BC.length() - 1) + ")";
        Long countAB =
                (Long) JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql_AB, paramsAB).get(0).get("count");
        Long countBC =
                (Long) JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql_BC, paramsBC).get(0).get("count");
        pmidPredicateRatioAB = (double) ABrelation.size() / (countAB.intValue() / 2);
        pmidPredicateRatioBC = (double) BCrelation.size() / (countBC.intValue() / 2);
//        String sql_AB = "SELECT sum(count) as totalC FROM pmid_count " +
//                "WHERE pmid in (" + sb_AB.deleteCharAt(sb_AB.length() - 1) + ")";
//        String sql_BC = "SELECT sum(count) as totalC FROM pmid_count " +
//                "WHERE pmid in (" + sb_BC.deleteCharAt(sb_BC.length() - 1) + ")";
//        BigDecimal countAB =
//                (BigDecimal) JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql_AB, paramsAB).get(0).get("totalC");
//        BigDecimal countBC =
//                (BigDecimal) JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql_BC, paramsBC).get(0).get("totalC");
//        pmidPredicateRatioAB = (double) ABrelation.size() / countAB.intValue();
//        pmidPredicateRatioBC = (double) BCrelation.size() / countBC.intValue();
    }

    private void setPMI() {
        double pmi = (double) ABrelation.size() / (pivotNode.getFrequency() * intermNode.getFrequency());
        pmiAB = Math.log(pmi * TOTALFREQ);
        pmi = (double) BCrelation.size() / (intermNode.getFrequency() * targetNode.getFrequency());
        pmiBC = Math.log(pmi * TOTALFREQ);
    }

    private void setYear() {
        yearAB = pivotNode.getEndYear() + 1 - Collections.max(ABrelation).getYear();
        yearBC = pivotNode.getEndYear() + 1 - Collections.max(BCrelation).getYear();
    }

    private void setJaccardSimilarity() {
        jaccardSimilarityAB = Feature.jaccardSimilarity(pivotNode, intermNode);
        jaccardSimilarityBC = Feature.jaccardSimilarity(intermNode, targetNode);
    }

    private void setNMS() {
        NMDSimilarityAB = Feature.normalizedMedlineSimilarity(pivotNode, intermNode);
        NMDSimilarityBC = Feature.normalizedMedlineSimilarity(intermNode, targetNode);
    }

    private void setOntology() {
        ontolotyMentionAB = Feature.ontologyMention(pivotNode, intermNode);
        ontolotyMentionBC = Feature.ontologyMention(intermNode, targetNode);
    }


    public Instance getStatInst() {
        DenseInstance instance = new DenseInstance(statAttributes.size());
        instance.setValue(0, ABrelation.size());
        instance.setValue(1, BCrelation.size());
        instance.setValue(2, pmidPredicateRatioAB);
        instance.setValue(3, pmidPredicateRatioBC);
        instance.setValue(4, pmiAB);
        instance.setValue(5, pmiBC);
        instance.setValue(6, entropyAB);
        instance.setValue(7, entropyBC);
        instance.setValue(8, jaccardSimilarityAB);
        instance.setValue(9, jaccardSimilarityBC);
        instance.setValue(10, NMDSimilarityAB);
        instance.setValue(11, NMDSimilarityBC);
        instance.setValue(12, ontolotyMentionAB);
        instance.setValue(13, ontolotyMentionBC);
        instance.setValue(14, intermNode.getFrequency());
        instance.setValue(15, intermNode.getPredicateNeighbors().size());
        instance.setValue(16, (nonNovelSet.contains(intermNode.getName())) ? 0 : 1);
        instance.setValue(17, yearAB);
        instance.setValue(18, yearBC);
        return instance;
    }

    public Instance getContentInst() {
        Instance instance = new SparseInstance(contentWordMap.size());
        for(int i =0;i<instance.numAttributes();i++){
            instance.setValue(i,0);
        }
        Multiset<PredicateInfo> totalSet = LinkedHashMultiset.create(absmap);
        totalSet.addAll(bcsmap);
        int i = totalSet.elementSet().size();
        for(Multiset.Entry<PredicateInfo> item: totalSet.entrySet()){
            String predicate = item.getElement().getPredicate();
            if (contentWordMap.containsKey(predicate)) {
                instance.setValue(contentWordMap.get(predicate), item.getCount());
            }
        }
//        int j =bcsmap.elementSet().size();
//        Iterator<Multiset.Entry<PredicateInfo>> it1 = absmap.entrySet().iterator();
//        Iterator<Multiset.Entry<PredicateInfo>> it2 = bcsmap.entrySet().iterator();
//        while (it1.hasNext() || it2.hasNext()) {
//            if (it1.hasNext()) {
//                Multiset.Entry<PredicateInfo> itemAB = it1.next();
//                String word = itemAB.getElement().getPredicate();
//                if (contentWordMap.containsKey(word)) {
//                    instance.setValueSparse(contentWordMap.get(word), itemAB.getCount());
//                }
//            }
//            if (it2.hasNext()) {
//                Multiset.Entry<PredicateInfo> itemBC = it2.next();
//                String word = itemBC.getElement().getPredicate();
//                if (contentWordMap.containsKey(word)) {
//                    instance.setValueSparse(contentWordMap.get(word), itemBC.getCount());
//                }
//            }
//        }
//        instance.setValue(instance.numAttributes() - 1, intermNode.getMeshId());
        return instance;
    }

    private String getrelationStr(ArrayList<PredicateInfo> relations, boolean isAB) {
        StringBuilder sb = new StringBuilder();
        for (PredicateInfo item : relations) {
            sb.append(item.getContent(isAB)).append(" ");
        }
        return sb.toString();
    }

    public ArrayList<PredicateInfo> getABrelation() {
        return ABrelation;
    }

    public ArrayList<PredicateInfo> getBCrelation() {
        return BCrelation;
    }

    private static ArrayList<Attribute> createAttr() {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("count(AB)"));
        attributes.add(new Attribute("count(BC)"));
        attributes.add(new Attribute("normalized count(AB)"));
        attributes.add(new Attribute("normalized count(BC)"));
        attributes.add(new Attribute("PMI(AB)"));
        attributes.add(new Attribute("PMI(BC)"));
        attributes.add(new Attribute("entropy(AB)"));
        attributes.add(new Attribute("entropy(BC)"));
        attributes.add(new Attribute("jaccard similarity(AB)"));
        attributes.add(new Attribute("jaccard similarity(BC)"));
        attributes.add(new Attribute("NMS similarity(AB)"));
        attributes.add(new Attribute("NMS similarity(BC)"));
        attributes.add(new Attribute("ontology mentioned(AB)"));
        attributes.add(new Attribute("ontology mentioned(BC)"));
        attributes.add(new Attribute("frequency(B)"));
        attributes.add(new Attribute("neighbor count(B)"));
        attributes.add(new Attribute("novel(B)"));
        attributes.add(new Attribute("time to indication(AB)"));
        attributes.add(new Attribute("time to indication(BC)"));
        ArrayList<String> attrVals = new ArrayList<>(2);
        attrVals.add("important");
        attrVals.add("unimportant");
        attributes.add(new Attribute("label", attrVals));
//        attributes.add(new Attribute("intermId"));
        return attributes;
    }

    private static void setContentWordMap() {
        contentWordMap = new HashMap<>();
        List<String> wordLs = Utils.readLineFile("vocabulary/trainingContentWord.txt");
        for (int i = 0; i < wordLs.size(); i++) {
            contentWordMap.put(wordLs.get(i), i);
        }
    }

    private static void setNonNovelSet() {
        nonNovelSet = new HashSet<>();
        String sql = "SELECT Mesh FROM biomedical_concept_mapping.umls_mesh where CUI in " +
                "(SELECT c.cui FROM semmed_ver26.concept_semtype cs " +
                "join semmed_ver26.concept c on c.concept_id=cs.concept_id where cs.novel='N')";
        List<Map<String, Object>> ls = JDBCHelper.query(DbConnector.BIOCONCEPT, sql);
        for (Map<String, Object> row : ls) {
            nonNovelSet.add((String) row.get("mesh"));
        }
    }

    public String getName() {
        return intermNode.getName();
    }

    public static ArrayList<Attribute> getAttributes() {
        return statAttributes;
    }
}
