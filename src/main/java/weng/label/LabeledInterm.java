package weng.label;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import weng.feature.*;
import weng.node.LiteratureNode;
import weng.util.JDBCHelper;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weng.util.calculator.*;
import weng.util.feature.Names;
import weng.util.file.Predicate;
import weng.util.file.PredicateInfo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lbj23k on 2017/4/13.
 */
public class LabeledInterm {
    private HashSet<String> nonNovelSet;
    private static ArrayList<Attribute> statAttributes;

    private int totalFreq;
    private LiteratureNode pivotNode;
    private LiteratureNode intermNode;
    private LiteratureNode targetNode;
    private ArrayList<LabeledInfo> ABrelation;
    private ArrayList<LabeledInfo> BCrelation;
    private int importance;
    private int intermId;
    private double entropyAB;
    private double entropyBC;
    private double pmiAB;
    private double pmiBC;
    private double weightedNormalizedPredicationCountAB;
    private double weightedNormalizedPredicationCountBC;
    private int yearAB;
    private int yearBC;
    private double jaccardSimilarityAB;
    private double jaccardSimilarityBC;
    private double NMDSimilarityAB;
    private double NMDSimilarityBC;
    private double frequencyB;
    private static final float predicateMiddleWeight = 1f;
    private double crossEntropy;
    private double intraEntropyAB;
    private double intraEntropyBC;
    private double esRatioAB;
    private double esRatioBC;
    private double eRatioAB;
    private double eRatioBC;
    private double sRatioAB;
    private double sRatioBC;
    private HashMap<String, Integer> semTypeAppearedList;
    private boolean ifABCRelationFiltered;

    static {
        statAttributes = getStatAttributes();
    }

    public LabeledInterm(LiteratureNode pivotNode, LiteratureNode intermNode, LiteratureNode targetNode,
                         int intermId, int importance, int totalFreq, Connection connLSUMLS,
                         Connection connSem, Connection connLit,
                         HashMap<String, HashMap<String, Integer>> semTypeMatrixByIntermediate) throws IOException {
        this.pivotNode = pivotNode;
        this.targetNode = targetNode;
        this.intermNode = intermNode;
        this.intermId = intermId;
        this.importance = importance;
        this.totalFreq = totalFreq;
        this.ifABCRelationFiltered = setPredicateInfo(connLSUMLS);
        this.nonNovelSet = setNonNovelSet(connSem);

        if (!ifABCRelationFiltered) {
            setEntropy();
            setPMI();

            List<String> abPmids = ABrelation.stream().map(LabeledInfo::getPmid).collect(Collectors.toList());
            List<String> bcPmids = BCrelation.stream().map(LabeledInfo::getPmid).collect(Collectors.toList());
            Predication predication = new Predication(
                    WeightedNormalizedPredicationCalc.count(connLit, abPmids),
                    WeightedNormalizedPredicationCalc.count(connLit, bcPmids));
            this.weightedNormalizedPredicationCountAB = predication.getAb();
            this.weightedNormalizedPredicationCountBC = predication.getBc();

            setYear();

            this.jaccardSimilarityAB = LabeledFeature.jaccardSimilarity(
                    pivotNode.df(connLit),
                    intermNode.df(connLit),
                    Sets.intersection(
                            pivotNode.getDocuments(connLit),
                            intermNode.getDocuments(connLit)
                    ).size());
            this.jaccardSimilarityBC = LabeledFeature.jaccardSimilarity(
                    intermNode.df(connLit),
                    targetNode.df(connLit),
                    Sets.intersection(
                            intermNode.getDocuments(connLit),
                            targetNode.getDocuments(connLit)
                    ).size());

            this.NMDSimilarityAB = LabeledFeature.normalizedMedlineSimilarity(
                    pivotNode.df(connLit),
                    intermNode.df(connLit),
                    Sets.intersection(
                            pivotNode.getDocuments(connLit),
                            intermNode.getDocuments(connLit)
                    ).size());
            this.NMDSimilarityBC = LabeledFeature.normalizedMedlineSimilarity(
                    intermNode.df(connLit),
                    targetNode.df(connLit),
                    Sets.intersection(
                            intermNode.getDocuments(connLit),
                            targetNode.getDocuments(connLit)
                    ).size());

            this.frequencyB = intermNode.getFreq();

            HashMap<String, String> predicateESs = PredicateInfo.predicateESs();
            HashMap<String, Float> predicateImportance = PredicateInfo.predicateImportance(predicateMiddleWeight);
            HashMap<String, Float> abEsWeight = PathEsClassCalc.labeledWeight(predicateESs, predicateImportance, ABrelation);
            HashMap<String, Float> bcEsWeight = PathEsClassCalc.labeledWeight(predicateESs, predicateImportance, BCrelation);

            Path path = new Path(CrossEntropyCalc.count(abEsWeight, bcEsWeight));
            this.crossEntropy = path.getLink();

            predication = new Predication(
                    IntraEntropyCalc.count(abEsWeight),
                    IntraEntropyCalc.count(bcEsWeight));
            this.intraEntropyAB = predication.getAb();
            this.intraEntropyBC = predication.getBc();

            predication = new Predication(
                    EsRatioCalc.count(abEsWeight),
                    EsRatioCalc.count(bcEsWeight));
            this.esRatioAB = predication.getAb();
            this.esRatioBC = predication.getBc();

            predication = new Predication(
                    predicateTypeRatioCalc.count(abEsWeight, "escalating"),
                    predicateTypeRatioCalc.count(bcEsWeight, "escalating"));
            this.eRatioAB = predication.getAb();
            this.eRatioBC = predication.getBc();

            predication = new Predication(
                    predicateTypeRatioCalc.count(abEsWeight, "suppressing"),
                    predicateTypeRatioCalc.count(bcEsWeight, "suppressing"));
            this.sRatioAB = predication.getAb();
            this.sRatioBC = predication.getBc();

            setSemTypeAppearedList(semTypeMatrixByIntermediate); // TODO: Test feature: semantic type
        }
    }

