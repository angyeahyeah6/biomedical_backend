package ken.model;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.classifiers.trees.RandomForest;

/**
 * Created by lbj23k on 2017/8/23.
 */
public class Classifier {
    public static AbstractClassifier getNaiveBayes(){
        return new NaiveBayes();
    }
    public static AbstractClassifier getRandomForest(){
        return new RandomForest();
    }
    public static AbstractClassifier getSVM(){
        SMO classifier = new SMO();
        classifier.setC(1000);
        classifier.setKernel(new RBFKernel());
        classifier.setCalibrator(new Logistic());
        classifier.setBuildCalibrationModels(true);
        return classifier;
    }
    public static AbstractClassifier getLogisticRegression(){
        return new Logistic();
    }
}
