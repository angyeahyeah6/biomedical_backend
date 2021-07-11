package weng.evaluation;

import weng.util.Utils;
import weng.util.file.RepurposingEval;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by lbj23k on 2017/6/23.
 */
public class Repurposing {

    public Repurposing() {
    }

    public double calculateAvgEval(File benchmark, File evalFile, int diseaseCount) {
        Logger logger = Logger.getAnonymousLogger();
        logger.info("[calculateAvgEval] Start");
        Map<String, List<String>> baseNdcgMap = (Map<String, List<String>>) new Utils().readObjectFile(benchmark.toString());
        Map<String, List<String>> evalNdcgMap = (Map<String, List<String>>) new Utils().readObjectFile(evalFile.toString());
        int evalSize = 30;
        double[] accBaseNdcg = new double[evalSize];
        double[] accEvalNdcg = new double[evalSize];

        for (String drug : evalNdcgMap.keySet()) {
            List<String> evalNdcg = evalNdcgMap.get(drug);
            List<String> baseNdcg = baseNdcgMap.get(drug);

            for (int i = 0; i < Math.min(baseNdcg.size(), evalSize); i++) {
                if (!Double.isNaN(Double.parseDouble(baseNdcg.get(i)))) {
                    accBaseNdcg[i] += Double.parseDouble(baseNdcg.get(i));
                    accEvalNdcg[i] += Double.parseDouble(evalNdcg.get(i));
                }
            }
        }

        Map<String, Integer> drugSizes = new HashMap<>();
        evalNdcgMap.keySet().forEach(e -> drugSizes.put(e, evalNdcgMap.get(e).size()));
        Map<Integer, Integer> drugTotalSizeByIndex = drugTotalSizeByIndex(evalSize, drugSizes);

        List<String> averageBaseNdcgLs = new ArrayList<>(evalSize);
        List<String> averageEvalNdcgLs = new ArrayList<>(evalSize);
        double avgImprove = 0;
        for (int i = 0; i < evalSize; i++) {
            int totalSize = drugTotalSizeByIndex.get(i);
            double avgBaseNdcg = accBaseNdcg[i] / totalSize;
            double avgEvalNdcg = accEvalNdcg[i] / totalSize;
            averageBaseNdcgLs.add(String.valueOf(avgBaseNdcg));
            averageEvalNdcgLs.add(String.valueOf(avgEvalNdcg));
            avgImprove += (avgEvalNdcg - avgBaseNdcg);
        }
        new Utils().writeLineFile(averageBaseNdcgLs, RepurposingEval.ouputNdcgDirectory + diseaseCount + "/" + benchmark.getName().replace(".dat", "") + ".txt");
        new Utils().writeLineFile(averageEvalNdcgLs, RepurposingEval.ouputNdcgDirectory + diseaseCount + "/" + evalFile.getName().replace(".dat", "") + ".txt");
        avgImprove /= evalSize;
        logger.info("[calculateAvgEval] Finish");
        return avgImprove;
    }

    public Map<Integer, Integer> drugTotalSizeByIndex(int evalSize,
                                                      Map<String, Integer> drugSizes) {
        Map<Integer, Integer> drugTotalSizeByIndex = new HashMap<>();
        for (int index = 0; index < evalSize; index++) {
            int finalIndex = index;
            drugTotalSizeByIndex.put(
                    index,
                    drugSizes.keySet().stream()
                            .filter(e -> drugSizes.get(e) > finalIndex)
                            .collect(Collectors.toList())
                            .size()
            );
        }
        return drugTotalSizeByIndex;
    }
}