    public void setSemTypeAppearedList(HashMap<String, HashMap<String, Integer>> semTypeMatrixByIntermediate) {
        String intermName = intermNode.getName();
        if (semTypeMatrixByIntermediate.keySet().contains(intermName)) {
            this.semTypeAppearedList = semTypeMatrixByIntermediate.get(intermName);
        } else {
            SemanticType semanticType = new SemanticType();
            this.semTypeAppearedList = semanticType.initSemGroupAppearedList();
        }
    }

    public boolean getIfABCRelationFiltered() {
        return this.ifABCRelationFiltered;
    }

    private boolean setPredicateInfo(Connection connLSUMLS) {
        ABrelation = new ArrayList<>();
        BCrelation = new ArrayList<>();
        String sql = "SELECT id, interaction, type from relation where intermediate_id=?";
        String sqlInfo = "SELECT year, direction, pmid from relation_info where relation_id=?";
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> ls = jdbcHelper.query(connLSUMLS, sql, intermId);
        for (Map<String, Object> row : ls) {
            String predicate = (String) row.get("interaction");
            int relationId = (int) row.get("id");
            int _type = (int) row.get("type");
            List<Map<String, Object>> ls2 = jdbcHelper.query(connLSUMLS, sqlInfo, relationId);
            for (Map<String, Object> row2 : ls2) {
                int year = (int) row2.get("year");
                int direction = (int) row2.get("direction");
                String pmid = (String) row2.get("pmid");
                LabeledInfo predicateItem = new LabeledInfo(direction, predicate, pmid, year);
                String predicateName = predicateItem.getPredicate();
                if (Predicate.set.contains(predicateName)) {
                    if (_type == 1) {
                        ABrelation.add(predicateItem);
                    } else {
                        BCrelation.add(predicateItem);
                    }
                }
            }
        }

        boolean ifABCRelationFiltered = false;
        int ABSize = ABrelation.size();
        int BCSize = BCrelation.size();
        if (ABSize == 0 || BCSize == 0) {
            ifABCRelationFiltered = true;
        }
        return ifABCRelationFiltered;
    }


