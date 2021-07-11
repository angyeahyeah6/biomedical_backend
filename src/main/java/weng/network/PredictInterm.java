package weng.network;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import weng.feature.*;
import weng.node.LiteratureNode;
import weng.util.calculator.*;
import weng.util.noSqlColumn.PredicationCount;
import weng.util.noSqlColumn.AbcPredicate;
import weng.util.feature.Names;
import weng.util.file.Predicate;
import weng.util.file.PredicateInfo;
import weng.util.DbConnector;
import weng.util.JDBCHelper;
import weng.util.Utils;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.SparseInstance;
import weng.util.file.RepurposingEval;
import weng.util.noSqlData.AbcPredicateInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lbj23k on 2017/6/2.
 */
public class PredictInterm {
    /**
     * TOTALFREQ
     * => All concepts' frequency between 1809 and 2004
     * => Sql: SELECT sum(`freq`) as sf FROM `umls_concept_by_year` where year between 1809 and 2004;
     * <p>
     * TOTAL_PREDICATION_FREQ
     * => All predications' frequency between 1809 and 2004
     * => Sql: SELECT COUNT(*) FROM `umls_predication_aggregate_filtered` where `is_exist`=1 and `year` between 1809 and 2004;
     */
    private static final int TOTALFREQ = 88069880;
    private static final int TOTAL_PREDICATION_FREQ = 21704833;
    private static ArrayList<Attribute> statAttributes;
    private static HashMap<String, Integer> contentWordMap;
    private static HashSet<String> nonNovelSet;

    private LiteratureNode pivotNode;
    private LiteratureNode intermNode;
    private LiteratureNode targetNode;
    private ArrayList<PredictInfo> ABrelation;
    private ArrayList<PredictInfo> BCrelation;
    private LinkedHashMultiset absmap;
    private LinkedHashMultiset bcsmap;
    private double entropyAB;
    private double entropyBC;
    private double pmiAB;
    private double pmiBC;

    private double weightedPredicationCountAB;
    private double weightedPredicationCountBC;
    private double normalizedWeightedPredicationCountAB;
    private double normalizedWeightedPredicationCountBC;
    private double weightedNormalizedPredicationCountAB;
    private double weightedNormalizedPredicationCountBC;

    private int yearAB;
    private int yearBC;
    private double jaccardSimilarityAB;
    private double jaccardSimilarityBC;
    private double NMDSimilarityAB;
    private double NMDSimilarityBC;
    private double frequencyB;

    private int ABCPatternOffset;
    private int ABCPatternAmplify;
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

    private static List<String> predicateLs = new Utils().readLineFile(Predicate.filePath);
    public Map<String, Double> predicateCountMap;
    private int importance = 0;
    private boolean ifABCRelationFiltered;

    static {
        if (statAttributes == null) {
            statAttributes = createAttr();
        }

        setContentWordMap(predicateLs);

        if (nonNovelSet == null) {
            setNonNovelSet();
        }
    }

    public PredictInterm(LiteratureNode pivotNode, LiteratureNode intermNode, LiteratureNode targetNode) {
        this.intermNode = intermNode;
        this.pivotNode = pivotNode;
        this.targetNode = targetNode;
        predicateCountMap = new HashMap<>();
        for (String predicate : predicateLs) {
            predicateCountMap.put(predicate, 0.0);
        }
    }

