package weng.model;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weng.evaluation.ConfusionMatrix;
import weng.label.LabeledInterm;
import weng.util.calculator.EvaluationCalc;
import weng.util.file.Stopwords;
import weng.util.file.evaluationData.Concept;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Eval {
    public void assignTestData(HashMap<String, AbstractClassifier> classifierByName,
                               Instances train,
                               Instances test) throws Exception {
        List<String> paths = Files.lines(Paths.get(Concept.filePath)).collect(Collectors.toList());
        List<Instance> stats = new ArrayList<>(test);
        List<EvalStat> evalStats = evalStats(paths, stats);
        train = bSameAsAOrCFiltered(train, evalStats);
        test = bSameAsAOrCFiltered(test, evalStats);

        String important = train.classAttribute().value(0);
        String unImportant = train.classAttribute().value(1);

        for (String classifierName : classifierByName.keySet()) {
            AbstractClassifier classifier = classifierByName.get(classifierName);
            List<EvalPerformance> imp_performances = new ArrayList<>();
            List<EvalPerformance> unImp_performances = new ArrayList<>();
            List<EvalPerformance> overall_performances = new ArrayList<>();

            train.setClassIndex(train.numAttributes() - 1);
            test.setClassIndex(test.numAttributes() - 1);

            Evaluation eval = new Evaluation(train);
            classifier.buildClassifier(train);
            eval.evaluateModel(classifier, test);

            ConfusionMatrix cM = new ConfusionMatrix(
                    eval.confusionMatrix()[0][0],
                    eval.confusionMatrix()[1][1],
                    eval.confusionMatrix()[1][0],
                    eval.confusionMatrix()[0][1]
            );

            List<String> reals = reals(test);
            List<String> predicts = predicts(test, train, classifier);
            stopwordsSetToUnImportant(cM, important, test, evalStats, reals, predicts);

            double impP = EvaluationCalc.precision(cM.getTp(), cM.getFp());
            double impR = EvaluationCalc.recall(cM.getTp(), cM.getFn());
            double impF = EvaluationCalc.fMeasure(impP, impR);

            double unImpP = EvaluationCalc.precision(cM.getTn(), cM.getFn());
            double unImpR = EvaluationCalc.recall(cM.getTn(), cM.getFp());
            double unImpF = EvaluationCalc.fMeasure(unImpP, unImpR);

            double weightedAvgP = EvaluationCalc.weightedAvg(impP, unImpP, cM.getTp(), cM.getTn(), cM.getFp(), cM.getFn());
            double weightedAvgR = EvaluationCalc.weightedAvg(impR, unImpR, cM.getTp(), cM.getTn(), cM.getFp(), cM.getFn());
            double weightedAvgF = EvaluationCalc.weightedAvg(impF, unImpF, cM.getTp(), cM.getTn(), cM.getFp(), cM.getFn());

            imp_performances.add(new EvalPerformance(impP, impR, impF));
            unImp_performances.add(new EvalPerformance(unImpP, unImpR, unImpF));
            overall_performances.add(new EvalPerformance(weightedAvgP, weightedAvgR, weightedAvgF));

            double num = 1;
            double imp_avgPrecision = (imp_performances.stream()
                    .map(EvalPerformance::getPrecision)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double imp_avgRecall = (imp_performances.stream()
                    .map(EvalPerformance::getRecall)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double imp_avgFMeasure = (imp_performances.stream()
                    .map(EvalPerformance::getFMeasure)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double unImp_avgPrecision = (unImp_performances.stream()
                    .map(EvalPerformance::getPrecision)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double unImp_avgRecall = (unImp_performances.stream()
                    .map(EvalPerformance::getRecall)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double unImp_avgFMeasure = (unImp_performances.stream()
                    .map(EvalPerformance::getFMeasure)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double overall_avgPrecision = (overall_performances.stream()
                    .map(EvalPerformance::getPrecision)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double overall_avgRecall = (overall_performances.stream()
                    .map(EvalPerformance::getRecall)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double overall_avgFMeasure = (overall_performances.stream()
                    .map(EvalPerformance::getFMeasure)
                    .reduce(0d, (x, y) -> x + y)) / num;

            System.out.println(classifierName + "\tPrecision\tRecall\tF1");
            System.out.format("important\t%.3f\t%.3f\t%.3f\n", imp_avgPrecision, imp_avgRecall, imp_avgFMeasure);
            System.out.format("unimportant\t%.3f\t%.3f\t%.3f\n", unImp_avgPrecision, unImp_avgRecall, unImp_avgFMeasure);
            System.out.format("overall\t%.3f\t%.3f\t%.3f\n", overall_avgPrecision, overall_avgRecall, overall_avgFMeasure);
            System.out.println();
            System.out.println();
        }
    }

    public void tenFold(HashMap<String, AbstractClassifier> classifierByName, Instances data) throws Exception {
        List<String> paths = Files.lines(Paths.get(Concept.filePath)).collect(Collectors.toList());
        List<Instance> stats = new ArrayList<>(data);
        List<EvalStat> evalStats = evalStats(paths, stats);
        data = bSameAsAOrCFiltered(data, evalStats);

        String important = data.classAttribute().value(0);
        String unImportant = data.classAttribute().value(1);

        for (String classifierName : classifierByName.keySet()) {
            AbstractClassifier classifier = classifierByName.get(classifierName);

            // Cross validation
            int iter = 30;
            int foldTimes = 10;
            List<EvalPerformance> imp_performances = new ArrayList<>();
            List<EvalPerformance> unImp_performances = new ArrayList<>();
            List<EvalPerformance> overall_performances = new ArrayList<>();
            for (int i = 0; i < iter; i++) {
                data.randomize(new Random(i));
                data.stratify(foldTimes);
                for (int j = 0; j < foldTimes; j++) {
                    Instances train = data.trainCV(foldTimes, j);
                    Instances test = data.testCV(foldTimes, j);
                    train.setClassIndex(train.numAttributes() - 1);
                    test.setClassIndex(test.numAttributes() - 1);

//                    train = stopwordsFiltered(train, evalStats);
//                    test = stopwordsFiltered(test, evalStats);

                    Evaluation eval = new Evaluation(train);
                    classifier.buildClassifier(train);
                    eval.evaluateModel(classifier, test);

                    ConfusionMatrix cM = new ConfusionMatrix(
                            eval.confusionMatrix()[0][0],
                            eval.confusionMatrix()[1][1],
                            eval.confusionMatrix()[1][0],
                            eval.confusionMatrix()[0][1]
                    );

                    List<String> reals = reals(test);
                    List<String> predicts = predicts(test, train, classifier);
                    stopwordsSetToUnImportant(cM, important, test, evalStats, reals, predicts);

                    double impP = EvaluationCalc.precision(cM.getTp(), cM.getFp());
                    double impR = EvaluationCalc.recall(cM.getTp(), cM.getFn());
                    double impF = EvaluationCalc.fMeasure(impP, impR);

                    double unImpP = EvaluationCalc.precision(cM.getTn(), cM.getFn());
                    double unImpR = EvaluationCalc.recall(cM.getTn(), cM.getFp());
                    double unImpF = EvaluationCalc.fMeasure(unImpP, unImpR);

                    double weightedAvgP = EvaluationCalc.weightedAvg(impP, unImpP, cM.getTp(), cM.getTn(), cM.getFp(), cM.getFn());
                    double weightedAvgR = EvaluationCalc.weightedAvg(impR, unImpR, cM.getTp(), cM.getTn(), cM.getFp(), cM.getFn());
                    double weightedAvgF = EvaluationCalc.weightedAvg(impF, unImpF, cM.getTp(), cM.getTn(), cM.getFp(), cM.getFn());

                    imp_performances.add(new EvalPerformance(impP, impR, impF));
                    unImp_performances.add(new EvalPerformance(unImpP, unImpR, unImpF));
                    overall_performances.add(new EvalPerformance(weightedAvgP, weightedAvgR, weightedAvgF));
                }
            }

            double num = iter * foldTimes;
            double imp_avgPrecision = (imp_performances.stream()
                    .map(EvalPerformance::getPrecision)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double imp_avgRecall = (imp_performances.stream()
                    .map(EvalPerformance::getRecall)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double imp_avgFMeasure = (imp_performances.stream()
                    .map(EvalPerformance::getFMeasure)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double unimp_avgPrecision = (unImp_performances.stream()
                    .map(EvalPerformance::getPrecision)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double unimp_avgRecall = (unImp_performances.stream()
                    .map(EvalPerformance::getRecall)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double unimp_avgFMeasure = (unImp_performances.stream()
                    .map(EvalPerformance::getFMeasure)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double overall_avgPrecision = (overall_performances.stream()
                    .map(EvalPerformance::getPrecision)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double overall_avgRecall = (overall_performances.stream()
                    .map(EvalPerformance::getRecall)
                    .reduce(0d, (x, y) -> x + y)) / num;
            double overall_avgFMeasure = (overall_performances.stream()
                    .map(EvalPerformance::getFMeasure)
                    .reduce(0d, (x, y) -> x + y)) / num;

            System.out.println(classifierName + "\tPrecision\tRecall\tF1");
            System.out.format("important\t%.3f\t%.3f\t%.3f\n", imp_avgPrecision, imp_avgRecall, imp_avgFMeasure);
            System.out.format("unimportant\t%.3f\t%.3f\t%.3f\n", unimp_avgPrecision, unimp_avgRecall, unimp_avgFMeasure);
            System.out.format("overall\t%.3f\t%.3f\t%.3f\n", overall_avgPrecision, overall_avgRecall, overall_avgFMeasure);
            System.out.println();
            System.out.println();
        }
    }

    public List<String> predicts(Instances test, Instances train, AbstractClassifier classifier) throws Exception {
        List<String> predicts = new ArrayList<>();
        for (int k = 0; k < test.numInstances(); k++) {
            double predictIndex = classifier.classifyInstance(test.instance(k));
            predicts.add(train.classAttribute().value((int) predictIndex));
        }
        return predicts;
    }

    public List<String> reals(Instances test) {
        List<String> reals = new ArrayList<>();
        for (int k = 0; k < test.numInstances(); k++) {
            double realIndex = test.instance(k).classValue();
            reals.add(test.classAttribute().value((int) realIndex));
        }
        return reals;
    }

    public List<EvalStat> evalStats(List<String> paths, List<Instance> stats) {
        List<EvalStat> evalStats = new ArrayList<>();
        for (String path : paths) {
            int index = paths.indexOf(path);
            String[] concepts = path.split("_");
            evalStats.add(new EvalStat(concepts[0], concepts[1], concepts[2], stats.get(index).toString()));
        }
        return evalStats;
    }

    public void stopwordsSetToUnImportant(ConfusionMatrix cM, String important, Instances data,
                                          List<EvalStat> evalStats, List<String> reals, List<String> predicts) {
        for (Instance attrs : data) {
            EvalStat evalStat = evalStats.stream()
                    .filter(e -> e.getAttributes().equals(attrs.toString()))
                    .collect(Collectors.toList()).get(0);
            if (Stopwords.set.contains(evalStat.getB())) {
                String real = reals.get(data.indexOf(attrs));
                String predict = predicts.get(data.indexOf(attrs));
                if (predict.equals(important)) {
                    if (real.equals(important)) {
                        cM.setTp(cM.getTp() - 1);
                        cM.setFn(cM.getFn() + 1);
                    } else {
                        cM.setFp(cM.getFp() - 1);
                        cM.setTn(cM.getTn() + 1);
                    }
                }
            }
        }
    }

    public Instances stopwordsFiltered(Instances data, List<EvalStat> evalStats) {
        ArrayList<Attribute> attributes = LabeledInterm.getStatAttributes();
        Instances dataFiltered = new Instances("statAttr", attributes, 0);
        dataFiltered.setClassIndex(dataFiltered.numAttributes() - 1);
        for (Instance attrs : data) {
            EvalStat evalStat = evalStats.stream()
                    .filter(e -> e.getAttributes().equals(attrs.toString()))
                    .collect(Collectors.toList()).get(0);
            if (!Stopwords.set.contains(evalStat.getB())) {
                dataFiltered.add(attrs);
            }
        }
        return dataFiltered;
    }

    public Instances bSameAsAOrCFiltered(Instances data, List<EvalStat> evalStats) {
        ArrayList<Attribute> attributes = LabeledInterm.getStatAttributes();
        Instances dataFiltered = new Instances("statAttr", attributes, 0);
        dataFiltered.setClassIndex(dataFiltered.numAttributes() - 1);
        for (Instance attrs : data) {
            EvalStat evalStat = evalStats.stream()
                    .filter(e -> e.getAttributes().equals(attrs.toString()))
                    .collect(Collectors.toList()).get(0);
            if (!evalStat.getB().equals(evalStat.getA())
                    && !evalStat.getB().equals(evalStat.getC())) {
                dataFiltered.add(attrs);
            }
        }
        return dataFiltered;
    }
}
