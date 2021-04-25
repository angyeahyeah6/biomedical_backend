package ken.evaluation;

import ken.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by lbj23k on 2017/6/23.
 */
public class Evaluator {
    public static void main(String[] args) {
        //TODO setting benchmark ndcg file & compared ndcg file
        File benchmark = new File("dat_file/ndcg/svm_stat_15.dat");
        File evalFile = new File("dat_file/ndcg/svm_feature_selection.dat");
        calculateAvgEval(benchmark, evalFile);
    }

    public static double calculateAvgEval(File benchmark, File evalFile) {
        int evalSize = 30;
        double[] accBaseNdcg = new double[evalSize];
        double[] accEvalNdcg = new double[evalSize];
        Map<String, List<String>> baseNdcgMap = (Map<String, List<String>>)
                Utils.readObjectFile(benchmark.toString());
        Map<String, List<String>> evalNdcgMap = (Map<String, List<String>>)
                Utils.readObjectFile(evalFile.toString());
        for (String drug : evalNdcgMap.keySet()) {
            List<String> baseNdcg = baseNdcgMap.get(drug);
            List<String> evalNdcg = evalNdcgMap.get(drug);
            for (int i = 0; i < evalSize; i++) {
                accBaseNdcg[i] += Double.parseDouble(baseNdcg.get(i));
                accEvalNdcg[i] += Double.parseDouble(evalNdcg.get(i));
            }
        }

        List<String> averageBaseNdcgLs = new ArrayList<>(evalSize);
        List<String> averageEvalNdcgLs = new ArrayList<>(evalSize);
        int totalSize = evalNdcgMap.size();
        double avgImprove = 0;
        for (int i = 0; i < evalSize; i++) {
            double avgBaseNdcg = accBaseNdcg[i] / totalSize;
            double avgEvalNdcg = accEvalNdcg[i] / totalSize;
            averageBaseNdcgLs.add(String.valueOf(avgBaseNdcg));
            averageEvalNdcgLs.add(String.valueOf(avgEvalNdcg));
            avgImprove += avgEvalNdcg - avgBaseNdcg;
        }
        Utils.writeLineFile(averageBaseNdcgLs, "output/ndcg/" + benchmark.getName() + ".txt");
        Utils.writeLineFile(averageEvalNdcgLs, "output/ndcg/" + evalFile.getName() + ".txt");
        avgImprove /= evalSize;
        System.out.format("average improve: %f\n", avgImprove);
        return avgImprove;
    }
}
