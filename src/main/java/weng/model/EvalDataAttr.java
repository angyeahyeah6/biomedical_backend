package weng.model;

import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weng.feature.SemanticType;
import weng.label.LabeledInfo;
import weng.label.LabeledInterm;
import weng.label.LabeledNetwork;
import weng.prepare.UmlsConcept;
import weng.util.JDBCHelper;
import weng.util.file.evaluationData.Concept;
import weng.util.file.evaluationData.EvalContent;
import weng.util.file.evaluationData.EvalStat;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class EvalDataAttr {

    public EvalDataAttr() {
    }

    public HashMap<String, AbstractClassifier> classifiers() {
        HashMap<String, AbstractClassifier> classifierByName = new HashMap<>();
        AbstractClassifier classifier = Classifier.logisticRegression();
        classifierByName.put("LogisticRegression", classifier);
        classifier = Classifier.naiveBayes();
        classifierByName.put("naiveBayes", classifier);
        classifier = Classifier.randomForest();
        classifierByName.put("RandomForest", classifier);
        classifier = Classifier.svm();
        classifierByName.put("SVM", classifier);
        return classifierByName;
    }

    public ArrayList<LabeledNetwork> getPredicateGraph(Connection connSem, Connection connLit, Connection connLSUMLS, Connection connBioRel) throws SQLException, IOException {

        // TODO: 21 cases, for predicting "Sildenafil citrate"
//        String sql = "SELECT `id`, `a_name`, `c_name`, `year` FROM `prediction_view` WHERE `count_b`=`label_count` AND `flag`=0 AND `id`<>13 AND `id`<>17 AND `id`<>25 AND `id`<>27;"; // no Metformin, no NO, no sibutramine, no miltefosine, no Sildenafil citrate

        // TODO: 21 cases, for predicting "Finasteride"
//        String sql = "SELECT `id`, `a_name`, `c_name`, `year` FROM `prediction_view` WHERE `count_b`=`label_count` AND `flag`=0 AND `id`<>5 AND `id`<>17 AND `id`<>25 AND `id`<>27;"; // no Metformin, no NO, no sibutramine, no miltefosine, no Finasteride

        // TODO: 21 cases, for predicting "Fluoxetine"
//        String sql = "SELECT `id`, `a_name`, `c_name`, `year` FROM `prediction_view` WHERE `count_b`=`label_count` AND `flag`=0 AND `id`<>6 AND `id`<>17 AND `id`<>25 AND `id`<>27;"; // no Metformin, no NO, no sibutramine, no miltefosine, no Fluoxetine

        String sql = "SELECT `id`, `a_name`, `c_name`, `year` FROM `prediction_view` WHERE `count_b`=`label_count` AND `flag`=0 AND `id`<>17;"; // 24 cases (no Metformin, no NO)
//        String sql = "SELECT `id`, `a_name`, `c_name`, `year` FROM `prediction_view` WHERE `count_b`=`label_count` AND `flag`=0;"; // Total cases (25 cases, no NO)

        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result = jdbcHelper.query(connLSUMLS, sql);
        ArrayList<LabeledNetwork> pgs = new ArrayList<>();
        for (Map<String, Object> row : result) {
            int link_id = (int) row.get("id");
            int year = (int) row.get("year");
            String a_name = (String) row.get("a_name");
            String c_name = (String) row.get("c_name");
            System.out.println("case: " + a_name + ", " + c_name + ", " + year);
            HashMap<String, HashSet<String>> semGroupsByIntermediate = getSemGroupsByIntermediate(a_name, c_name, connSem);
            HashMap<String, HashMap<String, Integer>> semGroupMatrixByIntermediate = getSemGroupMatrixByIntermediate(semGroupsByIntermediate);
            pgs.add(new LabeledNetwork(a_name, c_name, year, link_id, connSem, connLit, connLSUMLS, connBioRel, semGroupMatrixByIntermediate));
        }
        return pgs;
    }

    public HashMap<String, HashMap<String, Integer>> getSemGroupMatrixByIntermediate(HashMap<String, HashSet<String>> semGroupsByIntermediate) {
        HashMap<String, HashMap<String, Integer>> semGroupMatrixByIntermediate = new HashMap<>();
        for (String interm : semGroupsByIntermediate.keySet()) {
            HashSet<String> semGroups = semGroupsByIntermediate.get(interm);
            SemanticType semanticType = new SemanticType();
            HashMap<String, Integer> semGroupAppearedList = semanticType.initSemGroupAppearedList();
            for (String semType : semGroups) {
                semGroupAppearedList.put(semType, 1);
            }
            semGroupMatrixByIntermediate.put(interm, semGroupAppearedList);
        }
        return semGroupMatrixByIntermediate;
    }

    public HashMap<String, HashSet<String>> getSemGroupsByIntermediate(String a_name, String c_name, Connection connSem) {
        HashMap<String, HashSet<String>> semGroupsByIntermediate = new HashMap<>();
        String sqlSelSemType = "select `b_name`, `sg`.`SEMGROUP` from `label_system_umls`.`prediction_view` AS `pv` " +
                "inner join `label_system_umls`.`intermediate` as `in` on `pv`.`id`=`in`.`link_id` " +
                "inner join `semmed_ver26`.`concept` AS `con` on `con`.`PREFERRED_NAME`=`in`.`b_name` " +
                "inner join `semmed_ver26`.`concept_semtype` AS `cs` on `cs`.`CONCEPT_ID`=`con`.`CONCEPT_ID` " +
                "inner join `semmed_ver26`.`semanticType` AS `st` on `cs`.`SEMTYPE`=`st`.`SEMTYPE` " +
                "inner join `semmed_ver26`.`semanticGroup` AS `sg` on `st`.`SEMTYPE_ID`=`sg`.`SEMTYPE_ID` " +
                "where `pv`.`count_b`=`pv`.`label_count` AND `flag`=0 AND `con`.`TYPE`=\"META\" AND `pv`.`a_name`=? AND `pv`.`c_name`=?;";
        Object[] params = {a_name, c_name};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> ls = jdbcHelper.query(connSem, sqlSelSemType, params);
        for (Map<String, Object> row : ls) {
            String intermName = (String) row.get("b_name");
            String semGroup = (String) row.get("SEMGROUP");
            HashSet<String> semGroups = new HashSet<>();
            if (semGroupsByIntermediate.keySet().contains(intermName)) {
                semGroups = semGroupsByIntermediate.get(intermName);
            }
            semGroups.add(semGroup);
            semGroupsByIntermediate.put(intermName, semGroups);
        }
        return semGroupsByIntermediate;
    }

    public Instances readAttrFile(String path) {
        Instances data = null;
        try (BufferedReader reader = new BufferedReader(
                new FileReader(path))) {
            data = new Instances(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    private void writeAttr(String path, Instances data) throws IOException {
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(path));
        saver.writeBatch();
    }

    public void contentFile(ArrayList<LabeledNetwork> pgs) throws Exception {
        System.out.println("[contentFile] Start");

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
                ArrayList<LabeledInfo> ABrelation = interm.getABrelation();
                ArrayList<LabeledInfo> BCrelation = interm.getBCrelation();
                String text = interm.getRelationStr(ABrelation) + interm.getRelationStr(BCrelation);
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
        writeAttr(EvalContent.filePath, inst_new);

        System.out.println("[contentFile] Finish");
    }

    public void statFile(ArrayList<LabeledNetwork> pgs, Connection connSem, Connection connLit) throws IOException, SQLException {
        System.out.println("[statFile] Start");

        ArrayList<Attribute> attributes = LabeledInterm.getStatAttributes();
        Instances dataSet = new Instances("statAttr", attributes, 0);
        dataSet.setClassIndex(dataSet.numAttributes() - 1);
        HashMap<String, String> umlsCuiNameMap = UmlsConcept.getUmlsCuiNameMap(connSem);
        List<String> abcPaths = new ArrayList<>();
        for (LabeledNetwork pg : pgs) {
            for (LabeledInterm interm : pg.getIntermediate()) {
                abcPaths = interm.abcPaths(abcPaths);
                Instance inst = interm.getStatInst(umlsCuiNameMap, connLit);
                dataSet.add(inst);
            }
        }
        writeAttr(EvalStat.filePath, dataSet);
        Files.write(Paths.get(Concept.filePath), (Iterable<String>) abcPaths.stream()::iterator);

        System.out.println("[statFile] Finish");
    }
}
