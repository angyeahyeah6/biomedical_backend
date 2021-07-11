package weng.network;

import com.mongodb.DBCollection;
import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weng.model.Classifier;
import weng.model.EvalDataAttr;
import weng.node.LiteratureNode;
import weng.util.JDBCHelper;
import weng.util.Utils;
import weng.util.feature.Names;
import weng.util.file.Stopwords;
import weng.util.file.evaluationData.EvalContent;
import weng.util.file.evaluationData.EvalOldStat;
import weng.util.file.evaluationData.EvalStat;
import weng.util.noSqlData.AbcPredicateInfo;
import weng.util.noSqlData.PredicationCountInfo;
import weng.util.repurposing.PredicateImportanceScore;
import weng.util.sqlData.Predicate;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lbj23k on 2017/6/1.
 */
public class PredictNetwork implements Serializable {
    private static final long serialVersionUID = 689123320491080199L;
    private static AbstractClassifier classifier;
    private static Instances contentInstance;
    private static List<String> attrs;
    private LiteratureNode pivotNode;
    private LiteratureNode targetNode;
    private int startYear;
    private int endYear;
    private ArrayList<PredictInterm> intermediates;
    private Instances dataUnlabeled;
    private Instances contentInsts;
    private Instances statInsts;
    private double prob;
    private int importCount;
    private List<Integer> importIndexes = new ArrayList<>();
    private boolean isValid;
    private List<String> importInterm;

