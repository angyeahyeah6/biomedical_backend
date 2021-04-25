package ken.label;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import ken.network.Feature;
import ken.node.LiteratureNode;
import ken.util.DbConnector;
import ken.util.JDBCHelper;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by lbj23k on 2017/4/13.
 */
public class LabeledInterm {
    private static HashSet<String> nonNovelSet;
    private static ArrayList<Attribute> statAttributes;

    private int totalFreq;
    private LiteratureNode pivotNode;
    private LiteratureNode intermNode;
    private LiteratureNode targetNode;
    private ArrayList<LabeledInfo> ABrelation;
    private ArrayList<LabeledInfo> BCrelation;
    private final int importance;
    private final int intermId;
    private double entropyAB = 0;
    private double entropyBC = 0;
    private double pmiAB = 0;
    private double pmiBC = 0;
    private double pmidPredicateRatioAB = 0;
    private double pmidPredicateRatioBC = 0;
    private int yearAB = 0;
    private int yearBC = 0;
    private double jaccardSimilarityAB = 0;
    private double jaccardSimilarityBC = 0;
    private double NMDSimilarityAB = 0;
    private double NMDSimilarityBC = 0;
    private int ontolotyMentionAB = 0;
    private int ontolotyMentionBC = 0;

    static {
        statAttributes = getStatAttributes();
        setNonNovelSet();
    }

    public LabeledInterm(LiteratureNode pivotNode, LiteratureNode intermNode, LiteratureNode targetNode
            , int intermId, int importance, int totalFreq) {
        this.pivotNode = pivotNode;
        this.targetNode = targetNode;
        this.intermNode = intermNode;
        this.intermId = intermId;
        this.importance = importance;
        this.totalFreq = totalFreq;
        setPredicateInfo();
//        init();
    }

    private void init() {
        setEntropy();
        setPMI();
        setPmidPredicateRatio();
        setYear();
        setJaccardSimilarity();
        setNMS();
        setOntology();
    }

    private void setPredicateInfo() {
        ABrelation = new ArrayList<>();
        BCrelation = new ArrayList<>();
        String sql = "SELECT id, interaction, type from relation where intermediate_id=?";
        String sqlInfo = "SELECT year,direction, pmid from relation_info where relation_id=?";
        List<Map<String, Object>> ls = JDBCHelper.query(DbConnector.LABELSYS, sql, intermId);
        for (Map<String, Object> row : ls) {
            String predicate = (String) row.get("interaction");
            int relationId = (int) row.get("id");
            int _type = (int) row.get("type");
            List<Map<String, Object>> ls2 =
                    JDBCHelper.query(DbConnector.LABELSYS, sqlInfo, relationId);
            for (Map<String, Object> row2 : ls2) {
                int year = (int) row2.get("year");
                int direction = (int) row2.get("direction");
                String pmid = (String) row2.get("pmid");
                LabeledInfo predicateItem = new LabeledInfo(direction, predicate, pmid, year);
                if (_type == 1) {
                    ABrelation.add(predicateItem);
                } else {
                    BCrelation.add(predicateItem);
                }
            }
        }
    }


