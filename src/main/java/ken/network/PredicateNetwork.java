package ken.network;

import ken.label.AttrTester;
import ken.model.Classifier;
import ken.node.LiteratureNode;
import ken.prepare.MeshConceptObject;
import ken.util.DbConnector;
import ken.util.JDBCHelper;
import ken.util.Utils;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.util.*;

/**
 * Created by lbj23k on 2017/6/1.
 */
public class PredicateNetwork {
    private static AbstractClassifier classifier;
    private static HashMap<String, LiteratureNode> allNodes;
    private static Instances contentInstance;
    private LiteratureNode pivotNode;
    private LiteratureNode targetNode;
    private int startYear;
    private int endYear;
    private ArrayList<PredicateInterm> intermediates;
    private Instances dataUnlabeled;
    private Instances contentInsts;
    private Instances statInsts;
    private double prob = 0;
    private int importCount;

    static {
        if (classifier == null) {
            setClassifier();
        }
        if (contentInstance == null) {
            contentInstance = Utils.readAttrFile("output/attr/content.arff");
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

    public PredicateNetwork(String pivotName, String targetName, int startYear, int endYear) {
        allNodes = LiteratureNode.getAllNodes(startYear, endYear);
        this.startYear = startYear;
        this.endYear = endYear;
        this.pivotNode = allNodes.get(pivotName);
        this.targetNode = allNodes.get(targetName);
        prob = 0;
        importCount = 0;
        statInsts = new Instances("statAttr", PredicateInterm.getAttributes(), 0);
        contentInsts = new Instances(contentInstance, 0);
        setIntermediates();
    }

    public PredicateNetwork(String pivotName, String targetName, Instances insts) {
        allNodes = LiteratureNode.getAllNodes(startYear, endYear);
        this.pivotNode = allNodes.get(pivotName);
        this.targetNode = allNodes.get(targetName);
        dataUnlabeled = insts;
//        System.out.println(insts.get(0).numAttributes());
        dataUnlabeled.setClassIndex(dataUnlabeled.numAttributes() - 1);
    }

    private void setIntermediates() {
        this.intermediates = new ArrayList<>();
        Set<Integer> neighborSet = new HashSet<>();
        String sql_ab = "SELECT o_mesh_id as neighbor, predicate, pmid, year FROM mesh_predication_aggregate " +
                "WHERE s_mesh_id=? and o_mesh_id in " +
                "(SELECT s_mesh_id FROM mesh_predication_aggregate WHERE o_mesh_id=? and year between ? and ?) " +
                "and year between ? and ?";
        Object[] params_ab = {pivotNode.getMeshId(), targetNode.getMeshId(), startYear, endYear, startYear, endYear};
        List<Map<String, Object>> result_ab = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql_ab, params_ab);

        String sql_bc = "SELECT s_mesh_id as neighbor, predicate, pmid, year FROM mesh_predication_aggregate " +
                "WHERE o_mesh_id=? and s_mesh_id in " +
                "(SELECT o_mesh_id FROM mesh_predication_aggregate WHERE s_mesh_id=? and year between ? and ?) " +
                "and year between ? and ?";
        Object[] params_bc = {targetNode.getMeshId(), pivotNode.getMeshId(), startYear, endYear, startYear, endYear};
        List<Map<String, Object>> result_bc = JDBCHelper.query(DbConnector.LITERATURE_YEAR, sql_bc, params_bc);
        for (Map row : result_ab) {
            neighborSet.add((int) row.get("neighbor"));
        }
        for (int neighbor : neighborSet) {
            String neighborName = MeshConceptObject.getMeshIdNameMap().get(neighbor);
            LiteratureNode intermNode = allNodes.get(neighborName);
            PredicateInterm pi = new PredicateInterm(pivotNode, intermNode, targetNode);
            pi.setPredicateInfo(result_ab, result_bc);
            statInsts.add(pi.getStatInst());
            contentInsts.add(pi.getContentInst());
            intermediates.add(pi);
        }
        mergeInst();
    }

    public ArrayList<PredicateInterm> getIntermediates() {
        return intermediates;
    }

    /*
        these feature index derive from instance file
        in fact, they can be determined automatically by checking the feature name.... forgive me >__<
     */
    public double getLTCSumAw() {
        double result = 0;
        for (Instance inst : dataUnlabeled) {
//            System.out.println(inst.attribute(19));
//            double plus = inst.value(40) + inst.value(41);
            double plus = inst.value(19) + inst.value(20);
            result += plus / 2;
        }
        return result;
    }

    public double getAMW() {
        double result = 0;
        for (Instance inst : dataUnlabeled) {
            double minVal = Math.min(inst.value(19), inst.value(20));
            result += minVal;
        }
        return result / dataUnlabeled.numInstances();
    }

    public double getLTC() {
        return dataUnlabeled.numInstances();
    }

    public double getLTCSumMw() {
        double result = 0;
        for (Instance inst : dataUnlabeled) {
            double minVal = Math.min(inst.value(19), inst.value(20));
            result += minVal;
        }
        return result;
    }


    public double getCompNMSsumAw() {
        double result = 0;
        for (Instance inst : dataUnlabeled) {
            double nmsAB = Math.max(inst.value(29), inst.value(31));
            double nmsBC = Math.max(inst.value(30), inst.value(32));
            double plus = nmsAB + nmsBC;
            result += plus / 2;
        }
        return result;
    }

    public double getCompNMSsumMw() {
        double result = 0;
        for (Instance inst : dataUnlabeled) {
            double nmsAB = Math.max(inst.value(29), inst.value(31));
            double nmsBC = Math.max(inst.value(30), inst.value(32));
            double minVal = Math.min(nmsAB, nmsBC);
            result += minVal;
        }
        return result;
    }


    public double getLiterNMSsumAw() {
        double result = 0;
        for (Instance inst : dataUnlabeled) {
            double plus = inst.value(29) + inst.value(30);
            result += plus / 2;
        }
        return result;
    }

    public double getLiterNMSsumMw() {
        double result = 0;
        for (Instance inst : dataUnlabeled) {
            double minVal = Math.min(inst.value(29), inst.value(30));
            result += minVal;
        }
        return result;
    }


    public double getIntermCount() {
        return dataUnlabeled.numInstances();
    }

    public double getExpdectProb() {
        return prob;
    }

    public int getImportCount() {
        return importCount;
    }

    public void predictInterm() {
        for (int i = 0; i < dataUnlabeled.numInstances(); i++) {
            Instance inst = dataUnlabeled.instance(i);
//            String intermName = intermediates.get(i).getName();
            try {
                double[] probDistribution = classifier.distributionForInstance(inst);
                prob += probDistribution[0];
                if (probDistribution[0] > probDistribution[1]) {
                    importCount++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Instances getInst() {
        return dataUnlabeled;
    }

    private void mergeInst(){
        if(statInsts != null && contentInsts!= null){
            dataUnlabeled = Instances.mergeInstances(contentInsts, statInsts);
            dataUnlabeled.setClassIndex(dataUnlabeled.numAttributes() - 1);
        }
    }

    public Instances getStatInst() {
        return statInsts;
    }
    public void setOnlyStatInsts() {
        Remove remove = new Remove();
        int[] removeAttr = new int[19];
        for (int i = 0; i < 19; i++) {
            removeAttr[i] = i;
        }
        remove.setAttributeIndicesArray(removeAttr);
        try {
            remove.setInputFormat(dataUnlabeled);
            dataUnlabeled = Filter.useFilter(dataUnlabeled, remove);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void setOnlyConceptInsts() {
        setOnlyStatInsts();
        Remove remove = new Remove();
        int[] removeAttr = new int[4];
        removeAttr[0] = 14;
        removeAttr[1] = 15;
        removeAttr[2] = 16;
        removeAttr[3] = 19;
        remove.setInvertSelection(true);
        remove.setAttributeIndicesArray(removeAttr);
        try {
            remove.setInputFormat(dataUnlabeled);
            dataUnlabeled = Filter.useFilter(dataUnlabeled, remove);
            int j = 0;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void setOnlyPredicationInsts() {
        setOnlyStatInsts();
        Remove remove = new Remove();
        int[] removeAttr = new int[3];
        removeAttr[0] = 14;
        removeAttr[1] = 15;
        removeAttr[2] = 16;
        remove.setAttributeIndicesArray(removeAttr);
        try {
            remove.setInputFormat(dataUnlabeled);
            dataUnlabeled = Filter.useFilter(dataUnlabeled, remove);
            int j = 0;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void setFeatureSelectionInsts() {
        setOnlyStatInsts();
        Remove remove = new Remove();
        int[] removeAttr = new int[8];
        removeAttr[0] = 5;
        removeAttr[1] = 8;
        removeAttr[2] = 9;
        removeAttr[3] = 10;
        removeAttr[4] = 11;
        removeAttr[5] = 12;
        removeAttr[6] = 16;
        removeAttr[7] = 19;
        remove.setInvertSelection(true);
        remove.setAttributeIndicesArray(removeAttr);
        try {
            remove.setInputFormat(dataUnlabeled);
            dataUnlabeled = Filter.useFilter(dataUnlabeled, remove);
            int j = 0;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void setOnlyContentInsts() {
        Remove remove = new Remove();
        int[] removeAttr = new int[20];
        for (int i = 0; i < 19; i++) {
            removeAttr[i] = i;
        }
        removeAttr[19] = dataUnlabeled.numAttributes() - 1;
        remove.setInvertSelection(true);
        remove.setAttributeIndicesArray(removeAttr);
        try {
            remove.setInputFormat(dataUnlabeled);
            dataUnlabeled = Filter.useFilter(dataUnlabeled, remove);
            int j = 0;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    public List<Double> getListNetFeatures() {
        List<Double> featureLs = new ArrayList<>();
        featureLs.add(getLiterNMSsumAw());
        featureLs.add(getLiterNMSsumMw());
        featureLs.add(getCompNMSsumAw());
        featureLs.add(getCompNMSsumMw());
        featureLs.add(getLiterNMSsumAw());
        featureLs.add(getLiterNMSsumMw());
        featureLs.add(getIntermCount());
        return featureLs;
    }

    private double getSumMw(int index1, int index2) {
        double result = 0;
        for (Instance inst : dataUnlabeled) {
            double minVal = Math.min(inst.value(index1), inst.value(index2));
            result += minVal;
        }
        return result;
    }

    private double getSumAw(int index1, int index2) {
        double result = 0;
        for (Instance inst : dataUnlabeled) {
            double plus = inst.value(index1) + inst.value(index2);
            result += plus / 2;
        }
        return result;
    }

    public List<Double> getAllFeatures() {
        List<Double> featureLs = new ArrayList<>();
        featureLs.add(getSumAw(19, 20));
        featureLs.add(getSumMw(19, 20));
        featureLs.add(getSumAw(29, 30));
        featureLs.add(getSumMw(29, 30));
        featureLs.add(getLTC());
        return featureLs;
    }

    private static void setClassifier() {
        //TODO setting training data
//        Instances data = AttrTester.readAttrFile("output/attr/cfsSubset.arff");
        Instances data = AttrTester.readAttrFile("output/attr/stat.arff");
        data.setClassIndex(data.numAttributes() - 1);
        //TODO setting classifier
        AbstractClassifier trainer = Classifier.getSVM();
        //AbstractClassifier trainer = Classifier.getNaiveBayes();
        try {
            trainer.buildClassifier(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
        classifier = trainer;
    }
}
