package ken.model;

import ken.evaluation.IndexScore;
import ken.network.PredicateNetwork;
import ken.util.Utils;
import l2r.data.Document;
import l2r.data.MSDocument;
import l2r.module.ListNetModule;
import l2r.module.Module;
import l2r.parameter.Parameters;
import l2r.process.listnet.ModuleTest;
import l2r.process.listnet.Trainer;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by lbj23k on 2017/6/22.
 */
public class ListNetModel {
    private HashMap<String, Map<String, IndexScore>> perfectRank;
    private List<String> drugs500;
    private Logger logger;

    public static void main(String[] args) {
        ListNetModel model = new ListNetModel();
        model.runLearning2Rank();
    }
    public ListNetModel() {
        String rankPath = "dat_file/eval/goldenRank.dat";
        perfectRank = (HashMap<String, Map<String, IndexScore>>) Utils.readObjectFile(rankPath);
        drugs500 = Utils.readLineFile("vocabulary/500drugs_seed.txt");
        logger = Logger.getLogger("ListNetModel");
    }

    public void runLearning2Rank() {
        File outputDir = new File("output/listnet_model2");
        if (!outputDir.exists()) outputDir.mkdir();
        int folds = 10;
        logger.info("Prepare training, testing file...");
        crossValidation(folds, outputDir);
        Map<String, List<String>> ndcgMap = new HashMap<>();
        for (int i = 0; i < folds; i++) {
            String train = outputDir.getPath() + "/train" + i + ".txt";
            String test = outputDir.getPath() + "/test" + i + ".txt";
            String module = outputDir.getPath() + "/listnet_" + i + ".module";
            logger.info(i + " fold train...");
            ListNetTrain(train, module);
            try {
                logger.info(i + " fold test...");
                ListNetTest(module, test, ndcgMap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Utils.writeObject(ndcgMap, "dat_file/ndcg/listNet2.dat");

    }

    public void crossValidation(int folds, File outputDir) {
        for (int i = 0; i < folds; i++) {
            System.out.println("Create file for fold:" + i);
            List<String> trainDrugs = new ArrayList<>();
            List<String> testDrugs = new ArrayList<>();
            for (int j = 0; j < drugs500.size(); j++) {
                if (j % folds == i) testDrugs.add(drugs500.get(j));
                else trainDrugs.add(drugs500.get(j));
            }
            String trainPath = outputDir.getPath() + "/train" + i + ".txt";
            createProcessFile(trainDrugs, trainPath);
            String testPath = outputDir.getPath() + "/test" + i + ".txt";
            createProcessFile(testDrugs, testPath);
        }
    }

    public void createProcessFile(List<String> drugs, String outputPath) {
        List<String> trainLs = new ArrayList<>();
        for (String drug : drugs) {
            Map<String, Instances> instMap = (Map<String, Instances>)
                    Utils.readObjectFile("dat_file/instMap/" + drug + ".dat");
            int index = drugs500.indexOf(drug);
            Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);
            for (String disease : perfectDrugRank.keySet()) {
                PredicateNetwork predicateNet = new PredicateNetwork(drug, disease, instMap.get(disease));
                List<Double> features = predicateNet.getAllFeatures();
                int score = perfectDrugRank.get(disease).getRealScore();
                StringBuilder sb = new StringBuilder();
                sb.append(score).append(" ").append("qid:").append(index).append(" ");
                for (int j = 0; j < features.size(); j++) {
                    sb.append(j).append(":").append(features.get(j)).append(" ");
                }
                sb.append("#").append(drug).append(" ").append("#").append(disease);
                trainLs.add(sb.toString());
            }
        }
        Utils.writeLineFile(trainLs, outputPath);
    }

    public void ListNetTrain(String inputFile, String outputFile) {
        int round = 1000;
        Parameters.setEpochnum(round);
        try {
            Module m_training = Trainer.train(new File(inputFile));
            m_training.write(new File(outputFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ListNetTest(String module, String input, Map<String, List<String>> ndcgMap)
            throws IOException, IllegalClassFormatException {
        Module m_testing = ListNetModule.getInstance(new File(module));
        ModuleTest tester = new ModuleTest(m_testing);
        BufferedReader reader = new BufferedReader(new FileReader(input));
        String line;
        Map<String, List<IndexScore>> evalMap = new HashMap<>();
        Map<String, List<IndexScore>> perfectMap = new HashMap<>();


        while ((line = reader.readLine()) != null) {
            String[] strAry = line.split("#");
            String drug = strAry[1].trim();
            String disease = strAry[2].trim();
            Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);

            List<IndexScore> perfectScoreLs = evalMap.getOrDefault(drug, new ArrayList<>());
            List<IndexScore> evalScoreLs = perfectMap.getOrDefault(drug, new ArrayList<>());

            Document doc = MSDocument.parseDoc(line);
            doc = m_testing.getNormalizer().normalize(doc);
            double weight = tester.test(doc.getFeatures());
            IndexScore item = new IndexScore(disease);
            IndexScore perfectItem = perfectDrugRank.get(disease);
            item.setFeature(weight);
            item.setRealScore(perfectItem.getRealScore());

            perfectScoreLs.add(perfectItem);
            evalScoreLs.add(item);
            evalMap.put(drug, evalScoreLs);
            perfectMap.put(drug, perfectScoreLs);
        }
        for (String drug : perfectMap.keySet()) {
            List<IndexScore> perfectScoreLs = perfectMap.get(drug);
            List<IndexScore> evalScoreLs = evalMap.get(drug);
            Collections.sort(evalScoreLs, Collections.reverseOrder(new IndexScore.FeatureComparator()));
            Collections.sort(perfectScoreLs, Collections.reverseOrder(new IndexScore.ScoreComparator()));
            List<String> ndcg = LearningModel.getNdcg(perfectScoreLs, evalScoreLs);
            ndcgMap.put(drug, ndcg);
        }

    }
}