    public void setPredicateInfo(List<Map<String, Object>> abItem, List<Map<String, Object>> bcItem, Connection connLit,
                                 HashMap<String, HashMap<String, Integer>> semTypeMatrixByIntermediate,
                                 int startYear, int endYear) throws SQLException, IOException {
        ABrelation = new ArrayList<>();
        BCrelation = new ArrayList<>();
        int large = (abItem.size() > bcItem.size()) ? abItem.size() : bcItem.size();
        for (int i = 0; i < large; i++) {
            if (i < abItem.size()) {
                String neighborCui = (String) abItem.get(i).get("neighbor");
                if (neighborCui.equals(intermNode.getCui())) {
                    String predicate = (String) abItem.get(i).get("predicate");
                    String pmid_AB = (String) abItem.get(i).get("pmid");
                    int year_AB = (int) abItem.get(i).get("year");
                    if (predicateCountMap.containsKey(predicate)) {
                        predicateCountMap.put(predicate, predicateCountMap.get(predicate) + 1);
                    }
                    int direction = (int) abItem.get(i).get("is_exist");
                    ABrelation.add(new PredictInfo(predicate, pmid_AB, year_AB, direction));
                }
            }
            if (i < bcItem.size()) {
                String neighborCui = (String) bcItem.get(i).get("neighbor");
                if (neighborCui.equals(intermNode.getCui())) {
                    String predicate = (String) bcItem.get(i).get("predicate");
                    String pmid_BC = (String) bcItem.get(i).get("pmid");
                    int year_BC = (int) bcItem.get(i).get("year");
                    if (predicateCountMap.containsKey(predicate)) {
                        predicateCountMap.put(predicate, predicateCountMap.get(predicate) + 1);
                    }
                    int direction = (int) bcItem.get(i).get("is_exist");
                    BCrelation.add(new PredictInfo(predicate, pmid_BC, year_BC, direction));
                }
            }
        }
        absmap = LinkedHashMultiset.create(ABrelation);
        bcsmap = LinkedHashMultiset.create(BCrelation);

        setIfABCRelationFiltered(ABrelation.size() == 0 || BCrelation.size() == 0);
        initAttr(getIfABCRelationFiltered(), connLit, semTypeMatrixByIntermediate, startYear, endYear);
    }

    public void setPredicateInfo(List<AbcPredicateInfo> predicates,
                                 DBCollection collection) throws SQLException, IOException {
        List<String> abPredicates = new ArrayList<>();
        predicates.forEach(e -> abPredicates.add(e.getAbPredicate()));
        Map<String, Long> abPredicateCount = abPredicates.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        Map<String, Integer> abPredicateCountInteger = predicateCountToInteger(abPredicateCount);

        List<String> bcPredicates = new ArrayList<>();
        predicates.forEach(e -> bcPredicates.add(e.getBcPredicate()));
        Map<String, Long> bcPredicateCount = bcPredicates.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        Map<String, Integer> bcPredicateCountInteger = predicateCountToInteger(bcPredicateCount);

        DBObject document = new BasicDBObject();
        document.put(AbcPredicate.drug, pivotNode.getName());
        document.put(AbcPredicate.intermediate, intermNode.getName());
        document.put(AbcPredicate.disease, targetNode.getName());
        document.put(AbcPredicate.abPredicates, abPredicateCountInteger);
        document.put(AbcPredicate.bcPredicates, bcPredicateCountInteger);
        collection.insert(document, WriteConcern.UNACKNOWLEDGED);
    }

    public Map<String, Integer> predicateCountToInteger(Map<String, Long> predicateCount) {
        Map<String, Integer> predicateCountInteger = new HashMap<>();
        for (String predicate : predicateCount.keySet()) {
            predicateCountInteger.put(predicate, Integer.parseInt(predicateCount.get(predicate).toString()));
        }
        return predicateCountInteger;
    }

