package weng.main;

import weka.core.Instances;
import weng.label.LabeledNetwork;
import weng.model.Eval;
import weng.model.EvalData;
import weng.model.EvalDataAttr;
import weng.util.DbConnector;
import weng.util.file.evaluationData.EvalStat;

import java.sql.Connection;
import java.util.ArrayList;

public class EvaluatorData {
    public static void main(String[] args) {
        EvalDataAttr evalDataAttr = new EvalDataAttr();

        // Set data set
        try {
            DbConnector dbConnector = new DbConnector();
            Connection connSem = dbConnector.getSemMedConnection();
            Connection connLit = dbConnector.getLiteratureNodeConnection();
            Connection connLSUMLS = dbConnector.getLabelSystemUmlsConnection();
            Connection connBioRel = dbConnector.getBioRelationConnection();
            ArrayList<LabeledNetwork> pgs = evalDataAttr.getPredicateGraph(connSem, connLit, connLSUMLS, connBioRel);
            evalDataAttr.contentFile(pgs);
            evalDataAttr.statFile(pgs, connSem, connLit);
            connSem.close();
            connLit.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Performance evaluation of model
        try {
            Instances data = evalDataAttr.readAttrFile(EvalStat.filePath); // Total cases
            data.setClassIndex(data.numAttributes() - 1);

            // Performance
            Eval eval = new Eval();
            eval.tenFold(evalDataAttr.classifiers(), data);

            // Constructor predication data after evaluation
            EvalData evalData = new EvalData();
            evalData.tenFold(evalDataAttr.classifiers(), data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Performance evaluation of model by assigning the test data
//        try {
//            Instances data = evalDataAttr.readAttrFile(EvalStat.filePath); // Total cases
//            data.setClassIndex(data.numAttributes() - 1);
//
//            // Assign test data
//            String fileName = "output/attr/forEvalPredict/testData/stat_30Predicates_Metformin-MNOB_ori.arff";
////            String fileName = "output/attr/forEvalPredict/testData/stat_30Predicates_Metformin-MNOB_WNPC.arff";
//            Instances test = evalDataAttr.readAttrFile(fileName); // Total cases
//            test.setClassIndex(test.numAttributes() - 1);
//
//            // Performance
//            Eval eval = new Eval();
//            eval.assignTestData(evalDataAttr.classifiers(), data, test);
//
//            // Constructor data after evaluation
//            EvalData evalData = new EvalData();
//            evalData.assignTestData(evalDataAttr.classifiers(), data, test);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
