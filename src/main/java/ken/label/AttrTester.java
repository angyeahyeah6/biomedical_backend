package ken.label;

import ken.model.Classifier;
import ken.util.DbConnector;
import ken.util.JDBCHelper;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AttrTester {

    public static void main(String[] args) {
        AttrTester attrTester = new AttrTester();
        //TODO set classifier
        AbstractClassifier classifier = Classifier.getSVM();
        try {
            //TODO set data set
//            Instances data = readAttrFile("output/attr/stat.arff");
//            data.setClassIndex(data.numAttributes() - 1);
//            attrTester.evaluateModel(classifier, data);
//            attrTester.createContentArffFile();
            attrTester.createStatArffFile();
            attrTester.mergeInst();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
        setting training case from label_system
        default is case whose paths are all labeled
     */
    public ArrayList<LabeledNetwork> getPredicateGraph() {
        String sql = "SELECT id,a_name,c_name,year FROM prediction_view where count_b=label_count";
        List<Map<String, Object>> result = JDBCHelper.query(DbConnector.LABELSYS, sql);
        ArrayList<LabeledNetwork> pgs = new ArrayList<>();
        final boolean important = false;
        int count = 0;
        for (Map<String, Object> row : result) {
            count += 1;
            if(count % 20 == 0){
                System.out.println(count);
            }
            int link_id = (int) row.get("id");
            int year = (int) row.get("year");
            String a_name = (String) row.get("a_name");
            String c_name = (String) row.get("c_name");
            System.out.println(a_name + ", " + c_name + ", " + year);
            pgs.add(new LabeledNetwork(a_name, c_name, year, link_id, important));
        }
        return pgs;
    }

    /*
        merge content instances and statistical instances
     */
    public void mergeInst() throws Exception {
        Instances content = readAttrFile("output/attr/content.arff");
        Instances stat = readAttrFile("output/attr/stat.arff");
        Remove remove = new Remove();
        remove.setAttributeIndices("first");
        remove.setInputFormat(content);
        Instances content_new = Filter.useFilter(content, remove);
        Instances combined = Instances.mergeInstances(content_new, stat);
        combined.setRelationName("combined attributes");
        writeAttr("output/attr/combined.arff", combined);
    }

    public static Instances readAttrFile(String path) {
        Instances data = null;
        try (BufferedReader reader = new BufferedReader(
                new FileReader(path))) {
            data = new Instances(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public void createContentArffFile() throws Exception {
        ArrayList<LabeledNetwork> pgs = getPredicateGraph();
        ArrayList<Attribute> attributes = new ArrayList<>(2);
        attributes.add(new Attribute("predicate", (List<String>) null));
        ArrayList<String> attrVals = new ArrayList<>(2);
        attrVals.add("important");
        attrVals.add("unimportant");
        attributes.add(new Attribute("label", attrVals));
        Instances dataSet = new Instances("contentAttr", attributes, 0);
        dataSet.setClassIndex(dataSet.numAttributes() - 1);
        for (LabeledNetwork pg : pgs) {
            for (LabeledInterm interm : pg.getIntermediate()) {
                DenseInstance instance = new DenseInstance(dataSet.numAttributes());
                instance.setDataset(dataSet);
                String text = interm.getABrelationStr() + interm.getBCrelationStr();
                instance.setValue(0, text);
                String label = (interm.getImportance() == 1) ? "unimportant" : "important";
                instance.setValue(1, label);
                dataSet.add(instance);
            }
        }
        StringToWordVector filter = new StringToWordVector();
        filter.setInputFormat(dataSet);
        filter.setOutputWordCounts(true);
        Instances inst_new = Filter.useFilter(dataSet, filter);
        writeAttr("output/attr/content.arff", inst_new);
    }

    public void createStatArffFile() throws IOException {
        ArrayList<LabeledNetwork> pgs = getPredicateGraph();
        ArrayList<Attribute> attributes = LabeledInterm.getStatAttributes();
        Instances dataSet = new Instances("statAttr", attributes, 0);
        dataSet.setClassIndex(dataSet.numAttributes() - 1);
        for (LabeledNetwork pg : pgs) {
            for (LabeledInterm interm : pg.getIntermediate()) {
                Instance inst = interm.getStatInst();
                dataSet.add(inst);
            }
        }
        writeAttr("output/attr/new_stat.arff", dataSet);
    }


    private void writeAttr(String path, Instances data) throws IOException {
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(path));
        saver.writeBatch();
    }


    public void evaluateModel(AbstractClassifier classifier, Instances data) throws Exception {
        Evaluation eval = new Evaluation(data);
        //10-folds cross validation times, default is 30
        int iter = 30;
        double imp_avgPrecision = 0.0, imp_avgRecall = 0.0, imp_avgFval = 0.0;
        double unimp_avgPrecision = 0.0, unimp_avgRecall = 0.0, unimp_avgFval = 0.0;
        double overall_avgPrecision = 0.0, overall_avgRecall = 0.0, overall_avgFval = 0.0;
        for (int i = 0; i < iter; i++) {
            eval.crossValidateModel(classifier, data, 10, new Random(i));
            imp_avgPrecision += eval.precision(0);
            imp_avgRecall += eval.recall(0);
            imp_avgFval += eval.fMeasure(0);
            unimp_avgPrecision += eval.precision(1);
            unimp_avgRecall += eval.recall(1);
            unimp_avgFval += eval.fMeasure(1);
            overall_avgPrecision += eval.weightedPrecision();
            overall_avgRecall += eval.weightedRecall();
            overall_avgFval += eval.weightedFMeasure();
        }
        imp_avgPrecision /= iter;
        imp_avgRecall /= iter;
        imp_avgFval /= iter;
        unimp_avgPrecision /= iter;
        unimp_avgRecall /= iter;
        unimp_avgFval /= iter;
        overall_avgPrecision /= iter;
        overall_avgRecall /= iter;
        overall_avgFval /= iter;

        System.out.format("important: %.3f/%.3f/%.3f\n", imp_avgPrecision, imp_avgRecall, imp_avgFval);
        System.out.format("unimportant: %.3f/%.3f/%.3f\n", unimp_avgPrecision, unimp_avgRecall, unimp_avgFval);
        System.out.format("overall: %.3f/%.3f/%.3f\n", overall_avgPrecision, overall_avgRecall, overall_avgFval);
    }

    public void evalRemoveOnlyOneFeature() throws Exception {
        Instances data = readAttrFile("output/attr/stat.arff");
        data.setClassIndex(data.numAttributes() - 1);
        AbstractClassifier classifier = Classifier.getSVM();
        for (int j = 0; j < data.numAttributes() - 1; j++) {
            System.out.println(data.attribute(j));
            Remove remove = new Remove();
            remove.setAttributeIndices(String.format("%d", j + 1));
            remove.setInputFormat(data);
            data = Filter.useFilter(data, remove);
            evaluateModel(classifier, data);
            //reload data set because removing one feature previously
            data = readAttrFile("output/attr/stat.arff");
            data.setClassIndex(data.numAttributes() - 1);
        }
    }
}