    public void setPredicateInfo(List<Map<String, Object>> abItem, List<Map<String, Object>> bcItem,
                                 DBCollection collection) throws SQLException, IOException {
        ABrelation = new ArrayList<>();
        BCrelation = new ArrayList<>();
        int large = (abItem.size() > bcItem.size()) ? abItem.size() : bcItem.size();
        for (int i = 0; i < large; i++) {
            if (i < abItem.size()) {
                String neighborCui = (String) abItem.get(i).get("neighbor");
                if (neighborCui.equals(intermNode.getCui())) {
                    String predicate = (String) abItem.get(i).get("predicate");
                    String pmid_AB = (String) abItem.get(i).get("pmid");
                    int year_AB = (int) abItem.get(i).get("year");
                    if (predicateCountMap.containsKey(predicate)) {
                        predicateCountMap.put(predicate, predicateCountMap.get(predicate) + 1);
                    }
                    int direction = (int) abItem.get(i).get("is_exist");
                    ABrelation.add(new PredictInfo(predicate, pmid_AB, year_AB, direction));
                }
            }
            if (i < bcItem.size()) {
                String neighborCui = (String) bcItem.get(i).get("neighbor");
                if (neighborCui.equals(intermNode.getCui())) {
                    String predicate = (String) bcItem.get(i).get("predicate");
                    String pmid_BC = (String) bcItem.get(i).get("pmid");
                    int year_BC = (int) bcItem.get(i).get("year");
                    if (predicateCountMap.containsKey(predicate)) {
                        predicateCountMap.put(predicate, predicateCountMap.get(predicate) + 1);
                    }
                    int direction = (int) bcItem.get(i).get("is_exist");
                    BCrelation.add(new PredictInfo(predicate, pmid_BC, year_BC, direction));
                }
            }
        }

        DBObject document = new BasicDBObject();
        document.put(PredicationCount.drug, pivotNode.getName());
        document.put(PredicationCount.intermediate, intermNode.getName());
        document.put(PredicationCount.disease, targetNode.getName());
        document.put(PredicationCount.count_ab, ABrelation.size());
        document.put(PredicationCount.count_bc, BCrelation.size());
        collection.insert(document, WriteConcern.UNACKNOWLEDGED);
    }

    public void setIfABCRelationFiltered(boolean ifABCRelationFiltered) {
        this.ifABCRelationFiltered = ifABCRelationFiltered;
    }

    public boolean getIfABCRelationFiltered() {
        return ifABCRelationFiltered;
    }

