package weng.model;

import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weng.evaluation.ConfusionMatrix;
import weng.label.LabeledInterm;
import weng.util.file.Stopwords;
import weng.util.file.evaluationData.Concept;
import weng.util.file.evaluationData.Predict;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EvalData {
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

        Logger logger = Logger.getAnonymousLogger();
        for (String classifierName : classifierByName.keySet()) {
            List<EvalResult> evalResults = new ArrayList<>();
            AbstractClassifier classifier = classifierByName.get(classifierName);
            logger.info("[evaluationData (Assign Test Data)] Start with model: " + classifierName);

            train.setClassIndex(train.numAttributes() - 1);
            test.setClassIndex(test.numAttributes() - 1);

            List<String> reals = reals(test);
            classifier.buildClassifier(train);
            List<String> predicts = predicts(test, train, classifier);
            for (Instance attrs : test) {
                EvalStat evalStat = evalStats.stream()
                        .filter(e -> e.getAttributes().equals(attrs.toString()))
                        .collect(Collectors.toList()).get(0);
                if (Stopwords.set.contains(evalStat.getB())
                        && predicts.get(test.indexOf(attrs)).equals(important)) {
                    evalResults.add(new EvalResult(evalStat.getA(), evalStat.getB(), evalStat.getC(),
                            reals.get(test.indexOf(attrs)), unImportant));
                } else {
                    evalResults.add(new EvalResult(evalStat.getA(), evalStat.getB(), evalStat.getC(),
                            reals.get(test.indexOf(attrs)), predicts.get(test.indexOf(attrs))));
                }
            }
            logger.info("[evaluationData (Assign Test Data)] Finish");

            // Write predicted answer from evaluation into files
            logger.info("[Write prediction into files] Start");
            String header = "path\treal\tpredict\n";
            String filePath = Predict.filePath + "_" + classifierName + ".csv";
            Files.write(Paths.get(filePath), header.getBytes());
            Map<String, List<EvalResult>> caseEvalResults = evalResults.stream()
                    .collect(Collectors.groupingBy(e -> e.getA() + "_" + e.getC()));
            for (String caseName : caseEvalResults.keySet()) {
                Files.write(Paths.get(filePath), (caseName + "\n").getBytes(), StandardOpenOption.APPEND);
                Files.write(Paths.get(filePath),
                        caseEvalResults.get(caseName).stream()
                                .map(e -> e.getB() + "\t" + e.getReal() + "\t" + e.getPredict())
                                .collect(Collectors.toList()),
                        StandardOpenOption.APPEND);
                Files.write(Paths.get(filePath), "\n".getBytes(), StandardOpenOption.APPEND);
            }
            logger.info("[Write prediction into files] Finish");
        }
    }

    public void tenFold(HashMap<String, AbstractClassifier> classifierByName, Instances data) throws Exception {
        List<String> paths = Files.lines(Paths.get(Concept.filePath)).collect(Collectors.toList());
        List<Instance> stats = new ArrayList<>(data);
        List<EvalStat> evalStats = evalStats(paths, stats);
        data = bSameAsAOrCFiltered(data, evalStats);

        String important = data.classAttribute().value(0);
        String unImportant = data.classAttribute().value(1);

        Logger logger = Logger.getAnonymousLogger();
        for (String classifierName : classifierByName.keySet()) {
            // Cross validation
            List<EvalResult> evalResults = new ArrayList<>();
            AbstractClassifier classifier = classifierByName.get(classifierName);
            logger.info("[10-fold cross validation] Start with model: " + classifierName);
            int iter = 1;
            int foldTimes = 10;
            for (int i = 0; i < iter; i++) {
                logger.info("[10-fold cross validation] " + i + "times");
                data.randomize(new Random(i));
                data.stratify(foldTimes);
                for (int j = 0; j < foldTimes; j++) {
                    Instances train = data.trainCV(foldTimes, j);
                    Instances test = data.testCV(foldTimes, j);
                    train.setClassIndex(train.numAttributes() - 1);
                    test.setClassIndex(test.numAttributes() - 1);

                    List<String> reals = reals(test);
                    classifier.buildClassifier(train);
                    List<String> predicts = predicts(test, train, classifier);
                    for (Instance attrs : test) {
                        EvalStat evalStat = evalStats.stream()
                                .filter(e -> e.getAttributes().equals(attrs.toString()))
                                .collect(Collectors.toList()).get(0);
                        if (Stopwords.set.contains(evalStat.getB())
                                && predicts.get(test.indexOf(attrs)).equals(important)) {
                            evalResults.add(new EvalResult(evalStat.getA(), evalStat.getB(), evalStat.getC(),
                                    reals.get(test.indexOf(attrs)), unImportant));
                        } else {
                            evalResults.add(new EvalResult(evalStat.getA(), evalStat.getB(), evalStat.getC(),
                                    reals.get(test.indexOf(attrs)), predicts.get(test.indexOf(attrs))));
                        }

                    }
                }

            }
            logger.info("[10-fold cross validation] Finish");

            // Write predicted answer from evaluation into files
            logger.info("[Write prediction into files] Start");
            String header = "path\treal\tpredict\n";
            String filePath = Predict.filePath + "_" + classifierName + ".csv";
            Files.write(Paths.get(filePath), header.getBytes());
            Map<String, List<EvalResult>> caseEvalResults = evalResults.stream()
                    .collect(Collectors.groupingBy(e -> e.getA() + "_" + e.getC()));
            for (String caseName : caseEvalResults.keySet()) {
                Files.write(Paths.get(filePath), (caseName + "\n").getBytes(), StandardOpenOption.APPEND);
                Files.write(Paths.get(filePath),
                        caseEvalResults.get(caseName).stream()
                                .map(e -> e.getB() + "\t" + e.getReal() + "\t" + e.getPredict())
                                .collect(Collectors.toList()),
                        StandardOpenOption.APPEND);
                Files.write(Paths.get(filePath), "\n".getBytes(), StandardOpenOption.APPEND);
            }
            logger.info("[Write prediction into files] Finish");
        }
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
}