    static {
        if (contentInstance == null) {
            contentInstance = new Utils().readAttrFile(EvalContent.filePath);
            Remove remove = new Remove();
            remove.setAttributeIndices("first");
            try {
                remove.setInputFormat(contentInstance);
                contentInstance = Filter.useFilter(contentInstance, remove);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public PredictNetwork(boolean runOldModel, Map<String, Integer> neighborCount, String pivotName, String targetName, int startYear, int endYear,
                          HashMap<String, String> umlsCuiNameMap, HashMap<String, String> umlsNameCuiMap, Connection connLit,
                          HashMap<String, HashMap<String, Integer>> semTypeMatrixByIntermediate,
                          DBCollection collInfo, DBCollection collRef) throws SQLException, IOException {
        setClassifier(runOldModel);
        this.startYear = startYear;
        this.endYear = endYear;
        this.pivotNode = new LiteratureNode(pivotName, umlsNameCuiMap.get(pivotName), startYear, endYear, connLit);
        this.targetNode = new LiteratureNode(targetName, umlsNameCuiMap.get(targetName), startYear, endYear, connLit);
        this.prob = 0;
        this.importCount = 0;
        this.statInsts = new Instances("statAttr", PredictInterm.attributes(), 0);
        this.contentInsts = new Instances(contentInstance, 0);
        this.isValid = true;
        setIntermediates(runOldModel, neighborCount, pivotNode, targetNode, startYear, endYear, umlsCuiNameMap, connLit, semTypeMatrixByIntermediate, collInfo, collRef);
        this.importInterm = new ArrayList<>();
    }

    public PredictNetwork(boolean runOldModel, int classifierType, Map<String, Integer> neighborCount, String pivotName, String targetName, int startYear, int endYear,
                          HashMap<String, String> umlsCuiNameMap, HashMap<String, String> umlsNameCuiMap, Connection connLit,
                          HashMap<String, HashMap<String, Integer>> semTypeMatrixByIntermediate,
                          DBCollection collInfo, DBCollection collRef) throws SQLException, IOException {
        setClassifier(runOldModel, classifierType);
        this.startYear = startYear;
        this.endYear = endYear;
        this.pivotNode = new LiteratureNode(pivotName, umlsNameCuiMap.get(pivotName), startYear, endYear, connLit);
        this.targetNode = new LiteratureNode(targetName, umlsNameCuiMap.get(targetName), startYear, endYear, connLit);
        this.prob = 0;
        this.importCount = 0;
        this.statInsts = new Instances("statAttr", PredictInterm.predictAttributes(), 0);
        this.contentInsts = new Instances(contentInstance, 0);
        this.isValid = true;
        setPredictIntermediates(runOldModel, neighborCount, pivotNode, targetNode, startYear, endYear, umlsCuiNameMap, connLit, semTypeMatrixByIntermediate, collInfo, collRef);
        this.importInterm = new ArrayList<>();
    }

    public PredictNetwork(String pivotName, String targetName, int startYear, int endYear,
                          HashMap<String, String> umlsCuiNameMap, HashMap<String, String> umlsNameCuiMap, Connection connLit,
                          DBCollection collection) throws SQLException, IOException {
        setClassifier();
        this.startYear = startYear;
        this.endYear = endYear;
        this.pivotNode = new LiteratureNode(pivotName, umlsNameCuiMap.get(pivotName), startYear, endYear, connLit);
        this.targetNode = new LiteratureNode(targetName, umlsNameCuiMap.get(targetName), startYear, endYear, connLit);
        this.prob = 0;
        this.importCount = 0;
        this.statInsts = new Instances("statAttr", PredictInterm.getAttributes(), 0);
        this.contentInsts = new Instances(contentInstance, 0);
        this.isValid = true;
        setIntermediates(pivotNode, targetNode, startYear, endYear, umlsCuiNameMap, connLit, collection);
        this.importInterm = new ArrayList<>();
    }

    public PredictNetwork(boolean runOldModel, String pivotName, String targetName, Instances insts,
                          Connection connSem, Connection connLit) throws SQLException {
        setClassifier(runOldModel);
        this.pivotNode = new LiteratureNode(pivotName, startYear, endYear, connSem, connLit);
        this.targetNode = new LiteratureNode(targetName, startYear, endYear, connSem, connLit);
        dataUnlabeled = insts;
        dataUnlabeled.setClassIndex(dataUnlabeled.numAttributes() - 1);
    }

    public PredictNetwork(boolean runOldModel, int classifierType,
                          String pivotName, String targetName, Instances insts,
                          Connection connSem, Connection connLit) throws SQLException {
        setClassifier(runOldModel, classifierType);
        this.pivotNode = new LiteratureNode(pivotName, startYear, endYear, connSem, connLit);
        this.targetNode = new LiteratureNode(targetName, startYear, endYear, connSem, connLit);
        dataUnlabeled = insts;
        dataUnlabeled.setClassIndex(dataUnlabeled.numAttributes() - 1);
    }

    public void setDataUnlabeled(Instances dataUnlabeled) {
        this.dataUnlabeled = dataUnlabeled;
    }

    public Instances getDataUnlabeled() {
        return dataUnlabeled;
    }

    private void setIntermediates(boolean runOldModel, Map<String, Integer> neighborCount, LiteratureNode pivotNode, LiteratureNode targetNode, int startYear,
                                  int endYear, HashMap<String, String> umlsCuiNameMap, Connection connLit,
                                  HashMap<String, HashMap<String, Integer>> semTypeMatrixByIntermediate,
                                  DBCollection collInfo, DBCollection collRef) throws SQLException, IOException {
        this.intermediates = new ArrayList<>();
        Set<String> neighborSet = new HashSet<>();
        String sql_ab = "SELECT `o_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and `o_cui` in (SELECT `s_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and `year` between ? and ?) " +
                "and `year` between ? and ?";
        String pivotCui = pivotNode.getCui();
        String targetCui = targetNode.getCui();
        Object[] params_ab = {pivotCui, targetCui, startYear, endYear, startYear, endYear};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result_ab = jdbcHelper.query(connLit, sql_ab, params_ab);

        String sql_bc = "SELECT `s_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and `s_cui` in (SELECT `o_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and `year` between ? and ?) " +
                "and `year` between ? and ?";
        Object[] params_bc = {targetCui, pivotCui, startYear, endYear, startYear, endYear};
        List<Map<String, Object>> result_bc = jdbcHelper.query(connLit, sql_bc, params_bc);

        for (Map row : result_ab) {
            neighborSet.add((String) row.get("neighbor"));
        }
        if (neighborSet.size() < 5) {
            isValid = false;
            return;
        }

        if (runOldModel) {
            for (String neighbor : neighborSet) {
                String neighborName = umlsCuiNameMap.get(neighbor);
                LiteratureNode intermNode = new LiteratureNode(neighborName, neighbor, startYear, endYear, connLit);
                PredictInterm pi = new PredictInterm(pivotNode, intermNode, targetNode);
                pi.setPredicateInfo(result_ab, result_bc, connLit, semTypeMatrixByIntermediate, startYear, endYear);
                if (!pi.getIfABCRelationFiltered()) {
                    statInsts.add(pi.getOldStatInst(neighborCount, umlsCuiNameMap, collInfo, collRef));
                    intermediates.add(pi);
                }
            }
        } else {
            for (String neighbor : neighborSet) {
                String neighborName = umlsCuiNameMap.get(neighbor);
                if (!Stopwords.set.contains(neighborName)) {
                    LiteratureNode intermNode = new LiteratureNode(neighborName, neighbor, startYear, endYear, connLit);
                    PredictInterm pi = new PredictInterm(pivotNode, intermNode, targetNode);
                    pi.setPredicateInfo(result_ab, result_bc, connLit, semTypeMatrixByIntermediate, startYear, endYear);
                    if (!pi.getIfABCRelationFiltered()) {
                        statInsts.add(pi.getStatInst(neighborCount, umlsCuiNameMap, collInfo, collRef));
                        intermediates.add(pi);
                    }
                }
            }
        }
        dataUnlabeled = statInsts;
    }

    private void setPredictIntermediates(boolean runOldModel, Map<String, Integer> neighborCount, LiteratureNode pivotNode, LiteratureNode targetNode, int startYear,
                                         int endYear, HashMap<String, String> umlsCuiNameMap, Connection connLit,
                                         HashMap<String, HashMap<String, Integer>> semTypeMatrixByIntermediate,
                                         DBCollection collInfo, DBCollection collRef) throws SQLException, IOException {
        this.intermediates = new ArrayList<>();
        Set<String> neighborSet = new HashSet<>();
        String sql_ab = "SELECT `o_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and `o_cui` in (SELECT `s_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and `year` between ? and ?) " +
                "and `year` between ? and ?";
        String pivotCui = pivotNode.getCui();
        String targetCui = targetNode.getCui();
        Object[] params_ab = {pivotCui, targetCui, startYear, endYear, startYear, endYear};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result_ab = jdbcHelper.query(connLit, sql_ab, params_ab);

        String sql_bc = "SELECT `s_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and `s_cui` in (SELECT `o_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and `year` between ? and ?) " +
                "and `year` between ? and ?";
        Object[] params_bc = {targetCui, pivotCui, startYear, endYear, startYear, endYear};
        List<Map<String, Object>> result_bc = jdbcHelper.query(connLit, sql_bc, params_bc);

        for (Map row : result_ab) {
            neighborSet.add((String) row.get("neighbor"));
        }
        if (neighborSet.size() < 5) {
            isValid = false;
            return;
        }

        if (runOldModel) {
            for (String neighbor : neighborSet) {
                String neighborName = umlsCuiNameMap.get(neighbor);
                LiteratureNode intermNode = new LiteratureNode(neighborName, neighbor, startYear, endYear, connLit);
                PredictInterm pi = new PredictInterm(pivotNode, intermNode, targetNode);
                pi.setPredicateInfo(result_ab, result_bc, connLit, semTypeMatrixByIntermediate, startYear, endYear);
                if (!pi.getIfABCRelationFiltered()) {
                    statInsts.add(pi.getOldStatInst(neighborCount, umlsCuiNameMap, collInfo, collRef));
                    intermediates.add(pi);
                }
            }
        } else {
            for (String neighbor : neighborSet) {
                String neighborName = umlsCuiNameMap.get(neighbor);
                if (!Stopwords.set.contains(neighborName)) {
                    LiteratureNode intermNode = new LiteratureNode(neighborName, neighbor, startYear, endYear, connLit);
                    PredictInterm pi = new PredictInterm(pivotNode, intermNode, targetNode);
                    pi.setPredicateInfo(result_ab, result_bc, connLit, semTypeMatrixByIntermediate, startYear, endYear);
                    if (!pi.getIfABCRelationFiltered()) {
                        statInsts.add(pi.getPredictStatInst(neighborCount, umlsCuiNameMap, collInfo, collRef));
                        intermediates.add(pi);
                    }
                }
            }
        }
        dataUnlabeled = statInsts;
    }

    private void setIntermediates(LiteratureNode pivotNode, LiteratureNode targetNode, int startYear,
                                  int endYear, HashMap<String, String> umlsCuiNameMap, Connection connLit,
                                  DBCollection collection) throws SQLException, IOException {
        this.intermediates = new ArrayList<>();
        Set<String> neighborSet = new HashSet<>();
        String sql_ab = "SELECT `o_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and `o_cui` in (SELECT `s_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and `year` between ? and ?) " +
                "and `year` between ? and ?";
        String pivotCui = pivotNode.getCui();
        String targetCui = targetNode.getCui();
        Object[] params_ab = {pivotCui, targetCui, startYear, endYear, startYear, endYear};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result_ab = jdbcHelper.query(connLit, sql_ab, params_ab);

        String sql_bc = "SELECT `s_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and `s_cui` in (SELECT `o_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and `year` between ? and ?) " +
                "and `year` between ? and ?";
        Object[] params_bc = {targetCui, pivotCui, startYear, endYear, startYear, endYear};
        List<Map<String, Object>> result_bc = jdbcHelper.query(connLit, sql_bc, params_bc);

        for (Map row : result_ab) {
            neighborSet.add((String) row.get("neighbor"));
        }
        if (neighborSet.size() < 5) {
            isValid = false;
            return;
        }

        for (String neighbor : neighborSet) {
            String neighborName = umlsCuiNameMap.get(neighbor);
            if (!Stopwords.set.contains(neighborName)) {
                LiteratureNode intermNode = new LiteratureNode(neighborName, neighbor, startYear, endYear, connLit);
                PredictInterm pi = new PredictInterm(pivotNode, intermNode, targetNode);
                pi.setPredicateInfo(result_ab, result_bc, collection);
            }
        }
    }

    private void setIntermediates(LiteratureNode pivotNode, LiteratureNode targetNode, int startYear,
                                  int endYear, HashMap<String, String> umlsCuiNameMap, Connection connLit,
                                  HashMap<String, HashMap<String, Integer>> semTypeMatrixByIntermediate) throws SQLException, IOException {
        this.intermediates = new ArrayList<>();
        Set<String> neighborSet = new HashSet<>();
        String sql_ab = "SELECT `o_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and `o_cui` in (SELECT `s_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and `year` between ? and ?) " +
                "and `year` between ? and ?";
        String pivotCui = pivotNode.getCui();
        String targetCui = targetNode.getCui();
        Object[] params_ab = {pivotCui, targetCui, startYear, endYear, startYear, endYear};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result_ab = jdbcHelper.query(connLit, sql_ab, params_ab);

        String sql_bc = "SELECT `s_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and `s_cui` in (SELECT `o_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and `year` between ? and ?) " +
                "and `year` between ? and ?";
        Object[] params_bc = {targetCui, pivotCui, startYear, endYear, startYear, endYear};
        List<Map<String, Object>> result_bc = jdbcHelper.query(connLit, sql_bc, params_bc);

        for (Map row : result_ab) {
            neighborSet.add((String) row.get("neighbor"));
        }
        if (neighborSet.size() < 5) {
            isValid = false;
            return;
        }
        for (String neighbor : neighborSet) {
            String neighborName = umlsCuiNameMap.get(neighbor);
            LiteratureNode intermNode = new LiteratureNode(neighborName, neighbor, startYear, endYear, connLit);
            PredictInterm pi = new PredictInterm(pivotNode, intermNode, targetNode);
            pi.setPredicateInfo(result_ab, result_bc, connLit, semTypeMatrixByIntermediate, startYear, endYear);
            if (!pi.getIfABCRelationFiltered()) {
                statInsts.add(pi.getStatInst(umlsCuiNameMap, connLit));
                intermediates.add(pi);
            }
        }
        dataUnlabeled = statInsts;
    }

    public ArrayList<PredictInterm> getIntermediates() {
        return intermediates;
    }

    public double getAMW(List<PredicationCountInfo> predicationCounts) {
        List<PredicationCountInfo> predicationCountsFiltered = predicationCounts.stream()
                .filter(e -> (e.getDrug().equals(pivotNode.getName())
                        && e.getDisease().equals(targetNode.getName())))
                .collect(Collectors.toList());
        double result = 0;
        for (PredicationCountInfo info : predicationCountsFiltered) {
            double minVal = Math.min(info.getCount_ab(), info.getCount_bc());
            result += minVal;
        }
        return result / dataUnlabeled.numInstances();
    }

    public double getLTC() {
        return dataUnlabeled.numInstances();
    }

    public double getIntermCount() {
        return dataUnlabeled.numInstances();
    }

    public double getSEESPatternProb() {
        double totalAbE = 0;
        double totalBcE = 0;
        double totalAbS = 0;
        double totalBcS = 0;
        for (Instance inst : dataUnlabeled) {
            double abE = inst.value(dataUnlabeled.attribute(Names.eRatioAB));
            double abS = inst.value(dataUnlabeled.attribute(Names.sRatioAB));
            double bcE = inst.value(dataUnlabeled.attribute(Names.eRatioBC));
            double bcS = inst.value(dataUnlabeled.attribute(Names.sRatioBC));
            totalAbE += abE;
            totalBcE += bcE;
            totalAbS += abS;
            totalBcS += bcS;
        }
        double eSRatio = (totalAbE == 0 && totalBcS == 0) ? 0d :
                (Math.min(totalAbE, totalBcS) / Math.max(totalAbE, totalBcS));
        double sERatio = (totalAbS == 0 && totalBcE == 0) ? 0d :
                (Math.min(totalAbS, totalBcE) / Math.max(totalAbS, totalBcE));
        return Math.max(eSRatio, sERatio);
    }

    public double getExpectedProb() {
        return prob;
    }

    public double getNormalizedExpectedProb() {
        return prob / dataUnlabeled.numInstances();
    }

    public int getImportCount() {
        return importCount;
    }

    public void predictDistribution() {
        for (int i = 0; i < dataUnlabeled.numInstances(); i++) {
            Instance inst = dataUnlabeled.instance(i);
            try {
                double[] probDistribution = classifier.distributionForInstance(inst);
                prob += probDistribution[0];
                if (probDistribution[0] > probDistribution[1]) {
                    importCount++;
                    importIndexes.add(i);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void predictInterm() {
        for (int i = 0; i < dataUnlabeled.numInstances(); i++) {
            Instance inst = dataUnlabeled.instance(i);
            String intermCui = intermediates.get(i).getIntermCui();
            try {
                double[] probDistribution = classifier.distributionForInstance(inst);
                prob += probDistribution[0];
                if (probDistribution[0] > probDistribution[1]) {
                    importCount++;
                    importInterm.add(intermCui);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Instances getInst() {
        return dataUnlabeled;
    }

    public Instances getStatInst() {
        return statInsts;
    }

    public Instances takeOffSemanticGroupInsts(Instances data) {
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_ACTI).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_ANAT).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_CHEM).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_CONC).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_DEVI).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_DISO).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_GENE).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_GEOG).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_LIVB).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_OBJC).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_OCCU).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_ORGA).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_PHEN).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_PHYS).index());
        data.deleteAttributeAt(data.attribute(Names.semanticGroupB_PROC).index());
        return data;
    }

    public void setAutoAttrs() {
        Remove remove = new Remove();
        int[] removeAttr = new int[attrs.size()];
        int j = 0;
        for (int i = 0; i < dataUnlabeled.numAttributes(); i++) {
            for (String attr : attrs) {
                if (attr.equals(dataUnlabeled.attribute(i).toString())) {
                    removeAttr[j] = i;
                    j++;
                    break;
                }
            }
        }
        remove.setInvertSelection(true);
        remove.setAttributeIndicesArray(removeAttr);
        try {
            remove.setInputFormat(dataUnlabeled);
            dataUnlabeled = Filter.useFilter(dataUnlabeled, remove);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public boolean isValid() {
        return isValid;
    }

    private void setClassifier() {
        EvalDataAttr evalDataAttr = new EvalDataAttr();
        Instances data = evalDataAttr.readAttrFile(EvalStat.filePath);
        data.setClassIndex(data.numAttributes() - 1);

//        AbstractClassifier trainer = Classifier.logisticRegression();
//        AbstractClassifier trainer = Classifier.naiveBayes();
//        AbstractClassifier trainer = Classifier.randomForest();
        AbstractClassifier trainer = Classifier.svm();

        attrs = new ArrayList<>();
        for (int i = 0; i < data.numAttributes(); i++) {
            attrs.add(data.attribute(i).toString());
        }
        try {
            trainer.buildClassifier(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        classifier = trainer;
    }

    private void setClassifier(boolean runOldModel, int classifierType) {
        EvalDataAttr evalDataAttr = new EvalDataAttr();
        Instances data;
        if (runOldModel) {
            data = evalDataAttr.readAttrFile(EvalOldStat.filePath);
        } else {
            data = evalDataAttr.readAttrFile(EvalStat.filePath);
        }
        data.setClassIndex(data.numAttributes() - 1);

        AbstractClassifier trainer = null;
        switch (classifierType) {
            case 0:
                trainer = Classifier.logisticRegression();
                break;
            case 1:
                trainer = Classifier.naiveBayes();
                break;
            case 2:
                trainer = Classifier.randomForest();
                break;
            case 3:
                trainer = Classifier.svm();
                break;
        }

        attrs = new ArrayList<>();
        for (int i = 0; i < data.numAttributes(); i++) {
            attrs.add(data.attribute(i).toString());
        }
        try {
            trainer.buildClassifier(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        classifier = trainer;
    }

    private void setClassifier(boolean runOldModel) {
        EvalDataAttr evalDataAttr = new EvalDataAttr();
        Instances data;
        if (runOldModel) {
            data = evalDataAttr.readAttrFile(EvalOldStat.filePath);
        } else {
            data = evalDataAttr.readAttrFile(EvalStat.filePath);
        }
        data.setClassIndex(data.numAttributes() - 1);
        AbstractClassifier trainer = Classifier.randomForest();
        attrs = new ArrayList<>();
        for (int i = 0; i < data.numAttributes(); i++) {
            attrs.add(data.attribute(i).toString());
        }
        try {
            trainer.buildClassifier(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        classifier = trainer;
    }

    public List<String> getImportInterm() {
        return importInterm;
    }
}