    private void initAttr(boolean ifABCRelationFiltered,
                          Connection connLit,
                          HashMap<String, HashMap<String, Integer>> semTypeMatrixByIntermediate,
                          int startYear, int endYear) throws SQLException, IOException {
        if (!ifABCRelationFiltered) {
            setEntropy();
            setPMI();

            List<String> abPmids = ABrelation.stream().map(e -> e.getPmid()).collect(Collectors.toList());
            List<String> bcPmids = BCrelation.stream().map(e -> e.getPmid()).collect(Collectors.toList());
            Predication predication = new Predication(
                    WeightedPredicationCalc.count(connLit, abPmids),
                    WeightedPredicationCalc.count(connLit, bcPmids));
            this.weightedPredicationCountAB = predication.getAb();
            this.weightedPredicationCountBC = predication.getBc();

            predication = new Predication(
                    NormalizedWeightedPredicationCalc.count(connLit, abPmids),
                    NormalizedWeightedPredicationCalc.count(connLit, bcPmids));
            this.normalizedWeightedPredicationCountAB = predication.getAb();
            this.normalizedWeightedPredicationCountBC = predication.getBc();

            predication = new Predication(
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
            HashMap<String, Float> abEsWeight = PathEsClassCalc.predictWeight(predicateESs, predicateImportance, ABrelation);
            HashMap<String, Float> bcEsWeight = PathEsClassCalc.predictWeight(predicateESs, predicateImportance, BCrelation);
            InterPattern interPattern = new InterPattern(abEsWeight, bcEsWeight);

            this.ABCPatternOffset = interPattern.getOffset();
            this.ABCPatternAmplify = interPattern.getAmplify();

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

    private void setEntropy() {
        Iterator<Multiset.Entry<PredictInfo>> it1 = absmap.entrySet().iterator();
        Iterator<Multiset.Entry<PredictInfo>> it2 = bcsmap.entrySet().iterator();
        while (it1.hasNext() || it2.hasNext()) {
            if (it1.hasNext()) {
                Multiset.Entry<PredictInfo> itemAB = it1.next();
                double probAB = (double) itemAB.getCount() / absmap.size();
                entropyAB += Math.log(probAB) * probAB;
            }
            if (it2.hasNext()) {
                Multiset.Entry<PredictInfo> itemBC = it2.next();
                double probBC = (double) itemBC.getCount() / bcsmap.size();
                entropyBC += Math.log(probBC) * probBC;
            }
        }

        //normalized
        int distinctAb = absmap.elementSet().size();
        if (distinctAb != 1) {
            entropyAB /= Math.log(distinctAb);
            entropyAB = Math.abs(entropyAB);
        } else {
            entropyAB = 0.0d;
        }

        int distinctBc = bcsmap.elementSet().size();
        if (distinctBc != 1) {
            entropyBC /= Math.log(distinctBc);
            entropyBC = Math.abs(entropyBC);
        } else {
            entropyBC = 0.0d;
        }
    }

    private void setPMI() {
        double pmi = (double) ABrelation.size() / ((double) pivotNode.getFreq() * (double) intermNode.getFreq());
        pmiAB = Math.log(pmi * (double) TOTAL_PREDICATION_FREQ);
        pmi = (double) BCrelation.size() / ((double) intermNode.getFreq() * (double) targetNode.getFreq());
        pmiBC = Math.log(pmi * (double) TOTAL_PREDICATION_FREQ);
    }

    private void setYear() {
        yearAB = pivotNode.getEndYear() + 1 - Collections.max(ABrelation).getYear();
        yearBC = pivotNode.getEndYear() + 1 - Collections.max(BCrelation).getYear();
    }

    public Instance getStatInst(Map<String, Integer> neighborCounts, HashMap<String, String> umlsCuiNameMap, DBCollection collInfo, DBCollection collRef) throws SQLException {
        DenseInstance instance = new DenseInstance(attributes().size());
        instance.setValue(0, weightedNormalizedPredicationCountAB);
        instance.setValue(1, weightedNormalizedPredicationCountBC);
        instance.setValue(2, pmiAB);
        instance.setValue(3, pmiBC);
        instance.setValue(4, entropyAB);
        instance.setValue(5, entropyBC);
        instance.setValue(6, jaccardSimilarityAB);
        instance.setValue(7, jaccardSimilarityBC);
        instance.setValue(8, NMDSimilarityAB);
        instance.setValue(9, NMDSimilarityBC);
        instance.setValue(10, intermNode.getFreq());

        String intermName = intermNode.getName();
        int neighborCount;
        if (neighborCounts.containsKey(intermName)) {
            neighborCount = neighborCounts.get(intermName);
        } else {
            neighborCount = intermNode.getNeighborCount(umlsCuiNameMap, collInfo, collRef);
            neighborCounts.put(intermName, neighborCount);
            try {
                FileWriter fileWriter = new FileWriter(new File(RepurposingEval.neighborCount), true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(intermName + "\t" + neighborCount);
                bufferedWriter.newLine();
                bufferedWriter.flush();
                bufferedWriter.close();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        instance.setValue(11, neighborCount);

        instance.setValue(12, (nonNovelSet.contains(intermName)) ? 0 : 1);
        instance.setValue(13, yearAB);
        instance.setValue(14, yearBC);
        instance.setValue(15, crossEntropy);
        instance.setValue(16, intraEntropyAB);
        instance.setValue(17, intraEntropyBC);
        instance.setValue(18, eRatioAB);
        instance.setValue(19, eRatioBC);
        instance.setValue(20, sRatioAB);
        instance.setValue(21, sRatioBC);

        int attIndex = 22;
        for (String semType : semTypeAppearedList.keySet()) {
            int isSemTypeAppeared = semTypeAppearedList.get(semType);
            instance.setValue(attIndex, isSemTypeAppeared);
            attIndex++;
        }
        return instance;
    }

    public Instance getPredictStatInst(Map<String, Integer> neighborCounts, HashMap<String, String> umlsCuiNameMap, DBCollection collInfo, DBCollection collRef) throws SQLException {
        DenseInstance instance = new DenseInstance(predictAttributes().size());
        instance.setValue(0, weightedNormalizedPredicationCountAB);
        instance.setValue(1, weightedNormalizedPredicationCountBC);
        instance.setValue(2, pmiAB);
        instance.setValue(3, pmiBC);
        instance.setValue(4, entropyAB);
        instance.setValue(5, entropyBC);
        instance.setValue(6, jaccardSimilarityAB);
        instance.setValue(7, jaccardSimilarityBC);
        instance.setValue(8, NMDSimilarityAB);
        instance.setValue(9, NMDSimilarityBC);
        instance.setValue(10, intermNode.getFreq());

        String intermName = intermNode.getName();
        int neighborCount;
        if (neighborCounts.containsKey(intermName)) {
            neighborCount = neighborCounts.get(intermName);
        } else {
            neighborCount = intermNode.getNeighborCount(umlsCuiNameMap, collInfo, collRef);
            neighborCounts.put(intermName, neighborCount);
        }
        instance.setValue(11, neighborCount);

        instance.setValue(12, (nonNovelSet.contains(intermName)) ? 0 : 1);
        instance.setValue(13, yearAB);
        instance.setValue(14, yearBC);
        instance.setValue(15, crossEntropy);
        instance.setValue(16, intraEntropyAB);
        instance.setValue(17, intraEntropyBC);
        instance.setValue(18, eRatioAB);
        instance.setValue(19, eRatioBC);
        instance.setValue(20, sRatioAB);
        instance.setValue(21, sRatioBC);
        return instance;
    }

    public Instance getOldStatInst(Map<String, Integer> neighborCounts, HashMap<String, String> umlsCuiNameMap, DBCollection collInfo, DBCollection collRef) throws SQLException {
        DenseInstance instance = new DenseInstance(oldAttributes().size());
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
        instance.setValue(12, intermNode.getFreq());

        String intermName = intermNode.getName();
        int neighborCount;
        if (neighborCounts.containsKey(intermName)) {
            neighborCount = neighborCounts.get(intermName);
        } else {
            neighborCount = intermNode.getNeighborCount(umlsCuiNameMap, collInfo, collRef);
            neighborCounts.put(intermName, neighborCount);
        }
        instance.setValue(13, neighborCount);

        instance.setValue(14, (nonNovelSet.contains(intermName)) ? 0 : 1);
        instance.setValue(15, yearAB);
        instance.setValue(16, yearBC);
        return instance;
    }

    public Instance getStatInst(HashMap<String, String> umlsCuiNameMap, Connection connLit) throws SQLException {
        DenseInstance instance = new DenseInstance(statAttributes.size());
//        instance.setValue(0, ABrelation.size());
//        instance.setValue(1, BCrelation.size());

//        instance.setValue(2, weightedPredicationCountAB);
//        instance.setValue(3, weightedPredicationCountBC);
//        instance.setValue(2, normalizedWeightedPredicationCountAB);
//        instance.setValue(3, normalizedWeightedPredicationCountBC);
        instance.setValue(0, weightedNormalizedPredicationCountAB);
        instance.setValue(1, weightedNormalizedPredicationCountBC);

        instance.setValue(2, pmiAB);
        instance.setValue(3, pmiBC);
        instance.setValue(4, entropyAB);
        instance.setValue(5, entropyBC);
        instance.setValue(6, jaccardSimilarityAB);
        instance.setValue(7, jaccardSimilarityBC);
        instance.setValue(8, NMDSimilarityAB);
        instance.setValue(9, NMDSimilarityBC);
        instance.setValue(10, intermNode.getFreq());
        instance.setValue(11, intermNode.getNeighborCount(umlsCuiNameMap, connLit));
        instance.setValue(12, (nonNovelSet.contains(intermNode.getName())) ? 0 : 1);
        instance.setValue(13, yearAB);
        instance.setValue(14, yearBC);
//        instance.setValue(17, ABCPatternOffset);
//        instance.setValue(18, ABCPatternAmplify);
        instance.setValue(15, crossEntropy);
        instance.setValue(16, intraEntropyAB);
        instance.setValue(17, intraEntropyBC);
//        instance.setValue(19, esRatioAB);
//        instance.setValue(20, esRatioBC);
        instance.setValue(18, eRatioAB);
        instance.setValue(19, eRatioBC);
        instance.setValue(20, sRatioAB);
        instance.setValue(21, sRatioBC);

        int attIndex = 22;
        for (String semType : semTypeAppearedList.keySet()) { // TODO: Test feature: semantic type
            int isSemTypeAppeared = semTypeAppearedList.get(semType);
            instance.setValue(attIndex, isSemTypeAppeared);
            attIndex++;
        }

        return instance;
    }

    public Instance getStatInst(Connection connSem, Connection connLit) throws SQLException {
        DenseInstance instance = new DenseInstance(statAttributes.size());
//        instance.setValue(0, ABrelation.size());
//        instance.setValue(1, BCrelation.size());

//        instance.setValue(2, weightedPredicationCountAB);
//        instance.setValue(3, weightedPredicationCountBC);
//        instance.setValue(2, normalizedWeightedPredicationCountAB);
//        instance.setValue(3, normalizedWeightedPredicationCountBC);
        instance.setValue(0, weightedNormalizedPredicationCountAB);
        instance.setValue(1, weightedNormalizedPredicationCountBC);

        instance.setValue(2, pmiAB);
        instance.setValue(3, pmiBC);
        instance.setValue(4, entropyAB);
        instance.setValue(5, entropyBC);
        instance.setValue(6, jaccardSimilarityAB);
        instance.setValue(7, jaccardSimilarityBC);
        instance.setValue(8, NMDSimilarityAB);
        instance.setValue(9, NMDSimilarityBC);
        instance.setValue(10, intermNode.getFreq());
        instance.setValue(11, intermNode.getNeighborCount(connSem, connLit));
        instance.setValue(12, (nonNovelSet.contains(intermNode.getName())) ? 0 : 1);
        instance.setValue(13, yearAB);
        instance.setValue(14, yearBC);
//        instance.setValue(17, ABCPatternOffset);
//        instance.setValue(18, ABCPatternAmplify);
        instance.setValue(15, crossEntropy);
        instance.setValue(16, intraEntropyAB);
        instance.setValue(17, intraEntropyBC);
//        instance.setValue(19, esRatioAB);
//        instance.setValue(20, esRatioBC);
        instance.setValue(18, eRatioAB);
        instance.setValue(19, eRatioBC);
        instance.setValue(20, sRatioAB);
        instance.setValue(21, sRatioBC);

        int attIndex = 22;
        for (String semType : semTypeAppearedList.keySet()) { // TODO: Test feature: semantic type
            int isSemTypeAppeared = semTypeAppearedList.get(semType);
            instance.setValue(attIndex, isSemTypeAppeared);
            attIndex++;
        }

        return instance;
    }

    public Instance getContentInst() {
        Instance instance = new SparseInstance(contentWordMap.size());
        for (int i = 0; i < instance.numAttributes(); i++) {
            instance.setValue(i, 0);
        }
        Multiset<PredictInfo> totalSet = LinkedHashMultiset.create(absmap);
        totalSet.addAll(bcsmap);
        for (Multiset.Entry<PredictInfo> item : totalSet.entrySet()) {
            String predicate = item.getElement().getPredicate();
            if (contentWordMap.containsKey(predicate)) {
                instance.setValue(contentWordMap.get(predicate), item.getCount());
            }
        }
        return instance;
    }

    public ArrayList<PredictInfo> getABrelation() {
        return ABrelation;
    }

    public ArrayList<PredictInfo> getBCrelation() {
        return BCrelation;
    }

    private static ArrayList<Attribute> createAttr() {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute(Names.predicationCountAB));
        attributes.add(new Attribute(Names.predicationCountBC));
        attributes.add(new Attribute(Names.weightedPredicationCountAB));
        attributes.add(new Attribute(Names.weightedPredicationCountBC));
        attributes.add(new Attribute(Names.pmiAB));
        attributes.add(new Attribute(Names.pmiBC));
        attributes.add(new Attribute(Names.entropyAB));
        attributes.add(new Attribute(Names.entropyBC));
        attributes.add(new Attribute(Names.jaccardAB));
        attributes.add(new Attribute(Names.jaccardBC));
        attributes.add(new Attribute(Names.nmsAB));
        attributes.add(new Attribute(Names.nmsBC));
        attributes.add(new Attribute(Names.freqB));
        attributes.add(new Attribute(Names.neighborCountB));
        attributes.add(new Attribute(Names.novelB));
        attributes.add(new Attribute(Names.timeToIndicationAB));
        attributes.add(new Attribute(Names.timeToIndicationBC));
//        attributes.add(new Attribute(Names.offsetPatternABC));
//        attributes.add(new Attribute(Names.amplifyPatternABC));
//        attributes.add(new Attribute(Names.crossEntropyABC));
//        attributes.add(new Attribute(Names.intraEntropyAB));
//        attributes.add(new Attribute(Names.intraEntropyBC));
//        attributes.add(new Attribute(Names.esRatioAB));
//        attributes.add(new Attribute(Names.esRatioBC));
//        attributes.add(new Attribute(Names.eRatioAB));
//        attributes.add(new Attribute(Names.eRatioBC));
//        attributes.add(new Attribute(Names.sRatioAB));
//        attributes.add(new Attribute(Names.sRatioBC));

//        SemanticType semanticType = new SemanticType();
//        HashMap<String, Integer> semGroupAppearedList = semanticType.initSemGroupAppearedList();
//        for (String semGroup : semGroupAppearedList.keySet()) {
//            attributes.add(new Attribute(Names.semanticGroupB + semGroup));
//        }

        ArrayList<String> attrVals = new ArrayList<>(2);
        attrVals.add(Names.important);
        attrVals.add(Names.unimportant);
        attributes.add(new Attribute(Names.label, attrVals));
        return attributes;
    }

    public static ArrayList<Attribute> attributes() {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute(Names.weightedPredicationCountAB));
        attributes.add(new Attribute(Names.weightedPredicationCountBC));
        attributes.add(new Attribute(Names.pmiAB));
        attributes.add(new Attribute(Names.pmiBC));
        attributes.add(new Attribute(Names.entropyAB));
        attributes.add(new Attribute(Names.entropyBC));
        attributes.add(new Attribute(Names.jaccardAB));
        attributes.add(new Attribute(Names.jaccardBC));
        attributes.add(new Attribute(Names.nmsAB));
        attributes.add(new Attribute(Names.nmsBC));
        attributes.add(new Attribute(Names.freqB));
        attributes.add(new Attribute(Names.neighborCountB));
        attributes.add(new Attribute(Names.novelB));
        attributes.add(new Attribute(Names.timeToIndicationAB));
        attributes.add(new Attribute(Names.timeToIndicationBC));
        attributes.add(new Attribute(Names.crossEntropyABC));
        attributes.add(new Attribute(Names.intraEntropyAB));
        attributes.add(new Attribute(Names.intraEntropyBC));
        attributes.add(new Attribute(Names.eRatioAB));
        attributes.add(new Attribute(Names.eRatioBC));
        attributes.add(new Attribute(Names.sRatioAB));
        attributes.add(new Attribute(Names.sRatioBC));

        SemanticType semanticType = new SemanticType();
        HashMap<String, Integer> semGroupAppearedList = semanticType.initSemGroupAppearedList();
        for (String semGroup : semGroupAppearedList.keySet()) {
            attributes.add(new Attribute(Names.semanticGroupB + semGroup));
        }

        ArrayList<String> attrVals = new ArrayList<>(2);
        attrVals.add(Names.important);
        attrVals.add(Names.unimportant);
        attributes.add(new Attribute(Names.label, attrVals));
        return attributes;
    }

    public static ArrayList<Attribute> predictAttributes() {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute(Names.weightedPredicationCountAB));
        attributes.add(new Attribute(Names.weightedPredicationCountBC));
        attributes.add(new Attribute(Names.pmiAB));
        attributes.add(new Attribute(Names.pmiBC));
        attributes.add(new Attribute(Names.entropyAB));
        attributes.add(new Attribute(Names.entropyBC));
        attributes.add(new Attribute(Names.jaccardAB));
        attributes.add(new Attribute(Names.jaccardBC));
        attributes.add(new Attribute(Names.nmsAB));
        attributes.add(new Attribute(Names.nmsBC));
        attributes.add(new Attribute(Names.freqB));
        attributes.add(new Attribute(Names.neighborCountB));
        attributes.add(new Attribute(Names.novelB));
        attributes.add(new Attribute(Names.timeToIndicationAB));
        attributes.add(new Attribute(Names.timeToIndicationBC));
        attributes.add(new Attribute(Names.crossEntropyABC));
        attributes.add(new Attribute(Names.intraEntropyAB));
        attributes.add(new Attribute(Names.intraEntropyBC));
        attributes.add(new Attribute(Names.eRatioAB));
        attributes.add(new Attribute(Names.eRatioBC));
        attributes.add(new Attribute(Names.sRatioAB));
        attributes.add(new Attribute(Names.sRatioBC));

        ArrayList<String> attrVals = new ArrayList<>(2);
        attrVals.add(Names.important);
        attrVals.add(Names.unimportant);
        attributes.add(new Attribute(Names.label, attrVals));
        return attributes;
    }

    public static ArrayList<Attribute> oldAttributes() {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute(Names.predicationCountAB));
        attributes.add(new Attribute(Names.predicationCountBC));
        attributes.add(new Attribute(Names.weightedPredicationCountAB));
        attributes.add(new Attribute(Names.weightedPredicationCountBC));
        attributes.add(new Attribute(Names.pmiAB));
        attributes.add(new Attribute(Names.pmiBC));
        attributes.add(new Attribute(Names.entropyAB));
        attributes.add(new Attribute(Names.entropyBC));
        attributes.add(new Attribute(Names.jaccardAB));
        attributes.add(new Attribute(Names.jaccardBC));
        attributes.add(new Attribute(Names.nmsAB));
        attributes.add(new Attribute(Names.nmsBC));
        attributes.add(new Attribute(Names.freqB));
        attributes.add(new Attribute(Names.neighborCountB));
        attributes.add(new Attribute(Names.novelB));
        attributes.add(new Attribute(Names.timeToIndicationAB));
        attributes.add(new Attribute(Names.timeToIndicationBC));

        ArrayList<String> attrVals = new ArrayList<>(2);
        attrVals.add(Names.important);
        attrVals.add(Names.unimportant);
        attributes.add(new Attribute(Names.label, attrVals));
        return attributes;
    }

    private static void setContentWordMap(List<String> predicateLs) {
        contentWordMap = new HashMap<>();
        for (int i = 0; i < predicateLs.size(); i++) {
            contentWordMap.put(predicateLs.get(i), i);
        }
    }

    private static void setNonNovelSet() {
        nonNovelSet = new HashSet<>();
        String sql = "SELECT `c`.`PREFERRED_NAME` FROM `concept_semtype` AS `cs` join `concept` AS `c` " +
                "on `c`.`CONCEPT_ID`=`cs`.`CONCEPT_ID` where `cs`.`NOVEL`=\"N\";";
        String semmedDBName = DbConnector.SEMMED;
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> ls = jdbcHelper.query(semmedDBName, sql);
        for (Map<String, Object> row : ls) {
            nonNovelSet.add((String) row.get("PREFERRED_NAME"));
        }
    }

    public LiteratureNode getNode() {
        return intermNode;
    }

    public void setImportance(int importance) {
        this.importance = importance;
    }

    public int getImportance() {
        return importance;
    }

    public LinkedHashMultiset getABsMap() {
        return absmap;
    }

    public LinkedHashMultiset getBCsMap() {
        return bcsmap;
    }

    public static ArrayList<Attribute> getAttributes() {
        return statAttributes;
    }

    public String getIntermCui() {
        return intermNode.getCui();
    }
}