    private void setEntropy() {
        LinkedHashMultiset absHmap = LinkedHashMultiset.create(ABrelation);
        LinkedHashMultiset bcsHmap = LinkedHashMultiset.create(BCrelation);
        Iterator<Multiset.Entry<LabeledInfo>> abIt = absHmap.entrySet().iterator();
        Iterator<Multiset.Entry<LabeledInfo>> bcIt = bcsHmap.entrySet().iterator();

        while (abIt.hasNext() || bcIt.hasNext()) {
            if (abIt.hasNext()) {
                Multiset.Entry<LabeledInfo> itemAB = abIt.next();
                double probAB = (double) itemAB.getCount() / absHmap.size();
                entropyAB += Math.log(probAB) * probAB;
            }
            if (bcIt.hasNext()) {
                Multiset.Entry<LabeledInfo> itemBC = bcIt.next();
                double probBC = (double) itemBC.getCount() / bcsHmap.size();
                entropyBC += Math.log(probBC) * probBC;
            }
        }
        //normalized
        entropyAB /= absHmap.elementSet().size();
        entropyBC /= bcsHmap.elementSet().size();
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
        String sql_AB = "SELECT sum(count) as totalC FROM pmid_count " +
                "WHERE pmid in (" + sb_AB.deleteCharAt(sb_AB.length() - 1) + ")";
        String sql_BC = "SELECT sum(count) as totalC FROM pmid_count " +
                "WHERE pmid in (" + sb_BC.deleteCharAt(sb_BC.length() - 1) + ")";
        BigDecimal countAB =
                (BigDecimal) JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql_AB, paramsAB).get(0).get("totalC");
        BigDecimal countBC =
                (BigDecimal) JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql_BC, paramsBC).get(0).get("totalC");
        pmidPredicateRatioAB = (double) ABrelation.size() / countAB.intValue();
        pmidPredicateRatioBC = (double) BCrelation.size() / countBC.intValue();
    }

    private void setPMI() {
        double pmi = (double) ABrelation.size() / (pivotNode.getFrequency() * intermNode.getFrequency());
        pmiAB = Math.log(pmi * totalFreq);
        pmi = (double) BCrelation.size() / (intermNode.getFrequency() * targetNode.getFrequency());
        pmiBC = Math.log(pmi * totalFreq);
    }

    private void setYear() {
        yearAB = pivotNode.getEndYear() + 1 - Collections.max(ABrelation).year;
        yearBC = pivotNode.getEndYear() + 1 - Collections.max(BCrelation).year;
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

    public static ArrayList<Attribute> getStatAttributes() {
        if (statAttributes == null) {
            statAttributes = new ArrayList<>();
            statAttributes.add(new Attribute("count(AB)"));
            statAttributes.add(new Attribute("count(BC)"));
            statAttributes.add(new Attribute("normalized count(AB)"));
            statAttributes.add(new Attribute("normalized count(BC)"));
            statAttributes.add(new Attribute("PMI(AB)"));
            statAttributes.add(new Attribute("PMI(BC)"));
            statAttributes.add(new Attribute("entropy(AB)"));
            statAttributes.add(new Attribute("entropy(BC)"));
            statAttributes.add(new Attribute("jaccard similarity(AB)"));
            statAttributes.add(new Attribute("jaccard similarity(BC)"));
            statAttributes.add(new Attribute("NMS similarity(AB)"));
            statAttributes.add(new Attribute("NMS similarity(BC)"));
            statAttributes.add(new Attribute("ontology mentioned(AB)"));
            statAttributes.add(new Attribute("ontology mentioned(BC)"));
            statAttributes.add(new Attribute("frequency(B)"));
            statAttributes.add(new Attribute("neighbor count(B)"));
            statAttributes.add(new Attribute("novel(B)"));
            statAttributes.add(new Attribute("time to indication(AB)"));
            statAttributes.add(new Attribute("time to indication(BC)"));
            ArrayList<String> attrVals = new ArrayList<>(2);
            attrVals.add("important");
            attrVals.add("unimportant");
            statAttributes.add(new Attribute("label", attrVals));
        }
        return statAttributes;
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

    public int getImportance() {
        return importance;
    }

    private String getrelationStr(ArrayList<LabeledInfo> relations, boolean isAB) {
        StringBuilder sb = new StringBuilder();
        for (LabeledInfo item : relations) {
            String predicate = item.getPredicate();
            sb.append(predicate).append(" ");
        }
        return sb.toString();
    }

    public String getABrelationStr() {
        return getrelationStr(ABrelation, true);
    }

    public String getBCrelationStr() {
        return getrelationStr(BCrelation, false);
    }

    public LiteratureNode getIntermNode() {
        return intermNode;
    }

    public double getPmidPredicateRatioAB() {
        return pmidPredicateRatioAB;
    }

    public Instance getStatInst() {
        DenseInstance instance = new DenseInstance(statAttributes.size());
        Instances dataSet = new Instances("statAttr", statAttributes, 0);
        instance.setDataset(dataSet);
        dataSet.setClassIndex(dataSet.numAttributes() - 1);
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
        String label = (importance == 1) ? "unimportant" : "important";
        instance.setValue(19, label);
        return instance;
    }

    public Set<String> getPmidSet() {
        Set<String> pmidSet = new HashSet<>();
        for (LabeledInfo item : ABrelation) {
            pmidSet.add(item.pmid);
        }
        for (LabeledInfo item : BCrelation) {
            pmidSet.add(item.pmid);
        }
        return pmidSet;
    }
    @Override
    public String toString() {
        return intermNode + ", importance:" + importance;
    }
}