    private void setEntropy() {
        LinkedHashMultiset allABs = LinkedHashMultiset.create(ABrelation);
        LinkedHashMultiset allBCs = LinkedHashMultiset.create(BCrelation);
        Iterator<Multiset.Entry<LabeledInfo>> abIt = allABs.entrySet().iterator();
        Iterator<Multiset.Entry<LabeledInfo>> bcIt = allBCs.entrySet().iterator();
        while (abIt.hasNext() || bcIt.hasNext()) {
            if (abIt.hasNext()) {
                Multiset.Entry<LabeledInfo> ab = abIt.next();
                double probAB = (double) ab.getCount() / allABs.size();
                entropyAB += Math.log(probAB) * probAB;
            }
            if (bcIt.hasNext()) {
                Multiset.Entry<LabeledInfo> bc = bcIt.next();
                double probBC = (double) bc.getCount() / allBCs.size();
                entropyBC += Math.log(probBC) * probBC;
            }
        }

        // normalized
        int distinctAb = allABs.elementSet().size();
        if (distinctAb != 1) {
            entropyAB /= Math.log(distinctAb);
            entropyAB = Math.abs(entropyAB);
        } else {
            entropyAB = 0.0d;
        }

        int distinctBc = allBCs.elementSet().size();
        if (distinctBc != 1) {
            entropyBC /= Math.log(distinctBc);
            entropyBC = Math.abs(entropyBC);
        } else {
            entropyBC = 0.0d;
        }
    }

    private void setPMI() {
        double pmi = (double) ABrelation.size() / ((double) pivotNode.getFreq() * (double) intermNode.getFreq());
        pmiAB = Math.log(pmi * (double) totalFreq);
        pmi = (double) BCrelation.size() / ((double) intermNode.getFreq() * (double) targetNode.getFreq());
        pmiBC = Math.log(pmi * (double) totalFreq);
    }

    private void setYear() {
        yearAB = pivotNode.getEndYear() + 1 - Collections.max(ABrelation).getYear();
        yearBC = pivotNode.getEndYear() + 1 - Collections.max(BCrelation).getYear();
    }

    public static ArrayList<Attribute> getStatAttributes() {
        if (statAttributes == null) {
            statAttributes = new ArrayList<>();
            statAttributes.add(new Attribute(Names.predicationCountAB));
            statAttributes.add(new Attribute(Names.predicationCountBC));
            statAttributes.add(new Attribute(Names.weightedPredicationCountAB));
            statAttributes.add(new Attribute(Names.weightedPredicationCountBC));
            statAttributes.add(new Attribute(Names.pmiAB));
            statAttributes.add(new Attribute(Names.pmiBC));
            statAttributes.add(new Attribute(Names.entropyAB));
            statAttributes.add(new Attribute(Names.entropyBC));
            statAttributes.add(new Attribute(Names.jaccardAB));
            statAttributes.add(new Attribute(Names.jaccardBC));
            statAttributes.add(new Attribute(Names.nmsAB));
            statAttributes.add(new Attribute(Names.nmsBC));
            statAttributes.add(new Attribute(Names.freqB));
            statAttributes.add(new Attribute(Names.neighborCountB));
            statAttributes.add(new Attribute(Names.novelB));
            statAttributes.add(new Attribute(Names.timeToIndicationAB));
            statAttributes.add(new Attribute(Names.timeToIndicationBC));
            statAttributes.add(new Attribute(Names.crossEntropyABC));
            statAttributes.add(new Attribute(Names.intraEntropyAB));
            statAttributes.add(new Attribute(Names.intraEntropyBC));
            statAttributes.add(new Attribute(Names.eRatioAB));
            statAttributes.add(new Attribute(Names.eRatioBC));
            statAttributes.add(new Attribute(Names.sRatioAB));
            statAttributes.add(new Attribute(Names.sRatioBC));

            SemanticType semanticType = new SemanticType();
            HashMap<String, Integer> semGroupAppearedList = semanticType.initSemGroupAppearedList();
            for (String semGroup : semGroupAppearedList.keySet()) {
                statAttributes.add(new Attribute(Names.semanticGroupB + semGroup));
            }

            ArrayList<String> attrVals = new ArrayList<>(2);
            attrVals.add(Names.important);
            attrVals.add(Names.unimportant);
            statAttributes.add(new Attribute(Names.label, attrVals));
        }
        return statAttributes;
    }

    private HashSet<String> setNonNovelSet(Connection connSem) {
        HashSet<String> nonNovelSet = new HashSet<>();
        String sql = "SELECT `c`.`PREFERRED_NAME` FROM `concept_semtype` AS `cs` join `concept` AS `c` " +
                "on `c`.`CONCEPT_ID`=`cs`.`CONCEPT_ID` where `cs`.`NOVEL`=\"N\";";
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> ls = jdbcHelper.query(connSem, sql);
        for (Map<String, Object> row : ls) {
            nonNovelSet.add((String) row.get("PREFERRED_NAME"));
        }
        return nonNovelSet;
    }

    public int getImportance() {
        return importance;
    }

    public String getRelationStr(ArrayList<LabeledInfo> relations) {
        StringBuilder sb = new StringBuilder();
        for (LabeledInfo item : relations) {
            String predicate = item.getPredicateFiltered();
            sb.append(predicate).append(" ");
        }
        return sb.toString();
    }

    public ArrayList<LabeledInfo> getABrelation() {
        return ABrelation;
    }

    public ArrayList<LabeledInfo> getBCrelation() {
        return BCrelation;
    }

    public LiteratureNode getIntermNode() {
        return intermNode;
    }

    public Instance getStatInst(HashMap<String, String> umlsCuiNameMap, Connection connLit) throws SQLException {
        DenseInstance instance = new DenseInstance(statAttributes.size());
        Instances dataSet = new Instances("statAttr", statAttributes, 0);
        instance.setDataset(dataSet);
        dataSet.setClassIndex(dataSet.numAttributes() - 1);
        instance.setValue(0, ABrelation.size());
        instance.setValue(1, BCrelation.size());
        instance.setValue(2, weightedNormalizedPredicationCountAB);
        instance.setValue(3, weightedNormalizedPredicationCountBC);
        instance.setValue(4, pmiAB);
        instance.setValue(5, pmiBC);
        instance.setValue(6, entropyAB);
        instance.setValue(7, entropyBC);
        instance.setValue(8, jaccardSimilarityAB);
        instance.setValue(9, jaccardSimilarityBC);
        instance.setValue(10, NMDSimilarityAB);
        instance.setValue(11, NMDSimilarityBC);
        instance.setValue(12, frequencyB);
        instance.setValue(13, intermNode.getNeighborCount(umlsCuiNameMap, connLit));
        instance.setValue(14, (nonNovelSet.contains(intermNode.getName())) ? 0 : 1);
        instance.setValue(15, yearAB);
        instance.setValue(16, yearBC);
//        instance.setValue(17, crossEntropy);
//        instance.setValue(18, intraEntropyAB);
//        instance.setValue(19, intraEntropyBC);
//        instance.setValue(20, eRatioAB);
//        instance.setValue(21, eRatioBC);
//        instance.setValue(22, sRatioAB);
//        instance.setValue(23, sRatioBC);
        int attIndex = 17;
//        for (String semType : semTypeAppearedList.keySet()) { // TODO: Test feature: semantic type
//            int isSemTypeAppeared = semTypeAppearedList.get(semType);
//            instance.setValue(attIndex, isSemTypeAppeared);
//            attIndex++;
//        }

        String label = (importance == 1) ? "unimportant" : "important";
        instance.setValue(attIndex, label);
        return instance;
    }

    public List<String> abcPaths(List<String> paths) {
        paths.add(pivotNode.getName() + "_" + intermNode.getName() + "_" + targetNode.getName());
        return paths;
    }

    @Override
    public String toString() {
        return intermNode + ", importance:" + importance;
    }
}
